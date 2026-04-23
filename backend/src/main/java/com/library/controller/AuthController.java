package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserInfo;
import com.library.service.UserService;
import com.library.util.ClientIpResolver;
import com.library.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = userService.register(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = ClientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = userService.login(request, ipAddress, userAgent);
        return ApiResponse.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error("刷新令牌不能为空。");
        }

        try {
            AuthResponse response = userService.refreshToken(refreshToken);
            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(HttpServletRequest request) {
        String token = jwtUtil.extractToken(request);
        if (token == null) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED, "缺少认证令牌。");
        }

        if (!jwtUtil.validateAccessToken(token)) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED, "访问令牌无效或已过期。");
        }

        try {
            String studentId = jwtUtil.getStudentIdFromToken(token);
            UserInfo userInfo = userService.getUserInfo(studentId);
            return ApiResponse.ok(userInfo);
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED, "未授权访问。");
        }

        String token = authentication.getCredentials().toString();
        String studentId = jwtUtil.getStudentIdFromToken(token);
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return ApiResponse.error("旧密码不能为空。");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            return ApiResponse.error("新密码不能为空。");
        }

        if (newPassword.length() < 8 || newPassword.length() > 20) {
            return ApiResponse.error("新密码长度必须在 8 到 20 个字符之间。");
        }

        try {
            Map<String, Object> result = userService.changePassword(studentId, oldPassword, newPassword);
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/account-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountStatus(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        if (studentId == null || studentId.isBlank()) {
            return ApiResponse.error("学工号不能为空。");
        }

        var user = userService.findByStudentId(studentId);
        if (user == null) {
            return ApiResponse.ok(Map.of(
                "exists", false
            ));
        }

        return ApiResponse.ok(Map.of(
            "exists", true,
            "active", user.getStatus() == 1,
            "hasEmail", user.getEmail() != null && !user.getEmail().isBlank(),
            "hasPhone", user.getPhone() != null && !user.getPhone().isBlank()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = jwtUtil.extractToken(request);
        if (token != null) {
            jwtUtil.invalidateToken(token);
        }
        return ApiResponse.ok(null, "退出成功");
    }

}
