package com.library.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<?> readiness() {
        Map<String, Object> readiness = new HashMap<>();

        // 检查数据库连接
        boolean dbHealthy = checkDatabase();

        readiness.put("database", dbHealthy ? "UP" : "DOWN");
        readiness.put("status", dbHealthy ? "UP" : "DOWN");

        if (dbHealthy) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(503).body(readiness);
        }
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
