package com.library.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.controller.CaptchaController;
import com.library.service.IpBanService;
import com.library.service.RateLimitService;
import com.library.service.RequestPatternAnalyzer;
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
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RequestPatternAnalyzer patternAnalyzer,
                           RateLimitService rateLimitService,
                           IpBanService ipBanService,
                           CaptchaController captchaController,
                           JwtUtil jwtUtil,
                           ObjectMapper objectMapper) {
        this.patternAnalyzer = patternAnalyzer;
        this.rateLimitService = rateLimitService;
        this.ipBanService = ipBanService;
        this.captchaController = captchaController;
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

        if (ipBanService.isIpBanned(clientIp)) {
            sendRateLimitResponse(response, "当前 IP 已被临时封禁。", ipBanService.getRemainingBanTime(clientIp), true);
            return;
        }

        if (rateLimitService.checkBurstLimit(clientIp, userId)) {
            if (ipBanService.registerRateLimitViolation(clientIp)) {
                sendRateLimitResponse(response, "当前 IP 已被临时封禁。", ipBanService.getRemainingBanTime(clientIp), true);
                return;
            }
            sendRateLimitResponse(response, "请求过于频繁，请完成验证码后继续。", 1, true);
            return;
        }

        if (rateLimitService.checkGlobalLimit(clientIp, userId)) {
            if (ipBanService.registerRateLimitViolation(clientIp)) {
                sendRateLimitResponse(response, "当前 IP 已被临时封禁。", ipBanService.getRemainingBanTime(clientIp), true);
                return;
            }
            sendRateLimitResponse(response, "请求过于频繁，请稍后再试。", 60, true);
            return;
        }

        if (isSearchEndpoint(path) && rateLimitService.checkSearchLimit(clientIp, userId)) {
            sendRateLimitResponse(response, "搜索过于频繁，请稍后再试。", 60, true);
            return;
        }

        String passToken = request.getHeader("X-Captcha-Pass");
        boolean hasValidPassToken = captchaController.isPassTokenValid(
            passToken,
            clientIp,
            request.getHeader("User-Agent")
        );

        String fingerprint = request.getHeader("X-Device-FP");
        int suspicionScore = patternAnalyzer.recordAndAnalyze(clientIp, path, fingerprint);
        suspicionScore += patternAnalyzer.analyzeFingerprint(clientIp, fingerprint);
        int progressiveDelay = patternAnalyzer.getProgressiveDelay(clientIp);

        if (!hasValidPassToken && (progressiveDelay > 0 || suspicionScore >= CAPTCHA_SCORE_THRESHOLD)) {
            int retryAfter = progressiveDelay > 0 ? Math.max(progressiveDelay / 1000, 1) : 1;
            sendRateLimitResponse(response, "当前请求需要完成验证码校验。", retryAfter, true);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSearchEndpoint(String path) {
        return path.startsWith("/api/books/search") || path.startsWith("/api/books/advanced-search");
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/api/captcha/")
            || path.startsWith("/api/health")
            || path.startsWith("/actuator/health");
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
                                       int retryAfter,
                                       boolean requireCaptcha) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter, 1)));
        if (requireCaptcha) {
            response.setHeader("X-Captcha-Required", "true");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("retryAfter", Math.max(retryAfter, 1));
        body.put("captchaRequired", requireCaptcha);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
