package com.library.controller;

import com.library.service.DataExportService;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;
    private final JwtUtil jwtUtil;

    @GetMapping("/borrow-history")
    public ResponseEntity<?> exportBorrowHistory(
            @RequestParam(defaultValue = "excel") String format,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);

        try {
            if ("json".equalsIgnoreCase(format)) {
                String json = dataExportService.exportBorrowHistoryToJson(userId);
                String filename = "借阅历史_" + getTimestamp() + ".json";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json);
            } else {
                byte[] excelData = dataExportService.exportBorrowHistoryToExcel(userId);
                String filename = "借阅历史_" + getTimestamp() + ".xlsx";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(excelData);
            }
        } catch (IOException e) {
            log.error("Failed to export borrow history for user {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body("{\"success\": false, \"message\": \"导出失败，请稍后重试。\"}");
        }
    }

    @GetMapping("/book-reviews")
    public ResponseEntity<?> exportBookReviews(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);

        try {
            byte[] excelData = dataExportService.exportBookReviewsToExcel(userId);
            String filename = "我的评价_" + getTimestamp() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);
        } catch (IOException e) {
            log.error("Failed to export book reviews for user {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body("{\"success\": false, \"message\": \"导出失败，请稍后重试。\"}");
        }
    }

    @GetMapping("/all-data")
    public ResponseEntity<?> exportAllData(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);

        try {
            byte[] borrowData = dataExportService.exportBorrowHistoryToExcel(userId);
            byte[] reviewData = dataExportService.exportBookReviewsToExcel(userId);

            // 这里简化处理，实际可以打包成ZIP
            String filename = "我的数据_" + getTimestamp() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(borrowData);
        } catch (IOException e) {
            log.error("Failed to export all data for user {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body("{\"success\": false, \"message\": \"导出失败，请稍后重试。\"}");
        }
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }
        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
