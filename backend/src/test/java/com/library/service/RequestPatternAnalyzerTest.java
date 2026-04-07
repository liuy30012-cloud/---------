package com.library.service;

import com.library.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        analyzer = new RequestPatternAnalyzer(clock);
    }

    @Test
    void repeatedSequentialRequestsTriggerSuspicionAndDelay() {
        for (int i = 1; i <= 12; i++) {
            analyzer.recordAndAnalyze("1.1.1.1", "/api/books/" + i, "fp-1");
            clock.advance(Duration.ofSeconds(1));
        }

        assertTrue(analyzer.getSuspicionScore("1.1.1.1") > 50);
        assertTrue(analyzer.getProgressiveDelay("1.1.1.1") > 0);
    }

    @Test
    void heavilySharedFingerprintRaisesAdditionalSuspicion() {
        for (int i = 1; i <= 6; i++) {
            analyzer.recordAndAnalyze("192.0.2." + i, "/api/books/search", "shared-fingerprint");
        }

        assertTrue(analyzer.analyzeFingerprint("192.0.2.1", "shared-fingerprint") > 0);
    }
}
