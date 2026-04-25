package com.library.service;

import com.library.security.InMemorySecurityStateStore;
import com.library.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestPatternAnalyzerTest {

    private MutableClock clock;
    private RequestPatternAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        analyzer = new RequestPatternAnalyzer(new InMemorySecurityStateStore(clock), clock);
        ReflectionTestUtils.setField(analyzer, "suspicionThreshold", 50);
        ReflectionTestUtils.setField(analyzer, "cooldownBaseSeconds", 30);
        ReflectionTestUtils.setField(analyzer, "cooldownMaxSeconds", 90);
    }

    @Test
    void repeatedSequentialRequestsTriggerSuspicionAndCooldown() {
        for (int i = 1; i <= 12; i++) {
            analyzer.recordAndAnalyze("1.1.1.1", "/api/books/" + i, "fp-1");
            clock.advance(Duration.ofSeconds(1));
        }

        assertTrue(analyzer.getSuspicionScore("1.1.1.1") >= 50);
        assertTrue(analyzer.hasActiveCooldown("1.1.1.1"));
        assertTrue(analyzer.getCooldownRemainingSeconds("1.1.1.1") > 0);
    }

    @Test
    void sharedFingerprintRaisesAdditionalSuspicion() {
        for (int i = 1; i <= 6; i++) {
            analyzer.recordAndAnalyze("192.0.2." + i, "/api/books/search", "shared-fingerprint");
        }

        assertTrue(analyzer.analyzeFingerprint("192.0.2.1", "shared-fingerprint") > 0);
    }

    @Test
    void captchaFailuresEscalateCooldown() {
        analyzer.recordCaptchaFailure("203.0.113.9", "fp-captcha");

        assertTrue(analyzer.getSuspicionScore("203.0.113.9") > 0);
        assertTrue(analyzer.hasActiveCooldown("203.0.113.9"));
    }
}
