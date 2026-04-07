package com.library.filter;

import com.library.security.InMemorySecurityStateStore;
import com.library.service.AntiCrawlerRequestInspector;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AntiCrawlerFilterConfigTest {

    @Test
    void rejectsDefaultSecretWhenSignatureEnabled() {
        AntiCrawlerRequestInspector inspector = new AntiCrawlerRequestInspector(new InMemorySecurityStateStore());
        ReflectionTestUtils.setField(inspector, "signatureEnabled", true);
        ReflectionTestUtils.setField(inspector, "signatureSecret", "library-anti-crawler-shared-secret-2026");

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(inspector, "validateSignatureConfig"));
    }

    @Test
    void acceptsCustomSecretWhenSignatureEnabled() {
        AntiCrawlerRequestInspector inspector = new AntiCrawlerRequestInspector(new InMemorySecurityStateStore());
        ReflectionTestUtils.setField(inspector, "signatureEnabled", true);
        ReflectionTestUtils.setField(inspector, "signatureSecret", "super-unique-secret-1234567890-extra");

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(inspector, "validateSignatureConfig"));
    }

    @Test
    void rejectsShortSecretWhenSignatureEnabled() {
        AntiCrawlerRequestInspector inspector = new AntiCrawlerRequestInspector(new InMemorySecurityStateStore());
        ReflectionTestUtils.setField(inspector, "signatureEnabled", true);
        ReflectionTestUtils.setField(inspector, "signatureSecret", "short-secret-value");

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(inspector, "validateSignatureConfig"));
    }
}
