package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.ReadingStatusRequest;
import com.library.dto.ReadingStatusResponse;
import com.library.model.ReadingStatus;
import com.library.service.ReadingStatusService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ReadingStatusRequest request) {
        Long userId = extractUserId(token);
        ReadingStatusResponse response = readingStatusService.upsertReadingStatus(
                userId, request.getBookId(), request.getStatus(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.success(response, "阅读状态更新成功"));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeReadingStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        Long userId = extractUserId(token);
        readingStatusService.removeReadingStatus(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(null, "阅读状态已删除"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReadingStatusResponse>>> getUserReadingStatuses(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) ReadingStatus status) {
        Long userId = extractUserId(token);
        List<ReadingStatusResponse> statuses = readingStatusService.getUserReadingStatuses(userId, status);
        return ResponseEntity.ok(ApiResponse.success(statuses, "获取阅读状态列表成功"));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<ReadingStatusResponse>> getReadingStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        Long userId = extractUserId(token);
        ReadingStatusResponse response = readingStatusService.getReadingStatus(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(response, "获取阅读状态成功"));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatusCounts(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserId(token);
        Map<String, Long> counts = readingStatusService.getStatusCounts(userId);
        return ResponseEntity.ok(ApiResponse.success(counts, "获取状态计数成功"));
    }

    private Long extractUserId(String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(jwt);
    }
}
