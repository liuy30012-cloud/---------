package com.library.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.controller.CaptchaController;
import com.library.service.IpBanService;
import com.library.service.RateLimitService;
import com.library.service.RequestPatternAnalyzer;
import com.library.service.SecurityMetricsService;
import com.library.util.ClientIpResolver;
import com.library.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPTCHA_SCORE_THRESHOLD = 70;

    private final RequestPatternAnalyzer patternAnalyzer;
    private final RateLimitService rateLimitService;
    private final IpBanService ipBanService;
    private final CaptchaController captchaController;
    private final SecurityMetricsService securityMetricsService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RequestPatternAnalyzer patternAnalyzer,
                           RateLimitService rateLimitService,
                           IpBanService ipBanService,
                           CaptchaController captchaController,
                           SecurityMetricsService securityMetricsService,
                           JwtUtil jwtUtil,
                           ObjectMapper objectMapper) {
        this.patternAnalyzer = patternAnalyzer;
        this.rateLimitService = rateLimitService;
        this.ipBanService = ipBanService;
        this.captchaController = captchaController;
        this.securityMetricsService = securityMetricsService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIpResolver.resolve(request);
        Long userId = resolveAuthenticatedUserId();
        String fingerprint = request.getHeader("X-Device-FP");
        String userAgent = request.getHeader("User-Agent");

        if (ipBanService.isIpBanned(clientIp)) {
            sendRateLimitResponse(
                response,
                "Current IP is temporarily banned.",
                "IP_BANNED",
                "ip-ban",
                ipBanService.getRemainingBanTime(clientIp),
                true,
                0
            );
            return;
        }

        RateLimitService.LimitDecision decision = rateLimitService.checkRequestLimit(path, clientIp, fingerprint, userId);
        if (decision != null) {
            if (decision.countAsViolation() && ipBanService.registerRateLimitViolation(clientIp, "app")) {
                sendRateLimitResponse(
                    response,
                    "Current IP is temporarily banned.",
                    "IP_BANNED",
                    "ip-ban",
                    ipBanService.getRemainingBanTime(clientIp),
                    true,
                    0
                );
                return;
            }

            sendRateLimitResponse(
                response,
                "Too many requests. Please try again later.",
                decision.code(),
                decision.routeGroup(),
                (int) decision.retryAfterSeconds(),
                decision.captchaRequired(),
                decision.remainingTokens()
            );
            return;
        }

        boolean hasValidPassToken = captchaController.consumePassToken(
            request.getHeader("X-Captcha-Pass"),
            clientIp,
            userAgent,
            fingerprint
        );

        if (isCatalogRequest(path)) {
            int suspicionScore = patternAnalyzer.recordAndAnalyze(clientIp, path, fingerprint);
            if (!hasValidPassToken && patternAnalyzer.hasActiveCooldown(clientIp)) {
                securityMetricsService.recordBotCooldown();
                sendRateLimitResponse(
                    response,
                    "Human verification is required for this request.",
                    "COOLDOWN_ACTIVE",
                    "bot-cooldown",
                    Math.max(patternAnalyzer.getCooldownRemainingSeconds(clientIp), 1),
                    true,
                    0
                );
                return;
            }

            if (!hasValidPassToken && suspicionScore >= CAPTCHA_SCORE_THRESHOLD) {
                sendRateLimitResponse(
                    response,
                    "Human verification is required for this request.",
                    "BOT_CHALLENGE",
                    "bot-challenge",
                    1,
                    true,
                    0
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCatalogRequest(String path) {
        return isSearchEndpoint(path)
            || isBookDetailEndpoint(path)
            || "/api/books/categories".equals(path)
            || "/api/books/languages".equals(path)
            || "/api/statistics/popular-books".equals(path);
    }

    private boolean isBookDetailEndpoint(String path) {
        return path != null && path.matches("^/api/books/\\d+$");
    }

    private boolean isSearchEndpoint(String path) {
        return path.startsWith("/api/books/search") || path.startsWith("/api/books/advanced-search");
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/api/health")
            || path.startsWith("/actuator/health")
            || path.startsWith("/damage-photos/")
            || path.startsWith("/book-covers/");
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }

        try {
            return jwtUtil.getUserIdFromToken(authentication.getCredentials().toString());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void sendRateLimitResponse(HttpServletResponse response,
                                       String message,
                                       String code,
                                       String route,
                                       int retryAfter,
                                       boolean requireCaptcha,
                                       int remainingTokens) throws IOException {
        securityMetricsService.recordRateLimitBlocked(route, code);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter, 1)));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(remainingTokens, 0)));
        response.setHeader("X-Captcha-Required", requireCaptcha ? "true" : "false");

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("code", code);
        body.put("retryAfter", Math.max(retryAfter, 1));
        body.put("captchaRequired", requireCaptcha);
        body.put("remaining", Math.max(remainingTokens, 0));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
