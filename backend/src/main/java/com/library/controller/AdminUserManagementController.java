package com.library.controller;

import com.library.constant.AdminUserManagementMessageKeys;
import com.library.dto.AdminUserRoleUpdateRequest;
import com.library.dto.AdminUserStatisticsDTO;
import com.library.dto.AdminUserStatusUpdateRequest;
import com.library.dto.ApiResponse;
import com.library.dto.UserDTO;
import com.library.service.AdminUserManagementService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;
    private final JwtUtil jwtUtil;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserDTO> users = adminUserManagementService.getUsers(keyword, role, status, page, size);
        return ApiResponse.okWithPagination(
            users.getContent(),
            (int) users.getTotalElements(),
            users.getNumber(),
            users.getSize(),
            users.getTotalPages()
        );
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<AdminUserStatisticsDTO>> getStatistics() {
        return ApiResponse.ok(adminUserManagementService.getStatistics());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDTO>> updateRole(
        @PathVariable Long id,
        @Valid @RequestBody AdminUserRoleUpdateRequest request,
        Authentication authentication
    ) {
        UserDTO updatedUser = adminUserManagementService.updateUserRole(id, getUserIdFromAuth(authentication), request.getRole());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User role updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDTO>> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdminUserStatusUpdateRequest request,
        Authentication authentication
    ) {
        UserDTO updatedUser = adminUserManagementService.updateUserStatus(id, getUserIdFromAuth(authentication), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User status updated"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.AUTH_TOKEN_MISSING);
        }
        try {
            return jwtUtil.getUserIdFromToken(authentication.getCredentials().toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.AUTH_INVALID);
        }
    }
}
