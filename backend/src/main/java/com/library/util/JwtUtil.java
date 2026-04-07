package com.library.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private final java.util.concurrent.ConcurrentHashMap<String, Date> tokenBlacklist =
        new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.Set<String>> userTokens =
        new java.util.concurrent.ConcurrentHashMap<>();

    private java.util.concurrent.ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 32 bytes (256 bits) for HS256. " +
                "Current length: " + (secret != null ? secret.getBytes(StandardCharsets.UTF_8).length : 0) + " bytes. " +
                "Please update jwt.secret in application.yml"
            );
        }

        cleanupExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jwt-blacklist-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupBlacklist, 1, 1, java.util.concurrent.TimeUnit.HOURS);
    }

    @jakarta.annotation.PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            log.info("JWT 黑名单清理线程正在关闭...");
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("JWT 黑名单清理线程未能及时结束，准备强制关闭。");
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("JWT 黑名单清理线程关闭时被中断", e);
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String studentId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "access");
        return createToken(claims, studentId, expiration);
    }

    public String generateToken(String studentId, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("type", "access");
        String token = createToken(claims, studentId, expiration);

        trackUserToken(userId, token);
        return token;
    }

    public String generateToken(String studentId, String role, Long userId, Long customExpiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("type", "access");
        String token = createToken(claims, studentId, customExpiration);

        trackUserToken(userId, token);
        return token;
    }

    public String generateRefreshToken(String studentId, Long userId, Long customExpiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", userId);
        String token = createToken(claims, studentId, customExpiration);

        trackUserToken(userId, token);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public String getStudentIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("role");
    }

    public Long getUserIdFromToken(String token) {
        Object userId = getClaimsFromToken(token).get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("令牌中缺少用户标识。");
        }
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("令牌中的用户标识格式无效。");
            }
        }
        throw new IllegalArgumentException("令牌中的用户标识类型无效: " + userId.getClass().getName());
    }

    public boolean validateToken(String token) {
        try {
            if (tokenBlacklist.containsKey(token)) {
                return false;
            }
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        return validateTokenByType(token, "access", true);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenByType(token, "refresh", false);
    }

    public void invalidateToken(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            tokenBlacklist.put(token, expiration);
            log.info("Token 已加入黑名单，过期时间为 {}", expiration);
            cleanupBlacklist();
        } catch (Exception e) {
            log.debug("Token 失效处理失败，但不影响注销流程: {}", e.getMessage());
        }
    }

    private void cleanupBlacklist() {
        Date now = new Date();
        tokenBlacklist.entrySet().removeIf(entry -> entry.getValue().before(now));

        userTokens.values().forEach(tokens ->
            tokens.removeIf(token -> {
                try {
                    return isTokenExpired(token);
                } catch (Exception e) {
                    return true;
                }
            })
        );
        userTokens.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private synchronized void trackUserToken(Long userId, String token) {
        userTokens.computeIfAbsent(userId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(token);
    }

    public void invalidateAllUserTokens(Long userId) {
        java.util.Set<String> tokens = userTokens.remove(userId);
        if (tokens != null) {
            log.info("用户 {} 的全部 Token 将被失效处理，共 {} 个", userId, tokens.size());
            int invalidatedCount = 0;
            for (String token : tokens) {
                try {
                    Date expiration = getClaimsFromToken(token).getExpiration();
                    tokenBlacklist.put(token, expiration);
                    invalidatedCount++;
                } catch (Exception e) {
                    log.debug("忽略无法解析的 Token");
                }
            }
            log.debug("成功失效处理 {} 个 Token", invalidatedCount);
        } else {
            log.debug("用户 {} 当前没有需要失效的 Token", userId);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean validateTokenByType(String token, String expectedType, boolean allowLegacyAccessToken) {
        try {
            if (tokenBlacklist.containsKey(token)) {
                return false;
            }

            Claims claims = getClaimsFromToken(token);
            String type = (String) claims.get("type");

            if (type == null) {
                return allowLegacyAccessToken && "access".equals(expectedType);
            }

            return expectedType.equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
