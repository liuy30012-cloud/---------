package com.library.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.ApiResponse;
import com.library.security.SecurityStateStore;
import com.library.service.RequestPatternAnalyzer;
import com.library.service.SecurityMetricsService;
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

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
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
    private static final int BACKGROUND_WIDTH = 320;
    private static final int BACKGROUND_HEIGHT = 168;
    private static final int SLIDER_WIDTH = 44;
    private static final int PIECE_START_OFFSET = 8;
    private static final Duration CAPTCHA_SESSION_TTL = Duration.ofMinutes(2);

    private final SecurityStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RequestPatternAnalyzer requestPatternAnalyzer;
    private final SecurityMetricsService securityMetricsService;

    @Value("${anti-crawler.challenge.pass-token-ttl:120}")
    private long passTokenTtlSeconds;

    @Value("${anti-crawler.challenge.pass-token-uses:10}")
    private int passTokenUses;

    @Value("${anti-crawler.challenge.bind-user-agent:true}")
    private boolean bindUserAgent;

    public CaptchaController(SecurityStateStore stateStore,
                             ObjectMapper objectMapper,
                             Clock clock,
                             RequestPatternAnalyzer requestPatternAnalyzer,
                             SecurityMetricsService securityMetricsService) {
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.requestPatternAnalyzer = requestPatternAnalyzer;
        this.securityMetricsService = securityMetricsService;
    }

    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCaptcha(HttpServletRequest request) {
        String clientIp = ClientIpResolver.resolve(request);
        String userAgentHash = hashValue(request.getHeader("User-Agent"));
        String fingerprintHash = hashOptionalValue(request.getHeader("X-Device-FP"));

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        int targetX = 80 + ThreadLocalRandom.current().nextInt(BACKGROUND_WIDTH - SLIDER_WIDTH - 100);
        int targetY = 16 + ThreadLocalRandom.current().nextInt(BACKGROUND_HEIGHT - SLIDER_WIDTH - 32);
        int expectedOffset = targetX - PIECE_START_OFFSET;

        CaptchaSessionRecord session = new CaptchaSessionRecord(
                sessionId,
                expectedOffset,
                targetY,
                clock.millis(),
                clientIp,
                userAgentHash,
                fingerprintHash
        );
        saveSession(session);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("backgroundWidth", BACKGROUND_WIDTH);
        data.put("backgroundHeight", BACKGROUND_HEIGHT);
        data.put("sliderWidth", SLIDER_WIDTH);
        data.put("trackWidth", BACKGROUND_WIDTH - SLIDER_WIDTH);
        data.put("pieceStartOffset", PIECE_START_OFFSET);
        data.put("bgImage", renderBackgroundImage(sessionId, targetX, targetY));
        data.put("pieceImage", renderPieceImage(targetY));
        data.put("expiresIn", CAPTCHA_SESSION_TTL.toSeconds());

        securityMetricsService.recordCaptcha("generate", "success");
        return ApiResponse.ok(data);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCaptcha(@RequestBody Map<String, Object> body,
                                                                          HttpServletRequest request) {
        String sessionId = (String) body.get("sessionId");
        Number sliderXNum = (Number) body.get("sliderX");
        Number dragTimeNum = (Number) body.get("dragTime");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dragTrail = (List<Map<String, Object>>) body.get("dragTrail");

        if (sessionId == null || sliderXNum == null) {
            securityMetricsService.recordCaptcha("verify", "invalid-request");
            return ApiResponse.error("验证码参数不完整");
        }

        CaptchaSessionRecord session = loadSession(sessionId);
        if (session == null) {
            securityMetricsService.recordCaptcha("verify", "expired");
            return ApiResponse.error("验证码已过期，请刷新");
        }
        stateStore.delete(sessionKey(sessionId));

        String clientIp = ClientIpResolver.resolve(request);
        String userAgentHash = hashValue(request.getHeader("User-Agent"));
        String fingerprintHash = hashOptionalValue(request.getHeader("X-Device-FP"));
        if (!session.clientIp().equals(clientIp)
                || (bindUserAgent && !session.userAgentHash().equals(userAgentHash))
                || (!session.fingerprintHash().isEmpty() && !session.fingerprintHash().equals(fingerprintHash))) {
            securityMetricsService.recordCaptcha("verify", "context-mismatch");
            return ApiResponse.error("验证码上下文已失效，请刷新");
        }

        int sliderX = sliderXNum.intValue();
        int dragTime = dragTimeNum != null ? dragTimeNum.intValue() : 0;
        if (Math.abs(sliderX - session.expectedOffset()) > CAPTCHA_TOLERANCE) {
            requestPatternAnalyzer.recordCaptchaFailure(clientIp, request.getHeader("X-Device-FP"));
            securityMetricsService.recordCaptcha("verify", "wrong-offset");
            return ApiResponse.ok(Map.of("verified", false), "验证失败，请重试");
        }

        if (dragTime < 200 || !validateDragTrail(dragTrail)) {
            requestPatternAnalyzer.recordCaptchaFailure(clientIp, request.getHeader("X-Device-FP"));
            securityMetricsService.recordCaptcha("verify", "invalid-trail");
            return ApiResponse.ok(Map.of("verified", false), "验证失败，请重试");
        }

        long now = clock.millis();
        String passToken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        PassTokenRecord record = new PassTokenRecord(
                passToken,
                clientIp,
                userAgentHash,
                fingerprintHash,
                now + Duration.ofSeconds(passTokenTtlSeconds).toMillis(),
                Math.max(passTokenUses, 1)
        );
        savePassToken(record);

        securityMetricsService.recordCaptcha("verify", "success");
        return ApiResponse.ok(Map.of(
                "verified", true,
                "passToken", passToken,
                "expiresIn", passTokenTtlSeconds,
                "remainingUses", record.remainingUses()
        ), "验证通过");
    }

    public boolean isPassTokenValid(String passToken, String clientIp, String userAgent, String fingerprint) {
        return validatePassToken(loadPassToken(passToken), clientIp, userAgent, fingerprint, false);
    }

    public boolean isPassTokenValid(String passToken, String clientIp, String userAgent) {
        return isPassTokenValid(passToken, clientIp, userAgent, null);
    }

    public boolean consumePassToken(String passToken, String clientIp, String userAgent, String fingerprint) {
        PassTokenRecord record = loadPassToken(passToken);
        if (!validatePassToken(record, clientIp, userAgent, fingerprint, true)) {
            return false;
        }

        long now = clock.millis();
        if (record.remainingUses() <= 1) {
            stateStore.delete(passTokenKey(passToken));
            return true;
        }

        Duration ttl = Duration.ofMillis(Math.max(record.expiry() - now, 1L));
        savePassToken(record.withRemainingUses(record.remainingUses() - 1), ttl);
        return true;
    }

    private boolean validatePassToken(PassTokenRecord record,
                                      String clientIp,
                                      String userAgent,
                                      String fingerprint,
                                      boolean deleteWhenInvalid) {
        if (record == null || record.expiry() <= clock.millis() || record.remainingUses() <= 0) {
            if (deleteWhenInvalid && record != null) {
                stateStore.delete(passTokenKey(record.token()));
            }
            return false;
        }

        if (!record.clientIp().equals(clientIp)) {
            return false;
        }

        if (bindUserAgent && !record.userAgentHash().equals(hashValue(userAgent))) {
            return false;
        }

        String fingerprintHash = hashOptionalValue(fingerprint);
        return record.fingerprintHash().isEmpty() || record.fingerprintHash().equals(fingerprintHash);
    }

    private boolean validateDragTrail(List<Map<String, Object>> trail) {
        if (trail == null || trail.size() < 4) {
            return false;
        }

        Set<Integer> yValues = new java.util.HashSet<>();
        List<Double> speeds = new java.util.ArrayList<>();

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
        savePassToken(record, Duration.ofSeconds(Math.max(passTokenTtlSeconds, 1L)));
    }

    private void savePassToken(PassTokenRecord record, Duration ttl) {
        stateStore.set(passTokenKey(record.token()), writeValue(record), ttl);
    }

    private PassTokenRecord loadPassToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String payload = stateStore.get(passTokenKey(token));
        return payload == null ? null : readValue(payload, PassTokenRecord.class);
    }

    private String renderBackgroundImage(String sessionId, int targetX, int targetY) {
        BufferedImage image = new BufferedImage(BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            prepareCanvas(g2d);
            g2d.setPaint(new GradientPaint(0, 0, new Color(226, 235, 221), BACKGROUND_WIDTH, BACKGROUND_HEIGHT, new Color(249, 233, 202)));
            g2d.fillRect(0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

            long seed = sessionId.hashCode();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 12; i++) {
                int x = (int) Math.floorMod(seed + i * 37L, BACKGROUND_WIDTH - 24);
                int y = random.nextInt(0, BACKGROUND_HEIGHT - 20);
                int width = 18 + random.nextInt(26);
                int height = 8 + random.nextInt(18);
                g2d.setColor(new Color(255, 255, 255, 90));
                g2d.fillRoundRect(x, y, width, height, 10, 10);
            }

            g2d.setColor(new Color(82, 101, 88, 135));
            g2d.fill(new RoundRectangle2D.Double(targetX, targetY, SLIDER_WIDTH, SLIDER_WIDTH, 12, 12));
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.setStroke(new BasicStroke(2F));
            g2d.draw(new RoundRectangle2D.Double(targetX + 1, targetY + 1, SLIDER_WIDTH - 2, SLIDER_WIDTH - 2, 12, 12));
        } finally {
            g2d.dispose();
        }
        return toDataUri(image);
    }

    private String renderPieceImage(int targetY) {
        BufferedImage image = new BufferedImage(BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            prepareCanvas(g2d);
            g2d.setPaint(new GradientPaint(
                    PIECE_START_OFFSET,
                    targetY,
                    new Color(117, 158, 123, 235),
                    PIECE_START_OFFSET + SLIDER_WIDTH,
                    targetY + SLIDER_WIDTH,
                    new Color(197, 140, 72, 235)
            ));
            g2d.fill(new RoundRectangle2D.Double(PIECE_START_OFFSET, targetY, SLIDER_WIDTH, SLIDER_WIDTH, 12, 12));
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.setStroke(new BasicStroke(2F));
            g2d.draw(new RoundRectangle2D.Double(PIECE_START_OFFSET + 1, targetY + 1, SLIDER_WIDTH - 2, SLIDER_WIDTH - 2, 12, 12));
        } finally {
            g2d.dispose();
        }
        return toDataUri(image);
    }

    private void prepareCanvas(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private String toDataUri(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render captcha image", ex);
        }
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

    private String hashValue(String value) {
        String source = value == null ? "" : value;
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

    private String hashOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return hashValue(value);
    }

    private String sessionKey(String sessionId) {
        return "security:captcha:session:" + sessionId;
    }

    private String passTokenKey(String token) {
        return "security:captcha:pass:" + token;
    }

    private record CaptchaSessionRecord(String sessionId,
                                        int expectedOffset,
                                        int pieceTop,
                                        long createdAt,
                                        String clientIp,
                                        String userAgentHash,
                                        String fingerprintHash) {
    }

    private record PassTokenRecord(String token,
                                   String clientIp,
                                   String userAgentHash,
                                   String fingerprintHash,
                                   long expiry,
                                   int remainingUses) {
        private PassTokenRecord withRemainingUses(int nextRemainingUses) {
            return new PassTokenRecord(token, clientIp, userAgentHash, fingerprintHash, expiry, nextRemainingUses);
        }
    }
}
