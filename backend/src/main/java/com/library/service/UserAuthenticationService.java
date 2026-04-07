package com.library.service;

import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.UserInfo;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserAuthenticationService {

    private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserAccountService userAccountService;
    private final UserLoginSecurityService userLoginSecurityService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Clock clock;

    public UserAuthenticationService(UserAccountService userAccountService,
                                     UserLoginSecurityService userLoginSecurityService,
                                     UserRepository userRepository,
                                     JwtUtil jwtUtil,
                                     Clock clock) {
        this.userAccountService = userAccountService;
        this.userLoginSecurityService = userLoginSecurityService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String studentId = request.getStudentId();
        userLoginSecurityService.assertNotLocked(studentId, ipAddress, userAgent);

        User user = userAccountService.getUserByStudentId(studentId);
        String passwordToVerify = user != null ? user.getPassword() : DUMMY_BCRYPT_HASH;
        boolean passwordMatches = userAccountService.passwordMatches(request.getPassword(), passwordToVerify);

        if (user == null) {
            userLoginSecurityService.handleLoginFailure(studentId, ipAddress, userAgent, "用户不存在");
            throw new IllegalArgumentException("学号或密码错误。");
        }

        if (user.getStatus() == 0) {
            userLoginSecurityService.recordDisabledAccountAttempt(user, ipAddress, userAgent);
            throw new IllegalArgumentException("账号已被停用，请联系管理员。");
        }

        if (!passwordMatches) {
            userLoginSecurityService.handleLoginFailure(studentId, ipAddress, userAgent, "密码错误");
            throw new IllegalArgumentException("学号或密码错误。");
        }

        userLoginSecurityService.clearFailures(studentId);

        LocalDateTime loginTime = now();
        userRepository.incrementLoginStats(user.getId(), loginTime);
        user.setLastLoginTime(loginTime);
        user.incrementLoginCount();
        userLoginSecurityService.recordLoginSuccess(user, ipAddress, userAgent);

        Long tokenExpiration = 86_400_000L;
        Long refreshExpiration = 604_800_000L;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            tokenExpiration = 604_800_000L;
            refreshExpiration = 2_592_000_000L;
        }

        return new AuthResponse(
            jwtUtil.generateToken(user.getStudentId(), user.getRole(), user.getId(), tokenExpiration),
            jwtUtil.generateRefreshToken(user.getStudentId(), user.getId(), refreshExpiration),
            tokenExpiration,
            UserInfo.fromUser(user)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("刷新令牌无效。");
        }

        String studentId = jwtUtil.getStudentIdFromToken(refreshToken);
        User user = userAccountService.getUserByStudentId(studentId);
        if (user == null || user.getStatus() == 0) {
            throw new IllegalArgumentException("用户不存在或账号已停用。");
        }

        return new AuthResponse(
            jwtUtil.generateToken(user.getStudentId(), user.getRole(), user.getId(), 86_400_000L),
            jwtUtil.generateRefreshToken(user.getStudentId(), user.getId(), 604_800_000L),
            86_400_000L,
            UserInfo.fromUser(user)
        );
    }

    @Transactional
    public Map<String, Object> changePassword(String studentId, String oldPassword, String newPassword) {
        User user = userAccountService.getUserByStudentId(studentId);
        String passwordToVerify = user != null ? user.getPassword() : DUMMY_BCRYPT_HASH;
        boolean passwordMatches = userAccountService.passwordMatches(oldPassword, passwordToVerify);

        if (user == null || !passwordMatches) {
            throw new IllegalArgumentException("旧密码错误。");
        }

        userAccountService.validatePasswordStrength(newPassword);
        if (userAccountService.passwordMatches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同。");
        }

        user.setPassword(userAccountService.encodePassword(newPassword));
        userAccountService.save(user);
        jwtUtil.invalidateAllUserTokens(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "密码修改成功，请重新登录。");
        return result;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
