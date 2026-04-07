package com.library.service;

import com.library.security.SecurityStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
public class RateLimitService {

    private static final Duration BURST_TTL = Duration.ofSeconds(5);
    private static final Duration WINDOW_TTL = Duration.ofMinutes(2);

    private final SecurityStateStore stateStore;
    private final Clock clock;

    @Value("${anti-crawler.rate-limit.global-rpm:60}")
    private int globalRpm;

    @Value("${anti-crawler.rate-limit.search-rpm:20}")
    private int searchRpm;

    @Value("${anti-crawler.rate-limit.burst-size:10}")
    private int burstSize;

    public RateLimitService(SecurityStateStore stateStore, Clock clock) {
        this.stateStore = stateStore;
        this.clock = clock;
    }

    public boolean checkBurstLimit(String clientIp) {
        return checkBurstLimit(clientIp, null);
    }

    public boolean checkBurstLimit(String clientIp, Long userId) {
        long secondBucket = clock.millis() / 1000;
        return exceedsLimit("burst", "ip:" + normalize(clientIp), secondBucket, burstSize, BURST_TTL)
                || exceedsUserLimit("burst", userId, secondBucket, burstSize, BURST_TTL);
    }

    public boolean checkGlobalLimit(String clientIp) {
        return checkGlobalLimit(clientIp, null);
    }

    public boolean checkGlobalLimit(String clientIp, Long userId) {
        long minuteBucket = clock.millis() / 60_000;
        return exceedsLimit("global", "ip:" + normalize(clientIp), minuteBucket, globalRpm, WINDOW_TTL)
                || exceedsUserLimit("global", userId, minuteBucket, globalRpm, WINDOW_TTL);
    }

    public boolean checkSearchLimit(String clientIp) {
        return checkSearchLimit(clientIp, null);
    }

    public boolean checkSearchLimit(String clientIp, Long userId) {
        long minuteBucket = clock.millis() / 60_000;
        return exceedsLimit("search", "ip:" + normalize(clientIp), minuteBucket, searchRpm, WINDOW_TTL)
                || exceedsUserLimit("search", userId, minuteBucket, searchRpm, WINDOW_TTL);
    }

    private boolean exceedsUserLimit(String category, Long userId, long bucket, int limit, Duration ttl) {
        if (userId == null) {
            return false;
        }
        return exceedsLimit(category, "user:" + userId, bucket, limit, ttl);
    }

    private boolean exceedsLimit(String category, String scope, long bucket, int limit, Duration ttl) {
        String key = "security:ratelimit:" + category + ":" + scope + ":" + bucket;
        return stateStore.increment(key, ttl) > limit;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
