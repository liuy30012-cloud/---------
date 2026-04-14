package com.library.controller;

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

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = userService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = ClientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = userService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "刷新令牌不能为空。"
                ));
            }

            AuthResponse response = userService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "缺少认证令牌。"
                ));
            }

            if (!jwtUtil.validateAccessToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "访问令牌无效或已过期。"
                ));
            }

            String studentId = jwtUtil.getStudentIdFromToken(token);
            UserInfo userInfo = userService.getUserInfo(studentId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", userInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            if (authentication == null || authentication.getCredentials() == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未授权访问。"
                ));
            }

            String token = authentication.getCredentials().toString();
            String studentId = jwtUtil.getStudentIdFromToken(token);
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (oldPassword == null || oldPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "旧密码不能为空。"
                ));
            }

            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "新密码不能为空。"
                ));
            }

            if (newPassword.length() < 8 || newPassword.length() > 20) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "新密码长度必须在 8 到 20 个字符之间。"
                ));
            }

            Map<String, Object> result = userService.changePassword(studentId, oldPassword, newPassword);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/account-status")
    public ResponseEntity<?> getAccountStatus(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "学工号不能为空。"
            ));
        }

        var user = userService.findByStudentId(studentId);
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "exists", false
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "exists", true,
            "active", user.getStatus() == 1,
            "hasEmail", user.getEmail() != null && !user.getEmail().isBlank(),
            "hasPhone", user.getPhone() != null && !user.getPhone().isBlank()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            jwtUtil.invalidateToken(token);
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "退出成功"
        ));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
