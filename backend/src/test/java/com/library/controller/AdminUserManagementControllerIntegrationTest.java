package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.LibraryApplication;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.security.SecurityStateStore;
import com.library.service.AdminUserManagementService;
import com.library.service.RequestPatternAnalyzer;
import com.library.service.UserService;
import com.library.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserManagementControllerIntegrationTest {

    private static final String USER_AGENT = "Mozilla/5.0 Test Browser";
    private static final String TEST_PASSWORD = "Test123!A";
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(8_000);
    private static final AtomicInteger IP_SEQUENCE = new AtomicInteger(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminUserManagementService adminUserManagementService;

    @Autowired
    private SecurityStateStore securityStateStore;

    @Autowired
    private RequestPatternAnalyzer requestPatternAnalyzer;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (!createdUserIds.isEmpty()) {
            userRepository.deleteAllByIdInBatch(createdUserIds);
            createdUserIds.clear();
        }

        User builtInAdmin = userRepository.findByStudentId("admin001").orElseThrow();
        builtInAdmin.setRole("ADMIN");
        builtInAdmin.setStatus(1);
        userRepository.save(builtInAdmin);

        securityStateStore.clearAll();
        requestPatternAnalyzer.reset();
        userService.clearSecurityStateForTests();
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/admin/all")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        User student = userRepository.findByStudentId("2021001").orElseThrow();

        mockMvc.perform(get("/api/users/admin/statistics")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(student)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsersWithPaginationAndFilters() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();

        User keywordUser = registerUser("Filter Keyword");
        User teacherUser = registerUser("Filter Teacher");
        teacherUser.setRole("TEACHER");
        userRepository.save(teacherUser);
        User disabledUser = registerUser("Filter Disabled");
        disabledUser.setStatus(0);
        userRepository.save(disabledUser);

        mockMvc.perform(get("/api/users/admin/all")
                .param("page", "0")
                .param("size", "1")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.total").isNumber())
            .andExpect(jsonPath("$.totalPages").isNumber());

        resetSecurityState();

        mockMvc.perform(get("/api/users/admin/all")
                .param("keyword", keywordUser.getStudentId())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].studentId").value(keywordUser.getStudentId()))
            .andExpect(jsonPath("$.data[0].username").value(keywordUser.getUsername()));

        resetSecurityState();

        mockMvc.perform(get("/api/users/admin/all")
                .param("role", "TEACHER")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].studentId").value(teacherUser.getStudentId()))
            .andExpect(jsonPath("$.data[0].role").value("TEACHER"));

        resetSecurityState();

        mockMvc.perform(get("/api/users/admin/all")
                .param("status", "0")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.data[0].studentId").value(disabledUser.getStudentId()))
            .andExpect(jsonPath("$.data[0].status").value(0));
    }

    @Test
    void adminCanGetStatistics() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();
        User disabledUser = registerUser("Stats Disabled");
        disabledUser.setStatus(0);
        userRepository.save(disabledUser);

        mockMvc.perform(get("/api/users/admin/statistics")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalUsers").isNumber())
            .andExpect(jsonPath("$.data.activeUsers").isNumber())
            .andExpect(jsonPath("$.data.disabledUsers").isNumber())
            .andExpect(jsonPath("$.data.activeAdmins").value(1));
    }

    @Test
    void adminCanUpdateRoleAndInvalidateExistingTokens() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();
        User targetUser = registerUser("Role Target");
        String targetToken = issueToken(targetUser);

        mockMvc.perform(patch("/api/users/admin/{id}/role", targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RolePayload("TEACHER")))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(targetUser.getId()))
            .andExpect(jsonPath("$.data.role").value("TEACHER"));

        resetSecurityState();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(targetToken))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isForbidden());

        assertEquals("TEACHER", userRepository.findById(targetUser.getId()).orElseThrow().getRole());
    }

    @Test
    void adminCanDisableAndReEnableUser() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();
        User targetUser = registerUser("Status Target");
        String targetToken = issueToken(targetUser);

        mockMvc.perform(patch("/api/users/admin/{id}/status", targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StatusPayload(0)))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value(0));

        resetSecurityState();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(targetToken))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isForbidden());

        LoginRequest disabledLogin = new LoginRequest(targetUser.getStudentId(), TEST_PASSWORD);
        disabledLogin.setRememberMe(false);
        assertThrows(IllegalArgumentException.class, () -> userService.login(disabledLogin, nextIp(), USER_AGENT));

        resetSecurityState();

        mockMvc.perform(patch("/api/users/admin/{id}/status", targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StatusPayload(1)))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value(1));

        resetSecurityState();

        LoginRequest enabledLogin = new LoginRequest(targetUser.getStudentId(), TEST_PASSWORD);
        enabledLogin.setRememberMe(false);
        AuthResponse response = userService.login(enabledLogin, nextIp(), USER_AGENT);
        assertNotNull(response.getToken());
    }

    @Test
    void adminCannotModifyOwnRole() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();

        mockMvc.perform(patch("/api/users/admin/{id}/role", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RolePayload("STUDENT")))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("admin.user.error.self_role_change_forbidden"));
    }

    @Test
    void adminCannotDisableOwnAccount() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();

        mockMvc.perform(patch("/api/users/admin/{id}/status", admin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StatusPayload(0)))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("admin.user.error.self_status_change_forbidden"));
    }

    @Test
    void cannotDemoteLastActiveAdmin() throws Exception {
        User actorAdmin = userRepository.findByStudentId("admin001").orElseThrow();
        User targetAdmin = registerUser("Last Admin Role");
        targetAdmin.setRole("ADMIN");
        userRepository.save(targetAdmin);

        actorAdmin.setStatus(0);
        userRepository.save(actorAdmin);

        mockMvc.perform(patch("/api/users/admin/{id}/role", targetAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RolePayload("STUDENT")))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(actorAdmin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("admin.user.error.must_keep_active_admin"));
    }

    @Test
    void cannotDisableLastActiveAdmin() throws Exception {
        User actorAdmin = userRepository.findByStudentId("admin001").orElseThrow();
        User targetAdmin = registerUser("Last Admin Status");
        targetAdmin.setRole("ADMIN");
        userRepository.save(targetAdmin);

        actorAdmin.setStatus(0);
        userRepository.save(actorAdmin);

        mockMvc.perform(patch("/api/users/admin/{id}/status", targetAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StatusPayload(0)))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(actorAdmin)))
                .with(request -> {
                    request.setRemoteAddr(nextIp());
                    return request;
                }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("admin.user.error.must_keep_active_admin"));
    }

    @Test
    void concurrentCrossDemotionsStillLeaveOneActiveAdmin() throws InterruptedException {
        User builtInAdmin = userRepository.findByStudentId("admin001").orElseThrow();
        builtInAdmin.setStatus(0);
        userRepository.save(builtInAdmin);

        User adminA = registerUser("Concurrent Admin A");
        adminA.setRole("ADMIN");
        adminA.setStatus(1);
        userRepository.save(adminA);

        User adminB = registerUser("Concurrent Admin B");
        adminB.setRole("ADMIN");
        adminB.setStatus(1);
        userRepository.save(adminB);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                adminUserManagementService.updateUserRole(adminB.getId(), adminA.getId(), "STUDENT");
                successCount.incrementAndGet();
            } catch (IllegalArgumentException ex) {
                failureCount.incrementAndGet();
            } catch (Throwable throwable) {
                unexpectedErrors.add(throwable);
            } finally {
                done.countDown();
            }
        });

        executor.submit(() -> {
            ready.countDown();
            try {
                start.await();
                adminUserManagementService.updateUserRole(adminA.getId(), adminB.getId(), "STUDENT");
                successCount.incrementAndGet();
            } catch (IllegalArgumentException ex) {
                failureCount.incrementAndGet();
            } catch (Throwable throwable) {
                unexpectedErrors.add(throwable);
            } finally {
                done.countDown();
            }
        });

        assertTrue(ready.await(5, TimeUnit.SECONDS), "concurrent demotion workers did not become ready");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "concurrent demotion workers did not finish");
        executor.shutdownNow();

        assertTrue(unexpectedErrors.isEmpty(), "unexpected errors: " + unexpectedErrors);
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
        assertEquals(1, userRepository.countByRoleAndStatus("ADMIN", 1));
    }

    private User registerUser(String usernamePrefix) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        RegisterRequest request = new RegisterRequest();
        request.setStudentId(String.format("9%09d", sequence));
        request.setUsername(usernamePrefix + " " + sequence);
        request.setPassword(TEST_PASSWORD);
        request.setConfirmPassword(TEST_PASSWORD);
        request.setEmail("admin-user-" + sequence + "@example.com");
        userService.register(request);

        User user = userRepository.findByStudentId(request.getStudentId()).orElseThrow();
        createdUserIds.add(user.getId());
        return user;
    }

    private String issueToken(User user) {
        return jwtUtil.generateToken(user.getStudentId(), user.getRole(), user.getId());
    }

    private Authentication testAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
            user.getStudentId(),
            issueToken(user),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    private void resetSecurityState() {
        securityStateStore.clearAll();
        requestPatternAnalyzer.reset();
        userService.clearSecurityStateForTests();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String nextIp() {
        int sequence = IP_SEQUENCE.incrementAndGet();
        return "21.0.0." + sequence;
    }

    private record RolePayload(String role) {}

    private record StatusPayload(Integer status) {}
}
