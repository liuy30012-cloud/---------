package com.library.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.LibraryApplication;
import com.library.controller.CaptchaController;
import com.library.service.IpBanService;
import com.library.service.RequestPatternAnalyzer;
import com.library.util.ClientIpResolver;
import com.library.util.JwtUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = LibraryApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "anti-crawler.rate-limit.public-search-per-minute-ip=5",
                "anti-crawler.rate-limit.public-search-per-minute-fingerprint=5",
                "anti-crawler.rate-limit.public-search-per-minute-user=5",
                "anti-crawler.rate-limit.public-detail-per-minute-ip=100",
                "anti-crawler.rate-limit.metadata-per-minute-ip=1",
                "anti-crawler.rate-limit.captcha-generate-per-minute-ip=20",
                "anti-crawler.rate-limit.captcha-verify-per-10m-ip=20",
                "anti-crawler.challenge.pass-token-ttl=120",
                "anti-crawler.challenge.pass-token-uses=2",
                "anti-crawler.challenge.suspicion-threshold=35",
                "anti-crawler.honeypot.dry-run=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHotfixIntegrationTest {

    private static final String USER_AGENT = "Mozilla/5.0 Test Browser";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IpBanService ipBanService;

    @Autowired
    private CaptchaController captchaController;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SecurityStateStore securityStateStore;

    @Autowired
    private RequestPatternAnalyzer requestPatternAnalyzer;

    @Autowired
    private MeterRegistry meterRegistry;

    @AfterEach
    void resetTrustedProxies() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("127.0.0.1/32", "::1/128"));
        requestPatternAnalyzer.reset();
        securityStateStore.clearAll();
    }

    @Test
    void bannedIpStillRejectedWithValidCaptchaPassToken() throws Exception {
        String clientIp = "1.1.1.1";
        String fingerprint = "fp-1";
        String passToken = issueCaptchaPassToken(clientIp, USER_AGENT, fingerprint);
        ipBanService.banIp(clientIp, 60, "app");

        MvcResult result = mockMvc.perform(get("/api/books/search")
                        .param("keyword", "java")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("X-Device-FP", fingerprint)
                        .header("X-Captcha-Pass", passToken)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Captcha-Required", "true"))
                .andReturn();

        assertEquals("IP_BANNED", readBody(result).path("code").asText());
    }

    @Test
    void captchaGenerateNoLongerLeaksTargetCoordinates() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/captcha/generate")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("X-Device-FP", "fp-generate")
                        .with(request -> {
                            request.setRemoteAddr("2.2.2.2");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readBody(result).path("data");
        assertTrue(data.path("bgImage").asText().startsWith("data:image/png;base64,"));
        assertTrue(data.path("pieceImage").asText().startsWith("data:image/png;base64,"));
        assertTrue(data.hasNonNull("trackWidth"));
        assertTrue(data.hasNonNull("pieceStartOffset"));
        assertNull(data.get("targetX"));
        assertNull(data.get("targetY"));
    }

    @Test
    void captchaPassTokenIsBoundToFingerprintAndLimitedByUsage() throws Exception {
        String token = issueCaptchaPassToken("3.3.3.3", USER_AGENT, "fp-3");

        assertFalse(captchaController.consumePassToken(token, "3.3.3.3", USER_AGENT, "other-fp"));
        assertTrue(captchaController.consumePassToken(token, "3.3.3.3", USER_AGENT, "fp-3"));
        assertTrue(captchaController.consumePassToken(token, "3.3.3.3", USER_AGENT, "fp-3"));
        assertFalse(captchaController.consumePassToken(token, "3.3.3.3", USER_AGENT, "fp-3"));
    }

    @Test
    void honeypotRoutesAreReachableAndBanSuspiciousClients() throws Exception {
        double before = counterValue("library.security.honeypot", "endpoint", "/api/admin/data-export");
        String clientIp = "4.4.4.4";

        mockMvc.perform(get("/api/admin/data-export")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk());

        assertTrue(ipBanService.isIpBanned(clientIp));
        assertTrue(counterValue("library.security.honeypot", "endpoint", "/api/admin/data-export") > before);
    }

    @Test
    void metadataRateLimitEmitsSecurityMetrics() throws Exception {
        double before = counterValue("library.security.rate_limit.blocked", "route", "metadata", "code", "RATE_LIMIT");
        String clientIp = "5.5.5.5";

        mockMvc.perform(get("/api/books/categories")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk());

        MvcResult blocked = mockMvc.perform(get("/api/books/categories")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertEquals("RATE_LIMIT", readBody(blocked).path("code").asText());
        assertTrue(counterValue("library.security.rate_limit.blocked", "route", "metadata", "code", "RATE_LIMIT") > before);
    }

    @Test
    void sequentialBookEnumerationEventuallyTriggersCaptchaChallenge() throws Exception {
        String clientIp = "6.6.6.6";
        String fingerprint = "fp-cooldown";
        String code = null;

        for (int i = 1; i <= 20; i++) {
            MvcResult result = mockMvc.perform(get("/api/books/" + i)
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "application/json")
                            .header("X-Device-FP", fingerprint)
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            }))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                code = readBody(result).path("code").asText();
                break;
            }
        }

        assertNotNull(code);
        assertTrue(Set.of("BOT_CHALLENGE", "COOLDOWN_ACTIVE").contains(code));
    }

    @Test
    void clientIpResolverIgnoresForgedXffFromUntrustedProxy() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("10.0.0.0/8"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("8.8.8.8", ClientIpResolver.resolve(request));
    }

    @Test
    void clientIpResolverUsesTrustedProxyChains() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("10.0.0.0/8", "203.0.113.0/24"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.9");

        assertEquals("1.2.3.4", ClientIpResolver.resolve(request));
    }

    private String issueCaptchaPassToken(String clientIp, String userAgent, String fingerprint) throws Exception {
        MvcResult generateResult = mockMvc.perform(get("/api/captcha/generate")
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .header("X-Device-FP", fingerprint)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode generateJson = readBody(generateResult);
        String sessionId = generateJson.path("data").path("sessionId").asText();
        String storedSession = securityStateStore.get("security:captcha:session:" + sessionId);
        JsonNode storedSessionJson = objectMapper.readTree(storedSession);
        int expectedOffset = storedSessionJson.path("expectedOffset").asInt();

        String verifyBody = objectMapper.writeValueAsString(Map.of(
                "sessionId", sessionId,
                "sliderX", expectedOffset,
                "dragTime", 420,
                "dragTrail", List.of(
                        Map.of("x", 0, "y", 10, "t", 0),
                        Map.of("x", expectedOffset / 3, "y", 12, "t", 120),
                        Map.of("x", (expectedOffset * 2) / 3, "y", 9, "t", 260),
                        Map.of("x", expectedOffset, "y", 13, "t", 420)
                )
        ));

        MvcResult verifyResult = mockMvc.perform(post("/api/captcha/verify")
                        .contentType("application/json")
                        .content(verifyBody)
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .header("X-Device-FP", fingerprint)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        return readBody(verifyResult).path("data").path("passToken").asText();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private double counterValue(String name, String... tags) {
        Counter counter = meterRegistry.find(name).tags(tags).counter();
        return counter == null ? 0D : counter.count();
    }
}
