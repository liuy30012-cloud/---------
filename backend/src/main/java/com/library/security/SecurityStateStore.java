package com.library.security;

import java.time.Duration;

public interface SecurityStateStore {

    long increment(String key, Duration ttl);

    void set(String key, String value, Duration ttl);

    boolean setIfAbsent(String key, String value, Duration ttl);

    String get(String key);

    void delete(String key);

    long getRemainingTtlSeconds(String key);

    long countByPrefix(String prefix);

    TokenBucketResult consumeTokenBucket(String key,
                                         int capacity,
                                         double refillTokensPerSecond,
                                         long nowMillis,
                                         Duration ttl);

    void clearAll();

    record TokenBucketResult(boolean allowed, int remainingTokens, long retryAfterSeconds) {
    }
}
