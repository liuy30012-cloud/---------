package com.library.security;

import com.library.testsupport.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySecurityStateStoreTest {

    @Test
    void setIfAbsentRejectsReplayUntilKeyExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        InMemorySecurityStateStore store = new InMemorySecurityStateStore(clock);

        assertTrue(store.setIfAbsent("nonce:abc", "1.1.1.1", Duration.ofSeconds(30)));
        assertFalse(store.setIfAbsent("nonce:abc", "1.1.1.1", Duration.ofSeconds(30)));

        clock.advance(Duration.ofSeconds(31));

        assertTrue(store.setIfAbsent("nonce:abc", "1.1.1.1", Duration.ofSeconds(30)));
    }
}
