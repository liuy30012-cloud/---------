package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.ReadingStatusRequest;
import com.library.dto.ReadingStatusResponse;
import com.library.model.ReadingStatus;
import com.library.service.ReadingStatusService;
import com.library.util.JwtUtil;
import com.library.util.PageableHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reading-status")
@RequiredArgsConstructor
public class ReadingStatusController {

    private final ReadingStatusService readingStatusService;
    private final JwtUtil jwtUtil;

    @PutMapping
    public ResponseEntity<ApiResponse<ReadingStatusResponse>> upsertReadingStatus(
            Authentication authentication,
            @Valid @RequestBody ReadingStatusRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        ReadingStatusResponse response = readingStatusService.upsertReadingStatus(
                userId, request.getBookId(), request.getStatus(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.success(response, "阅读状态更新成功"));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeReadingStatus(
            Authentication authentication,
            @PathVariable Long bookId) {
        Long userId = getUserIdFromAuth(authentication);
        readingStatusService.removeReadingStatus(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(null, "阅读状态已删除"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReadingStatusResponse>>> getUserReadingStatuses(
            Authentication authentication,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String[] sort) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<ReadingStatusResponse> statuses = readingStatusService
                .getUserReadingStatuses(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(statuses, "获取阅读状态列表成功"));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<ReadingStatusResponse>> getReadingStatus(
            Authentication authentication,
            @PathVariable Long bookId) {
        Long userId = getUserIdFromAuth(authentication);
        ReadingStatusResponse response = readingStatusService.getReadingStatus(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(response, "获取阅读状态成功"));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatusCounts(
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Long> counts = readingStatusService.getStatusCounts(userId);
        return ResponseEntity.ok(ApiResponse.success(counts, "获取状态计数成功"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
