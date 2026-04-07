package com.library.controller;

import com.library.model.NotificationRecord;
import com.library.service.NotificationService;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<NotificationRecord> getAllNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return notificationService.getUserNotifications(userId);
    }

    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAsRead(userId, id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已标记为已读。");
        return result;
    }

    @PutMapping("/read-all")
    public Map<String, Object> markAllAsRead(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        long count = notificationService.markAllAsRead(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", count);
        return result;
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
