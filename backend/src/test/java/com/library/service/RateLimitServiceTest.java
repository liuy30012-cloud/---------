package com.library.service;

import com.library.security.InMemorySecurityStateStore;
import com.library.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitServiceTest {

    private MutableClock clock;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        rateLimitService = new RateLimitService(new InMemorySecurityStateStore(clock), clock);
        ReflectionTestUtils.setField(rateLimitService, "publicSearchPerMinuteIp", 2);
        ReflectionTestUtils.setField(rateLimitService, "publicSearchPerMinuteFingerprint", 10);
        ReflectionTestUtils.setField(rateLimitService, "publicSearchPerMinuteUser", 2);
        ReflectionTestUtils.setField(rateLimitService, "publicDetailPerMinuteIp", 10);
        ReflectionTestUtils.setField(rateLimitService, "metadataPerMinuteIp", 1);
        ReflectionTestUtils.setField(rateLimitService, "captchaGeneratePerMinuteIp", 1);
        ReflectionTestUtils.setField(rateLimitService, "captchaVerifyPerTenMinutesIp", 2);
    }

    @Test
    void searchIpLimitRefillsGraduallyInsteadOfResettingOnBucketBoundary() {
        assertNull(rateLimitService.checkRequestLimit("/api/books/search", "1.1.1.1", null, null));
        assertNull(rateLimitService.checkRequestLimit("/api/books/search", "1.1.1.1", null, null));

        RateLimitService.LimitDecision blocked = rateLimitService.checkRequestLimit("/api/books/search", "1.1.1.1", null, null);
        assertNotNull(blocked);
        assertEquals("search", blocked.routeGroup());

        clock.advance(Duration.ofSeconds(30));
        assertNull(rateLimitService.checkRequestLimit("/api/books/search", "1.1.1.1", null, null));
    }

    @Test
    void searchUserLimitAppliesAcrossDifferentIps() {
        assertNull(rateLimitService.checkRequestLimit("/api/books/search", "1.1.1.1", null, 42L));
        assertNull(rateLimitService.checkRequestLimit("/api/books/search", "2.2.2.2", null, 42L));

        RateLimitService.LimitDecision blocked = rateLimitService.checkRequestLimit("/api/books/search", "3.3.3.3", null, 42L);
        assertNotNull(blocked);
        assertEquals("user", blocked.scope());
    }

    @Test
    void metadataAndCaptchaEndpointsUseIndependentPolicies() {
        assertNull(rateLimitService.checkRequestLimit("/api/books/categories", "5.5.5.5", null, null));
        assertNotNull(rateLimitService.checkRequestLimit("/api/books/categories", "5.5.5.5", null, null));

        assertNull(rateLimitService.checkRequestLimit("/api/captcha/generate", "6.6.6.6", null, null));
        RateLimitService.LimitDecision captchaBlocked = rateLimitService.checkRequestLimit("/api/captcha/generate", "6.6.6.6", null, null);
        assertNotNull(captchaBlocked);
        assertEquals("captcha-generate", captchaBlocked.routeGroup());
    }
}
