package com.library.dto;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationMessageTest {

    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    ValidationMessageTest() {
        validator.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void loginRequestUsesChineseValidationMessages() {
        LoginRequest request = new LoginRequest();
        request.setStudentId("");
        request.setPassword("");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "学号不能为空".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "密码不能为空".equals(v.getMessage())));
    }

    @Test
    void registerRequestUsesChineseValidationMessages() {
        RegisterRequest request = new RegisterRequest();
        request.setStudentId("abc");
        request.setUsername("a");
        request.setPassword("short");
        request.setConfirmPassword("");
        request.setEmail("bad-email");
        request.setPhone("123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> "学号必须为 7 到 10 位数字".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "用户名长度必须在 2 到 20 个字符之间".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "密码长度必须在 8 到 20 个字符之间".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "确认密码不能为空".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "邮箱格式不正确".equals(v.getMessage())));
        assertTrue(violations.stream().anyMatch(v -> "手机号格式不正确".equals(v.getMessage())));
    }
}
