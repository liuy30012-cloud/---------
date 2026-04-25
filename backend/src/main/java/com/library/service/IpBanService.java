package com.library.service;

import com.library.security.SecurityStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IpBanService {

    private static final Duration VIOLATION_TTL = Duration.ofMinutes(10);
    private static final String BAN_PREFIX = "security:ipban:ban:";
    private static final String VIOLATION_PREFIX = "security:ipban:violations:";

    private final SecurityStateStore stateStore;
    private final SecurityMetricsService securityMetricsService;

    @Value("${anti-crawler.rate-limit.ban-threshold:3}")
    private int banThreshold;

    @Value("${anti-crawler.rate-limit.ban-duration:900}")
    private int banDuration;

    public IpBanService(SecurityStateStore stateStore, SecurityMetricsService securityMetricsService) {
        this.stateStore = stateStore;
        this.securityMetricsService = securityMetricsService;
    }

    public boolean isIpBanned(String clientIp) {
        return stateStore.get(banKey(clientIp)) != null;
    }

    public int getRemainingBanTime(String clientIp) {
        return (int) stateStore.getRemainingTtlSeconds(banKey(clientIp));
    }

    public boolean registerRateLimitViolation(String clientIp) {
        return registerRateLimitViolation(clientIp, "app");
    }

    public boolean registerRateLimitViolation(String clientIp, String source) {
        long count = stateStore.increment(violationKey(clientIp), VIOLATION_TTL);
        if (count >= banThreshold) {
            banIp(clientIp, banDuration, source);
            stateStore.delete(violationKey(clientIp));
            return true;
        }
        return false;
    }

    public void banIp(String ip, int durationSeconds) {
        banIp(ip, durationSeconds, "app");
    }

    public void banIp(String ip, int durationSeconds, String source) {
        boolean alreadyBanned = isIpBanned(ip);
        stateStore.set(banKey(ip), source, Duration.ofSeconds(Math.max(durationSeconds, 1)));
        if (!alreadyBanned) {
            securityMetricsService.recordIpBan(source);
        }
    }

    public void unbanIp(String ip) {
        stateStore.delete(banKey(ip));
        stateStore.delete(violationKey(ip));
    }

    public int getBannedIpCount() {
        return (int) stateStore.countByPrefix(BAN_PREFIX);
    }

    public long getViolationCount(String clientIp) {
        String value = stateStore.get(violationKey(clientIp));
        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public int getTrackedViolationCount() {
        return (int) stateStore.countByPrefix(VIOLATION_PREFIX);
    }

    public String getBanSource(String clientIp) {
        return stateStore.get(banKey(clientIp));
    }

    private String banKey(String ip) {
        return BAN_PREFIX + normalize(ip);
    }

    private String violationKey(String ip) {
        return VIOLATION_PREFIX + normalize(ip);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
