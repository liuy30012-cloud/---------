package com.library.config;

import com.library.util.ClientIpResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedProxyConfigurerTest {

    @AfterEach
    void resetTrustedProxies() {
        ClientIpResolver.setTrustedProxyCidrs(List.of(
                "127.0.0.1/32",
                "::1/128",
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16"
        ));
    }

    @Test
    void antiCrawlerPropertyOverridesLegacySecurityProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("anti-crawler.trusted-proxies[0]", "203.0.113.0/24")
                .withProperty("security.trusted-proxies[0]", "198.51.100.0/24");

        new TrustedProxyConfigurer(environment).configureTrustedProxies();

        assertTrue(ClientIpResolver.isTrustedProxy("203.0.113.10"));
        assertFalse(ClientIpResolver.isTrustedProxy("198.51.100.10"));
    }

    @Test
    void legacySecurityPropertyStillWorksWhenNewPropertyMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("security.trusted-proxies[0]", "198.51.100.0/24");

        new TrustedProxyConfigurer(environment).configureTrustedProxies();

        assertTrue(ClientIpResolver.isTrustedProxy("198.51.100.10"));
    }
}
