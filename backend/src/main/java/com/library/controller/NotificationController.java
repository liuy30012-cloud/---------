package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.model.NotificationRecord;
import com.library.service.NotificationService;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getAllNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<NotificationRecord> notifications = notificationService.getUserNotifications(userId);
        return ApiResponse.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAsRead(userId, id);
        return ApiResponse.ok(null, "已标记为已读。");
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Long>>> markAllAsRead(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        long count = notificationService.markAllAsRead(userId);
        return ApiResponse.ok(Map.of("count", count));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
