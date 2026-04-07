package com.library.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.LibraryApplication;
import com.library.controller.CaptchaController;
import com.library.security.SecurityStateStore;
import com.library.service.IpBanService;
import com.library.service.RequestPatternAnalyzer;
import com.library.util.ClientIpResolver;
import com.library.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
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

    @AfterEach
    void resetTrustedProxies() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("127.0.0.1/32", "::1/128"));
        requestPatternAnalyzer.reset();
        securityStateStore.clearAll();
    }

    @Test
    void bannedIpStillRejectedWithValidCaptchaPassToken() throws Exception {
        String clientIp = "1.1.1.1";
        String passToken = issueCaptchaPassToken(clientIp, USER_AGENT);
        ipBanService.banIp(clientIp, 60);

        mockMvc.perform(get("/api/books/search")
                        .param("keyword", "java")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("X-Captcha-Pass", passToken)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Captcha-Required", "true"));
    }

    @Test
    void captchaPassTokenBoundToIpAndUserAgent() throws Exception {
        String passToken = issueCaptchaPassToken("2.2.2.2", USER_AGENT);

        assertTrue(captchaController.isPassTokenValid(passToken, "2.2.2.2", USER_AGENT));
        assertFalse(captchaController.isPassTokenValid(passToken, "3.3.3.3", USER_AGENT));
        assertFalse(captchaController.isPassTokenValid(passToken, "2.2.2.2", USER_AGENT + " changed"));
    }

    @Test
    void unsignedBrowserRequestNoLongerFailsMissingSign() throws Exception {
        mockMvc.perform(get("/api/books/search")
                        .param("keyword", "java")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr("4.4.4.4");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void searchRateLimitWithoutPassTokenTriggersCaptchaChallenge() throws Exception {
        mockMvc.perform(get("/api/books/search")
                        .param("keyword", "history")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr("5.5.5.5");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/search")
                        .param("keyword", "history")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr("5.5.5.5");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Captcha-Required", "true"));
    }

    @Test
    void authenticatedUserLimitAppliesAcrossDifferentIps() throws Exception {
        String token = jwtUtil.generateToken("2021001", "STUDENT", 1L);

        mockMvc.perform(get("/api/books/categories")
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr("6.6.6.1");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/categories")
                        .header("Authorization", "Bearer " + token)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr("6.6.6.2");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void clientIpResolverIgnoresForgedXffFromUntrustedProxy() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("10.0.0.0/8"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertTrue("8.8.8.8".equals(ClientIpResolver.resolve(request)));
    }

    @Test
    void clientIpResolverUsesXffFromTrustedProxyChain() {
        ClientIpResolver.setTrustedProxyCidrs(List.of("10.0.0.0/8", "203.0.113.0/24"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.9");

        assertTrue("1.2.3.4".equals(ClientIpResolver.resolve(request)));
    }

    @Test
    void sensitiveAdminEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/admin/data-export"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/internal/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/swagger.json"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/graphql"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedGlobalLimitViolationsEventuallyBanIp() throws Exception {
        String clientIp = "7.7.7.7";

        mockMvc.perform(get("/api/books/categories")
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk());

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/books/categories")
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "application/json")
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            }))
                    .andExpect(status().isTooManyRequests());
        }

        assertTrue(ipBanService.isIpBanned(clientIp));
    }

    private String issueCaptchaPassToken(String clientIp, String userAgent) throws Exception {
        MvcResult generateResult = mockMvc.perform(get("/api/captcha/generate")
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode generateJson = objectMapper.readTree(generateResult.getResponse().getContentAsString());
        String sessionId = generateJson.get("sessionId").asText();
        int targetX = generateJson.get("targetX").asInt();

        String verifyBody = objectMapper.writeValueAsString(Map.of(
                "sessionId", sessionId,
                "sliderX", targetX,
                "dragTime", 420,
                "dragTrail", List.of(
                        Map.of("x", 0, "y", 10, "t", 0),
                        Map.of("x", targetX / 3, "y", 12, "t", 120),
                        Map.of("x", (targetX * 2) / 3, "y", 9, "t", 260),
                        Map.of("x", targetX, "y", 13, "t", 420)
                )
        ));

        MvcResult verifyResult = mockMvc.perform(post("/api/captcha/verify")
                        .contentType("application/json")
                        .content(verifyBody)
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode verifyJson = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        return verifyJson.get("passToken").asText();
    }
}
