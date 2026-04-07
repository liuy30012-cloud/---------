package com.library.service;

import com.library.dto.RegisterRequest;
import com.library.dto.UserInfo;
import com.library.model.User;
import com.library.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致。");
        }

        validatePasswordStrength(request.getPassword());
        ensureUniqueIdentity(request.getStudentId(), request.getEmail());

        User user = new User();
        user.setStudentId(request.getStudentId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setLoginCount(0);

        try {
            User saved = userRepository.save(user);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("user", UserInfo.fromUser(saved));
            return result;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("学号或邮箱已存在，请检查后重试。", ex);
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserInfo getUserInfo(String studentId) {
        User user = getUserByStudentId(studentId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }
        return UserInfo.fromUser(user);
    }

    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByStatus(1);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码长度不能少于 8 个字符。");
        }
        if (password.length() > 20) {
            throw new IllegalArgumentException("密码长度不能超过 20 个字符。");
        }

        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasDigit) {
            throw new IllegalArgumentException("密码必须包含数字。");
        }
        if (!hasLowerCase || !hasUpperCase) {
            throw new IllegalArgumentException("密码必须同时包含大小写字母。");
        }
        if (!hasSpecialChar) {
            throw new IllegalArgumentException("密码必须包含特殊字符。");
        }
    }

    @Transactional
    public void ensureDefaultUsersExist() {
        ensureBuiltInUser(
            "2021001",
            "张三",
            "Test123456",
            "zhangsan@example.com",
            "STUDENT"
        );
        ensureBuiltInUser(
            "admin001",
            "系统管理员",
            "Admin123456!",
            "admin@library.local",
            "ADMIN"
        );
    }

    private void ensureBuiltInUser(String studentId, String username, String rawPassword, String email, String role) {
        if (userRepository.existsByStudentId(studentId)) {
            return;
        }

        User builtInUser = new User();
        builtInUser.setStudentId(studentId);
        builtInUser.setUsername(username);
        builtInUser.setPassword(encodePassword(rawPassword));
        builtInUser.setEmail(email);
        builtInUser.setRole(role);
        builtInUser.setStatus(1);
        builtInUser.setLoginCount(0);
        userRepository.save(builtInUser);
    }

    private void ensureUniqueIdentity(String studentId, String email) {
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("该学号已注册。");
        }

        String normalizedEmail = blankToNull(email);
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("该邮箱已被使用。");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
