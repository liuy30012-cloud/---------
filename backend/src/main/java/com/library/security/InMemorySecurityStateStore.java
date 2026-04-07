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

@Slf4j
public class InMemorySecurityStateStore implements SecurityStateStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
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
    public void clearAll() {
        entries.clear();
    }

    private void cleanupExpiredEntries() {
        long now = clock.millis();
        entries.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private record Entry(String value, long expiryAt) {
        boolean isExpired(long now) {
            return expiryAt <= now;
        }
    }
}
