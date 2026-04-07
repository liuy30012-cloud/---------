package com.library.controller;

import com.library.dto.BorrowTrendDTO;
import com.library.dto.CategoryStatisticsDTO;
import com.library.dto.DashboardDataDTO;
import com.library.dto.InventoryAlertSummaryDTO;
import com.library.dto.InventoryStatisticsDTO;
import com.library.dto.PopularBookDTO;
import com.library.dto.PurchaseSuggestionSummaryDTO;
import com.library.dto.UserProfileDTO;
import com.library.service.StatisticsService;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final JwtUtil jwtUtil;

    @GetMapping("/popular-books")
    public ResponseEntity<?> getPopularBooks(@RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;

            List<PopularBookDTO> popularBooks = statisticsService.getPopularBooks(limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", popularBooks);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/borrow-trends")
    public ResponseEntity<?> getBorrowTrends(@RequestParam(defaultValue = "30") int days) {
        try {
            if (days < 1) days = 30;
            if (days > 365) days = 365;

            List<BorrowTrendDTO> trends = statisticsService.getBorrowTrends(days);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", trends);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/category-statistics")
    public ResponseEntity<?> getCategoryStatistics() {
        try {
            List<CategoryStatisticsDTO> statistics = statisticsService.getCategoryStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", statistics);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/user-profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            UserProfileDTO profile = statisticsService.getUserProfile(userId);

            if (profile == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "用户数据不存在");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", profile);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inventory-statistics")
    public ResponseEntity<?> getInventoryStatistics() {
        try {
            InventoryStatisticsDTO statistics = statisticsService.getInventoryStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", statistics);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            DashboardDataDTO dashboard = statisticsService.getDashboardData();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dashboard);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/inventory-alerts")
    public ResponseEntity<?> getInventoryAlerts() {
        try {
            InventoryAlertSummaryDTO alerts = statisticsService.getInventoryAlerts();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", alerts);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/purchase-suggestions")
    public ResponseEntity<?> getPurchaseSuggestions() {
        try {
            PurchaseSuggestionSummaryDTO suggestions = statisticsService.getPurchaseSuggestions();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", suggestions);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("未登录或令牌无效。");
        }

        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
