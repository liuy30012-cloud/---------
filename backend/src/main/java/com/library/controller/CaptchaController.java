package com.library.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import com.library.security.SecurityStateStore;
import com.library.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final int CAPTCHA_TOLERANCE = 5;
    private static final Duration CAPTCHA_SESSION_TTL = Duration.ofMinutes(2);

    private final SecurityStateStore stateStore;
    private final ObjectMapper objectMapper;

    @Value("${anti-crawler.challenge.pass-token-ttl:600}")
    private long passTokenTtlSeconds;

    @Value("${anti-crawler.challenge.bind-user-agent:true}")
    private boolean bindUserAgent;

    public CaptchaController(SecurityStateStore stateStore, ObjectMapper objectMapper) {
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCaptcha(HttpServletRequest request) {
        String clientIp = ClientIpResolver.resolve(request);
        String userAgentHash = hashUserAgent(request.getHeader("User-Agent"));

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        int bgWidth = 320;
        int bgHeight = 168;
        int sliderWidth = 44;
        int targetX = 80 + ThreadLocalRandom.current().nextInt(bgWidth - sliderWidth - 100);
        int targetY = 16 + ThreadLocalRandom.current().nextInt(bgHeight - sliderWidth - 32);

        List<Map<String, Integer>> puzzlePieces = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            puzzlePieces.add(Map.of(
                    "x", ThreadLocalRandom.current().nextInt(20, bgWidth - sliderWidth),
                    "y", ThreadLocalRandom.current().nextInt(10, bgHeight - sliderWidth)
            ));
        }

        CaptchaSessionRecord session = new CaptchaSessionRecord(
                sessionId,
                targetX,
                targetY,
                System.currentTimeMillis(),
                clientIp,
                userAgentHash
        );
        saveSession(session);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("backgroundWidth", bgWidth);
        data.put("backgroundHeight", bgHeight);
        data.put("sliderWidth", sliderWidth);
        data.put("targetX", targetX);
        data.put("targetY", targetY);
        data.put("puzzlePieces", puzzlePieces);
        data.put("expiresIn", CAPTCHA_SESSION_TTL.toSeconds());
        return ApiResponse.ok(data);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCaptcha(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String sessionId = (String) body.get("sessionId");
        Number sliderXNum = (Number) body.get("sliderX");
        Number dragTimeNum = (Number) body.get("dragTime");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dragTrail = (List<Map<String, Object>>) body.get("dragTrail");

        if (sessionId == null || sliderXNum == null) {
            return ApiResponse.error("验证码参数不完整");
        }

        CaptchaSessionRecord session = loadSession(sessionId);
        if (session == null) {
            return ApiResponse.error("验证码已过期，请刷新");
        }
        stateStore.delete(sessionKey(sessionId));

        String clientIp = ClientIpResolver.resolve(request);
        String userAgentHash = hashUserAgent(request.getHeader("User-Agent"));
        if (!session.clientIp().equals(clientIp) || (bindUserAgent && !session.userAgentHash().equals(userAgentHash))) {
            return ApiResponse.error("验证码上下文已失效，请刷新");
        }

        int sliderX = sliderXNum.intValue();
        int dragTime = dragTimeNum != null ? dragTimeNum.intValue() : 0;

        if (Math.abs(sliderX - session.targetX()) > CAPTCHA_TOLERANCE) {
            log.warn("Captcha verification failed: IP={} targetX={} sliderX={}", clientIp, session.targetX(), sliderX);
            return ApiResponse.ok(Map.of("verified", false), "验证失败，请重试");
        }

        if (dragTime < 200 || !validateDragTrail(dragTrail)) {
            log.warn("Captcha drag trail rejected: IP={} dragTime={}", clientIp, dragTime);
            return ApiResponse.ok(Map.of("verified", false), "验证失败，请重试");
        }

        long now = System.currentTimeMillis();
        String passToken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        PassTokenRecord record = new PassTokenRecord(
                passToken,
                clientIp,
                userAgentHash,
                now + Duration.ofSeconds(passTokenTtlSeconds).toMillis()
        );
        savePassToken(record);

        return ApiResponse.ok(Map.of(
                "verified", true,
                "passToken", passToken,
                "expiresIn", passTokenTtlSeconds
        ), "验证通过");
    }

    public boolean isPassTokenValid(String passToken, String clientIp, String userAgent) {
        if (passToken == null || passToken.isBlank()) {
            return false;
        }

        PassTokenRecord record = loadPassToken(passToken);
        if (record == null) {
            return false;
        }

        if (!record.clientIp().equals(clientIp)) {
            return false;
        }

        if (bindUserAgent && !record.userAgentHash().equals(hashUserAgent(userAgent))) {
            return false;
        }

        if (record.expiry() <= System.currentTimeMillis()) {
            stateStore.delete(passTokenKey(passToken));
            return false;
        }

        return true;
    }

    private boolean validateDragTrail(List<Map<String, Object>> trail) {
        if (trail == null || trail.size() < 4) {
            return false;
        }

        Set<Integer> yValues = new HashSet<>();
        List<Double> speeds = new ArrayList<>();

        for (Map<String, Object> point : trail) {
            Number y = (Number) point.get("y");
            if (y != null) {
                yValues.add(y.intValue());
            }
        }

        for (int i = 1; i < trail.size(); i++) {
            Number x1 = (Number) trail.get(i - 1).get("x");
            Number x2 = (Number) trail.get(i).get("x");
            Number t1 = (Number) trail.get(i - 1).get("t");
            Number t2 = (Number) trail.get(i).get("t");

            if (x1 == null || x2 == null || t1 == null || t2 == null) {
                continue;
            }

            double deltaTime = t2.doubleValue() - t1.doubleValue();
            if (deltaTime > 0) {
                speeds.add(Math.abs(x2.doubleValue() - x1.doubleValue()) / deltaTime);
            }
        }

        if (yValues.size() <= 1) {
            return false;
        }

        if (speeds.size() < 3) {
            return true;
        }

        double mean = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        if (mean == 0D) {
            return false;
        }

        double variance = speeds.stream()
                .mapToDouble(speed -> Math.pow(speed - mean, 2))
                .average()
                .orElse(0D);
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        return coefficientOfVariation > 0.05D;
    }

    private void saveSession(CaptchaSessionRecord session) {
        stateStore.set(sessionKey(session.sessionId()), writeValue(session), CAPTCHA_SESSION_TTL);
    }

    private CaptchaSessionRecord loadSession(String sessionId) {
        String payload = stateStore.get(sessionKey(sessionId));
        return payload == null ? null : readValue(payload, CaptchaSessionRecord.class);
    }

    private void savePassToken(PassTokenRecord record) {
        stateStore.set(passTokenKey(record.token()), writeValue(record), Duration.ofSeconds(passTokenTtlSeconds));
    }

    private PassTokenRecord loadPassToken(String token) {
        String payload = stateStore.get(passTokenKey(token));
        return payload == null ? null : readValue(payload, PassTokenRecord.class);
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize captcha state", ex);
        }
    }

    private <T> T readValue(String payload, Class<T> valueType) {
        try {
            return objectMapper.readValue(payload, valueType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize captcha state", ex);
        }
    }

    private String hashUserAgent(String userAgent) {
        String source = userAgent == null ? "" : userAgent;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String sessionKey(String sessionId) {
        return "security:captcha:session:" + sessionId;
    }

    private String passTokenKey(String token) {
        return "security:captcha:pass:" + token;
    }

    private record CaptchaSessionRecord(
            String sessionId,
            int targetX,
            int targetY,
            long createdAt,
            String clientIp,
            String userAgentHash
    ) {
    }

    private record PassTokenRecord(
            String token,
            String clientIp,
            String userAgentHash,
            long expiry
    ) {
    }
}
