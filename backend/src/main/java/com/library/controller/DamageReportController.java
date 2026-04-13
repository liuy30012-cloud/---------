package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.DamageReportResponse;
import com.library.dto.DamageReportStatistics;
import com.library.dto.UpdateDamageReportStatusPayload;
import com.library.service.DamageReportService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/damage-reports")
@RequiredArgsConstructor
public class DamageReportController {

    private final DamageReportService damageReportService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<DamageReportResponse>> createReport(
        @RequestParam Long bookId,
        @RequestParam String damageTypes,
        @RequestParam(required = false) String description,
        @RequestParam("photos") List<MultipartFile> photos,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        DamageReportResponse response = damageReportService.createReport(bookId, userId, damageTypes, description, photos);
        return ResponseEntity.ok(ApiResponse.success(response, "损坏报告提交成功"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DamageReportResponse>>> getMyReports(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Page<DamageReportResponse> reports = damageReportService.getMyReports(userId, page, size);
        return ApiResponse.okWithPagination(reports.getContent(), (int) reports.getTotalElements(),
            page, size, reports.getTotalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DamageReportResponse>> getReportDetail(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        DamageReportResponse response = damageReportService.getReportDetail(id, userId, isAdmin);
        return ApiResponse.ok(response);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DamageReportResponse>>> getAllReports(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Page<DamageReportResponse> reports = damageReportService.getAllReports(status, page, size);
        return ApiResponse.okWithPagination(reports.getContent(), (int) reports.getTotalElements(),
            page, size, reports.getTotalPages());
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DamageReportStatistics>> getStatistics() {
        return ApiResponse.ok(damageReportService.getStatistics());
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DamageReportResponse>> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateDamageReportStatusPayload payload,
        Authentication authentication
    ) {
        Long adminId = getUserIdFromAuth(authentication);
        DamageReportResponse response = damageReportService.updateStatus(id, adminId, payload);
        return ResponseEntity.ok(ApiResponse.success(response, "报告状态已更新"));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        damageReportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success(null, "报告已删除"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌");
        }
        try {
            return jwtUtil.getUserIdFromToken(authentication.getCredentials().toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException("认证信息无效");
        }
    }
}
