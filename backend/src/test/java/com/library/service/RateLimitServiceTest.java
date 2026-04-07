package com.library.service;

import com.library.security.InMemorySecurityStateStore;
import com.library.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private MutableClock clock;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        rateLimitService = new RateLimitService(new InMemorySecurityStateStore(clock), clock);
        ReflectionTestUtils.setField(rateLimitService, "burstSize", 2);
        ReflectionTestUtils.setField(rateLimitService, "globalRpm", 2);
        ReflectionTestUtils.setField(rateLimitService, "searchRpm", 1);
    }

    @Test
    void burstLimitResetsOnNewSecondBucket() {
        assertFalse(rateLimitService.checkBurstLimit("1.1.1.1"));
        assertFalse(rateLimitService.checkBurstLimit("1.1.1.1"));
        assertTrue(rateLimitService.checkBurstLimit("1.1.1.1"));

        clock.advance(Duration.ofSeconds(1));

        assertFalse(rateLimitService.checkBurstLimit("1.1.1.1"));
    }

    @Test
    void userScopedGlobalLimitAppliesAcrossDifferentIps() {
        assertFalse(rateLimitService.checkGlobalLimit("1.1.1.1", 42L));
        assertFalse(rateLimitService.checkGlobalLimit("2.2.2.2", 42L));
        assertTrue(rateLimitService.checkGlobalLimit("3.3.3.3", 42L));
    }

    @Test
    void searchLimitUsesIndependentBucket() {
        assertFalse(rateLimitService.checkSearchLimit("1.1.1.1"));
        assertTrue(rateLimitService.checkSearchLimit("1.1.1.1"));
    }
}
