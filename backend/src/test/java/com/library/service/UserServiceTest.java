package com.library.service;

import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.testsupport.BorrowTestApplication;
import com.library.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BorrowTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class UserServiceTest {

    private static final String TEST_PASSWORD = "Test123!A";
    private static final AtomicInteger STUDENT_SEQUENCE = new AtomicInteger(2_000);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @AfterEach
    void resetSecurityState() {
        userService.clearSecurityStateForTests();
    }

    @Test
    @DisplayName("Concurrent login failures remain consistent")
    void testLoginFailureCounterThreadSafety() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    LoginRequest loginRequest = new LoginRequest("9999999999", "wrongpass");
                    loginRequest.setRememberMe(false);
                    userService.login(loginRequest, "127.0.0.1", "test-agent");
                    unexpectedErrors.add(new AssertionError("login should have failed"));
                } catch (IllegalArgumentException e) {
                    failureCount.incrementAndGet();
                } catch (Throwable throwable) {
                    unexpectedErrors.add(throwable);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS), "workers did not become ready in time");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "login failure workers did not finish in time");
        executor.shutdownNow();

        assertTrue(unexpectedErrors.isEmpty(), "unexpected errors: " + unexpectedErrors);
        assertEquals(threadCount, failureCount.get());
    }

    @Test
    @DisplayName("Concurrent successful logins update the default user's counters")
    void testUserLoginCountThreadSafety() throws InterruptedException {
        int threadCount = 20;
        int initialLoginCount = userRepository.findByStudentId("2021001").orElseThrow().getLoginCount();

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    LoginRequest loginRequest = new LoginRequest("2021001", "Test123456");
                    loginRequest.setRememberMe(false);
                    userService.login(loginRequest, "127.0.0.1", "test-agent");
                } catch (Throwable throwable) {
                    unexpectedErrors.add(throwable);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS), "workers did not become ready in time");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "successful login workers did not finish in time");
        executor.shutdownNow();

        assertTrue(unexpectedErrors.isEmpty(), "unexpected errors: " + unexpectedErrors);

        User user = userRepository.findByStudentId("2021001").orElseThrow();
        assertEquals(initialLoginCount + threadCount, user.getLoginCount());
        assertNotNull(user.getLastLoginTime());
    }

    @Test
    @DisplayName("Login logs can be recorded repeatedly without losing entries")
    void testLoginLogRecording() {
        String studentId = nextStudentId();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setStudentId(studentId);
        registerRequest.setUsername("Log Test");
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setConfirmPassword(TEST_PASSWORD);
        registerRequest.setEmail(studentId + "@example.com");

        userService.register(registerRequest);

        for (int i = 0; i < 20; i++) {
            LoginRequest loginRequest = new LoginRequest(studentId, TEST_PASSWORD);
            loginRequest.setRememberMe(false);
            userService.login(loginRequest, "127.0.0.1", "test-agent");
        }

        assertEquals(10, userService.getLoginLogs(studentId, 10).size());
    }

    @Test
    @DisplayName("Registration succeeds with a strong password")
    void testSuccessfulRegistration() {
        String studentId = nextStudentId();
        RegisterRequest request = new RegisterRequest();
        request.setStudentId(studentId);
        request.setUsername("New User");
        request.setPassword(TEST_PASSWORD);
        request.setConfirmPassword(TEST_PASSWORD);
        request.setEmail(studentId + "@example.com");

        var result = userService.register(request);

        assertTrue((Boolean) result.get("success"));
        assertEquals("注册成功", result.get("message"));
    }

    @Test
    @DisplayName("Weak passwords are rejected")
    void testPasswordStrengthValidation() {
        RegisterRequest request = new RegisterRequest();
        request.setStudentId(nextStudentId());
        request.setUsername("Weak Password");
        request.setPassword("weak");
        request.setConfirmPassword("weak");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            userService.register(request)
        );

        assertEquals("密码长度不能少于 8 个字符。", exception.getMessage());
    }

    @Test
    @DisplayName("Refresh token validation rejects access tokens")
    void testRefreshTokenValidationUsesTokenType() {
        LoginRequest loginRequest = new LoginRequest("2021001", "Test123456");
        loginRequest.setRememberMe(false);

        var authResponse = userService.login(loginRequest, "127.0.0.1", "test-agent");

        assertTrue(jwtUtil.validateAccessToken(authResponse.getToken()));
        assertFalse(jwtUtil.validateRefreshToken(authResponse.getToken()));
        assertTrue(jwtUtil.validateRefreshToken(authResponse.getRefreshToken()));
        assertFalse(jwtUtil.validateAccessToken(authResponse.getRefreshToken()));

        assertNotNull(userService.refreshToken(authResponse.getRefreshToken()));
        assertThrows(IllegalArgumentException.class, () -> userService.refreshToken(authResponse.getToken()));
    }

    private String nextStudentId() {
        return String.format("8%09d", STUDENT_SEQUENCE.incrementAndGet());
    }
}
