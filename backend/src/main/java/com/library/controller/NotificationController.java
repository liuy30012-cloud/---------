package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.model.NotificationRecord;
import com.library.service.NotificationService;
import com.library.util.JwtUtil;
import com.library.util.PageableHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<ApiResponse<Page<NotificationRecord>>> getAllNotifications(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<NotificationRecord> notifications = notificationService.getUserNotifications(userId, pageable);
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
