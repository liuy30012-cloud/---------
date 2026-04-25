package com.library.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.controller.CaptchaController;
import com.library.service.IpBanService;
import com.library.service.RateLimitService;
import com.library.service.RequestPatternAnalyzer;
import com.library.service.SecurityMetricsService;
import com.library.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RequestPatternAnalyzer patternAnalyzer;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private IpBanService ipBanService;

    @Mock
    private CaptchaController captchaController;

    @Mock
    private SecurityMetricsService securityMetricsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"/damage-photos/demo.jpg", "/book-covers/demo.jpg"})
    void skipsRateLimitingForPublicAssetPaths(String path) throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
            patternAnalyzer,
            rateLimitService,
            ipBanService,
            captchaController,
            securityMetricsService,
            jwtUtil,
            objectMapper
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(patternAnalyzer, rateLimitService, ipBanService, captchaController, securityMetricsService, jwtUtil);
    }
}
