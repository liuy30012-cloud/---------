package com.library.security;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisSecurityStateStore implements SecurityStateStore {

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
    public void clearAll() {
        Set<String> keys = redisTemplate.keys("security:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
