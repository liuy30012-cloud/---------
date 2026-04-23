package com.library.service;

import com.library.security.SecurityStateStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Slf4j
@Service
public class AntiCrawlerRequestInspector {

    private static final Set<String> CRAWLER_UA_KEYWORDS = Set.of(
        "curl", "wget", "scrapy", "python-requests", "python-urllib",
        "httpclient", "java/", "go-http-client", "node-fetch",
        "php/", "ruby", "perl", "libwww", "mechanize",
        "phantom", "headless", "selenium", "puppeteer", "playwright",
        "bot", "spider", "crawl", "scraper", "harvest",
        "absbot", "ahrefsbot", "semrushbot", "dotbot"
    );

    private final SecurityStateStore stateStore;

    @Value("${anti-crawler.signature.enabled:false}")
    private boolean signatureEnabled;

    @Value("${anti-crawler.signature.secret}")
    private String signatureSecret;

    @Value("${anti-crawler.signature.time-window:60}")
    private int timeWindow;

    @Value("${anti-crawler.ua-check.enabled:true}")
    private boolean uaCheckEnabled;

    public AntiCrawlerRequestInspector(SecurityStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @PostConstruct
    public void validateSignatureConfig() {
        if (!signatureEnabled) {
            return;
        }

        if (signatureSecret == null
            || signatureSecret.isBlank()
            || signatureSecret.length() < 32) {
            throw new IllegalStateException(
                "anti-crawler.signature.enabled=true requires anti-crawler.signature.secret with at least 32 characters"
            );
        }
    }

    public Decision inspect(String path,
                            String clientIp,
                            String userAgent,
                            String accept,
                            String signature,
                            String timestamp,
                            String nonce) {
        if (isExemptPath(path)) {
            return Decision.allow();
        }

        if (uaCheckEnabled) {
            if (userAgent == null || userAgent.trim().isEmpty()) {
                return Decision.block("请求已被拒绝。", "MISSING_UA");
            }

            String lowerUa = userAgent.toLowerCase();
            for (String keyword : CRAWLER_UA_KEYWORDS) {
                if (lowerUa.contains(keyword)) {
                    log.warn("Blocked crawler user agent: ip={} ua={}", clientIp, userAgent);
                    return Decision.block("请求已被拒绝。", "BLOCKED_UA");
                }
            }

            if (!isAuthEndpoint(path) && (accept == null || accept.trim().isEmpty())) {
                log.debug("Request missing Accept header: ip={} path={}", clientIp, path);
            }
        }

        if (signatureEnabled && !isSignatureExempt(path)) {
            if (signature == null || timestamp == null || nonce == null) {
                return Decision.block("请求校验失败。", "MISSING_SIGN");
            }

            if (!validateSignature(signature, timestamp, nonce, path, clientIp)) {
                return Decision.block("请求校验失败。", "INVALID_SIGN");
            }
        }

        return Decision.allow();
    }

    private boolean validateSignature(String signature, String timestamp, String nonce, String path, String clientIp) {
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            return false;
        }

        long diff = Math.abs(System.currentTimeMillis() - requestTime);
        if (diff > timeWindow * 1000L) {
            log.warn("Signature timestamp out of window: ip={} diff={}ms", clientIp, diff);
            return false;
        }

        String nonceKey = "security:anti-crawler:nonce:" + nonce;
        boolean nonceAccepted = stateStore.setIfAbsent(nonceKey, clientIp, Duration.ofSeconds(timeWindow));
        if (!nonceAccepted) {
            log.warn("Nonce replay detected: ip={} nonce={}", clientIp, nonce);
            return false;
        }

        String expectedSignature = computeHmacSha256(timestamp + path + nonce, signatureSecret);
        return expectedSignature != null && expectedSignature.equals(signature);
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("Failed to compute HMAC signature", ex);
            return null;
        }
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/");
    }

    private boolean isSignatureExempt(String path) {
        return path.startsWith("/api/auth/")
            || path.startsWith("/api/captcha/")
            || path.startsWith("/api/health")
            || path.startsWith("/actuator/health");
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/api/health") || path.startsWith("/actuator/health");
    }

    public record Decision(boolean allowed, String message, String code) {
        static Decision allow() {
            return new Decision(true, null, null);
        }

        static Decision block(String message, String code) {
            return new Decision(false, message, code);
        }
    }
}
