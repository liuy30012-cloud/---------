package com.library.security;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class InMemorySecurityStateStore implements SecurityStateStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, TokenBucketEntry> tokenBuckets = new ConcurrentHashMap<>();
    private final Clock clock;
    private ScheduledExecutorService cleanupExecutor;

    public InMemorySecurityStateStore() {
        this(Clock.systemUTC());
    }

    public InMemorySecurityStateStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    public void init() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "security-state-store-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public long increment(String key, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");

        long now = clock.millis();
        long expiryAt = now + ttl.toMillis();

        Entry entry = entries.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Entry("1", expiryAt);
            }

            long nextValue = Long.parseLong(current.value()) + 1;
            return new Entry(String.valueOf(nextValue), expiryAt);
        });

        return Long.parseLong(entry.value());
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");

        entries.put(key, new Entry(value, clock.millis() + ttl.toMillis()));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");

        long now = clock.millis();
        long expiryAt = now + ttl.toMillis();
        AtomicBoolean created = new AtomicBoolean(false);

        entries.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                created.set(true);
                return new Entry(value, expiryAt);
            }
            return existing;
        });

        return created.get();
    }

    @Override
    public String get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired(clock.millis())) {
            entries.remove(key, entry);
            return null;
        }

        return entry.value();
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    @Override
    public long getRemainingTtlSeconds(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return 0;
        }

        long remainingMillis = entry.expiryAt() - clock.millis();
        if (remainingMillis <= 0) {
            entries.remove(key, entry);
            return 0;
        }

        return Math.max(1, (long) Math.ceil(remainingMillis / 1000.0));
    }

    @Override
    public long countByPrefix(String prefix) {
        cleanupExpiredEntries();
        return entries.keySet().stream().filter(key -> key.startsWith(prefix)).count();
    }

    @Override
    public TokenBucketResult consumeTokenBucket(String key,
                                                int capacity,
                                                double refillTokensPerSecond,
                                                long nowMillis,
                                                Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");

        AtomicReference<TokenBucketResult> resultRef = new AtomicReference<>();
        tokenBuckets.compute(key, (ignored, current) -> {
            TokenBucketEntry active = current;
            if (active == null || active.isExpired(nowMillis)) {
                active = new TokenBucketEntry(capacity, nowMillis, nowMillis + ttl.toMillis());
            }

            double elapsedSeconds = Math.max(0D, (nowMillis - active.lastRefillAt()) / 1000D);
            double replenished = Math.min(capacity, active.tokens() + elapsedSeconds * refillTokensPerSecond);
            boolean allowed = replenished >= 1D;
            long retryAfterSeconds = 0L;

            if (allowed) {
                replenished -= 1D;
            } else if (refillTokensPerSecond > 0D) {
                retryAfterSeconds = Math.max(1L, (long) Math.ceil((1D - replenished) / refillTokensPerSecond));
            } else {
                retryAfterSeconds = Math.max(1L, ttl.toSeconds());
            }

            resultRef.set(new TokenBucketResult(
                    allowed,
                    Math.max((int) Math.floor(replenished), 0),
                    retryAfterSeconds
            ));
            return new TokenBucketEntry(replenished, nowMillis, nowMillis + ttl.toMillis());
        });

        return resultRef.get();
    }

    @Override
    public void clearAll() {
        entries.clear();
        tokenBuckets.clear();
    }

    private void cleanupExpiredEntries() {
        long now = clock.millis();
        entries.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        tokenBuckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private record Entry(String value, long expiryAt) {
        boolean isExpired(long now) {
            return expiryAt <= now;
        }
    }

    private record TokenBucketEntry(double tokens, long lastRefillAt, long expiryAt) {
        boolean isExpired(long now) {
            return expiryAt <= now;
        }
    }
}
