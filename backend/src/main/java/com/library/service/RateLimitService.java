package com.library.service;

import com.library.security.SecurityStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

@Service
public class RateLimitService {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    private final SecurityStateStore stateStore;
    private final Clock clock;

    @Value("${anti-crawler.rate-limit.public-search-per-minute-ip:30}")
    private int publicSearchPerMinuteIp;

    @Value("${anti-crawler.rate-limit.public-search-per-minute-fingerprint:30}")
    private int publicSearchPerMinuteFingerprint;

    @Value("${anti-crawler.rate-limit.public-search-per-minute-user:60}")
    private int publicSearchPerMinuteUser;

    @Value("${anti-crawler.rate-limit.public-detail-per-minute-ip:60}")
    private int publicDetailPerMinuteIp;

    @Value("${anti-crawler.rate-limit.metadata-per-minute-ip:30}")
    private int metadataPerMinuteIp;

    @Value("${anti-crawler.rate-limit.metadata-per-minute-user:60}")
    private int metadataPerMinuteUser;

    @Value("${anti-crawler.rate-limit.captcha-generate-per-minute-ip:5}")
    private int captchaGeneratePerMinuteIp;

    @Value("${anti-crawler.rate-limit.captcha-verify-per-10m-ip:10}")
    private int captchaVerifyPerTenMinutesIp;

    public RateLimitService(SecurityStateStore stateStore, Clock clock) {
        this.stateStore = stateStore;
        this.clock = clock;
    }

    public LimitDecision checkRequestLimit(String path, String clientIp, String fingerprint, Long userId) {
        if (isCaptchaGenerateEndpoint(path)) {
            return consume("captcha-generate", "ip", clientIp, captchaGeneratePerMinuteIp, ONE_MINUTE, false);
        }

        if (isCaptchaVerifyEndpoint(path)) {
            return consume("captcha-verify", "ip", clientIp, captchaVerifyPerTenMinutesIp, TEN_MINUTES, false);
        }

        if (isSearchEndpoint(path)) {
            LimitDecision ipDecision = consume("search", "ip", clientIp, publicSearchPerMinuteIp, ONE_MINUTE, true);
            if (ipDecision != null) {
                return ipDecision;
            }

            LimitDecision fingerprintDecision = consume(
                    "search",
                    "fingerprint",
                    fingerprint,
                    publicSearchPerMinuteFingerprint,
                    ONE_MINUTE,
                    true
            );
            if (fingerprintDecision != null) {
                return fingerprintDecision;
            }

            return consume("search", "user", userId == null ? null : String.valueOf(userId), publicSearchPerMinuteUser, ONE_MINUTE, true);
        }

        if (isBookDetailEndpoint(path)) {
            return consume("book-detail", "ip", clientIp, publicDetailPerMinuteIp, ONE_MINUTE, true);
        }

        if (isMetadataEndpoint(path)) {
            LimitDecision ipDecision = consume("metadata", "ip", clientIp, metadataPerMinuteIp, ONE_MINUTE, true);
            if (ipDecision != null) {
                return ipDecision;
            }

            return consume("metadata", "user", userId == null ? null : String.valueOf(userId), metadataPerMinuteUser, ONE_MINUTE, true);
        }

        return null;
    }

    private LimitDecision consume(String routeGroup,
                                  String scope,
                                  String scopeValue,
                                  int capacity,
                                  Duration window,
                                  boolean requireCaptcha) {
        String normalizedScopeValue = normalize(scopeValue);
        if (normalizedScopeValue == null || capacity <= 0) {
            return null;
        }

        double refillTokensPerSecond = capacity / (double) Math.max(window.toSeconds(), 1L);
        SecurityStateStore.TokenBucketResult result = stateStore.consumeTokenBucket(
                rateLimitKey(routeGroup, scope, normalizedScopeValue),
                capacity,
                refillTokensPerSecond,
                clock.millis(),
                window.multipliedBy(2)
        );

        if (result.allowed()) {
            return null;
        }

        return new LimitDecision(
                routeGroup,
                scope,
                "RATE_LIMIT",
                Math.max(result.retryAfterSeconds(), 1L),
                requireCaptcha,
                true,
                result.remainingTokens()
        );
    }

    private String rateLimitKey(String routeGroup, String scope, String scopeValue) {
        return "security:ratelimit:v2:" + routeGroup + ":" + scope + ":" + encodeScopeValue(scopeValue);
    }

    private String encodeScopeValue(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isSearchEndpoint(String path) {
        return path.startsWith("/api/books/search") || path.startsWith("/api/books/advanced-search");
    }

    private boolean isBookDetailEndpoint(String path) {
        return path != null && path.matches("^/api/books/\\d+$");
    }

    private boolean isMetadataEndpoint(String path) {
        return "/api/books/categories".equals(path)
                || "/api/books/languages".equals(path)
                || "/api/statistics/popular-books".equals(path);
    }

    private boolean isCaptchaGenerateEndpoint(String path) {
        return "/api/captcha/generate".equals(path);
    }

    private boolean isCaptchaVerifyEndpoint(String path) {
        return "/api/captcha/verify".equals(path);
    }

    public record LimitDecision(String routeGroup,
                                String scope,
                                String code,
                                long retryAfterSeconds,
                                boolean captchaRequired,
                                boolean countAsViolation,
                                int remainingTokens) {
    }
}
