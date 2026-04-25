package com.library.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisSecurityStateStore implements SecurityStateStore {

    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill_at')
            local tokens = tonumber(bucket[1])
            local last_refill_at = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
            end

            if last_refill_at == nil then
                last_refill_at = now
            end

            local elapsed = math.max(0, now - last_refill_at)
            tokens = math.min(capacity, tokens + (elapsed / 1000.0) * refill_rate)

            local allowed = 0
            local retry_after = 0

            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            elseif refill_rate > 0 then
                retry_after = math.ceil((1 - tokens) / refill_rate)
            else
                retry_after = math.ceil(ttl / 1000.0)
            end

            redis.call('HSET', key, 'tokens', tokens, 'last_refill_at', now)
            redis.call('PEXPIRE', key, ttl)

            return {allowed, math.floor(tokens), retry_after}
            """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisSecurityStateStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long increment(String key, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        Long value = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ttl);
        return value != null ? value : 0L;
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(created);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public long getRemainingTtlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }

    @Override
    public long countByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        return keys == null ? 0 : keys.size();
    }

    @Override
    public TokenBucketResult consumeTokenBucket(String key,
                                                int capacity,
                                                double refillTokensPerSecond,
                                                long nowMillis,
                                                Duration ttl) {
        List<?> result = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillTokensPerSecond),
                String.valueOf(nowMillis),
                String.valueOf(ttl.toMillis())
        );

        long allowed = readLong(result, 0);
        int remainingTokens = (int) Math.max(readLong(result, 1), 0L);
        long retryAfterSeconds = Math.max(readLong(result, 2), 0L);
        return new TokenBucketResult(allowed == 1L, remainingTokens, retryAfterSeconds);
    }

    @Override
    public void clearAll() {
        Set<String> keys = redisTemplate.keys("security:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private long readLong(List<?> values, int index) {
        if (values == null || values.size() <= index || values.get(index) == null) {
            return 0L;
        }

        Object value = values.get(index);
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }
}
