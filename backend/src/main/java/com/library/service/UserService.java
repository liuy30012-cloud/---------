package com.library.service;

import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserInfo;
import com.library.model.LoginLog;
import com.library.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserAccountService userAccountService;
    private final UserAuthenticationService userAuthenticationService;
    private final UserLoginSecurityService userLoginSecurityService;

    public UserService(UserAccountService userAccountService,
                       UserAuthenticationService userAuthenticationService,
                       UserLoginSecurityService userLoginSecurityService) {
        this.userAccountService = userAccountService;
        this.userAuthenticationService = userAuthenticationService;
        this.userLoginSecurityService = userLoginSecurityService;
    }

    public Map<String, Object> register(RegisterRequest request) {
        return userAccountService.register(request);
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        return userAuthenticationService.login(request, ipAddress, userAgent);
    }

    public AuthResponse refreshToken(String refreshToken) {
        return userAuthenticationService.refreshToken(refreshToken);
    }

    public UserInfo getUserInfo(String studentId) {
        return userAccountService.getUserInfo(studentId);
    }

    public User getUserById(Long userId) {
        return userAccountService.getUserById(userId);
    }

    public Map<String, Object> changePassword(String studentId, String oldPassword, String newPassword) {
        return userAuthenticationService.changePassword(studentId, oldPassword, newPassword);
    }

    public List<LoginLog> getLoginLogs(String studentId, int limit) {
        return userLoginSecurityService.getLoginLogs(studentId, limit);
    }

    public long getUserCount() {
        return userAccountService.getUserCount();
    }

    public long getActiveUserCount() {
        return userAccountService.getActiveUserCount();
    }

    public User findByStudentId(String studentId) {
        return userAccountService.getUserByStudentId(studentId);
    }

    public void clearSecurityStateForTests() {
        userLoginSecurityService.clearStateForTests();
    }
}
