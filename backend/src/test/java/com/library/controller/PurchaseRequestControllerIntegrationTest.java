package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.LibraryApplication;
import com.library.model.Book;
import com.library.model.PurchaseRequest;
import com.library.model.PurchaseRequestStatus;
import com.library.model.PurchaseRequestVote;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.PurchaseRequestRepository;
import com.library.repository.PurchaseRequestVoteRepository;
import com.library.repository.UserRepository;
import com.library.security.SecurityStateStore;
import com.library.service.RequestPatternAnalyzer;
import com.library.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseRequestControllerIntegrationTest {

    private static final String USER_AGENT = "Mozilla/5.0 Test Browser";
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(4_000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PurchaseRequestVoteRepository purchaseRequestVoteRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SecurityStateStore securityStateStore;

    @Autowired
    private RequestPatternAnalyzer requestPatternAnalyzer;

    @AfterEach
    void tearDown() {
        purchaseRequestVoteRepository.deleteAllInBatch();
        purchaseRequestRepository.deleteAllInBatch();
        bookRepository.deleteAll();
        securityStateStore.clearAll();
        requestPatternAnalyzer.reset();
    }

    @Test
    void authenticatedUserCanViewBoard() throws Exception {
        User student = userRepository.findByStudentId("2021001").orElseThrow();

        mockMvc.perform(get("/api/purchase-requests")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(student)))
                .with(request -> {
                    request.setRemoteAddr("11.0.0.1");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalRequests").exists());
    }

    @Test
    void authenticatedUserCanCreateRequest() throws Exception {
        User student = userRepository.findByStudentId("2021001").orElseThrow();

        mockMvc.perform(post("/api/purchase-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "UNIX 编程艺术",
                    "author", "Eric S. Raymond",
                    "isbn", "9787121025233",
                    "reason", "系统设计课程参考"
                )))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(student)))
                .with(request -> {
                    request.setRemoteAddr("11.0.0.2");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.created").value(true))
            .andExpect(jsonPath("$.data.request.supportCount").value(1));
    }

    @Test
    void authenticatedUserCanVote() throws Exception {
        User proposer = userRepository.findByStudentId("2021001").orElseThrow();
        User voter = createStudent("接口投票用户");
        PurchaseRequest request = saveRequest(proposer.getId(), "现代操作系统", "Andrew S. Tanenbaum", "9787111128069");

        mockMvc.perform(post("/api/purchase-requests/{id}/vote", request.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(voter)))
                .with(httpRequest -> {
                    httpRequest.setRemoteAddr("11.0.0.3");
                    return httpRequest;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.alreadyVoted").value(false))
            .andExpect(jsonPath("$.data.request.supportCount").value(2));
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/purchase-requests")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(request -> {
                    request.setRemoteAddr("11.0.0.4");
                    return request;
                }))
            .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotUpdateStatus() throws Exception {
        User proposer = userRepository.findByStudentId("2021001").orElseThrow();
        PurchaseRequest request = saveRequest(proposer.getId(), "模式语言", "Christopher Alexander", "9787111126225");
        User student = userRepository.findByStudentId("2021001").orElseThrow();

        mockMvc.perform(patch("/api/purchase-requests/{id}/status", request.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "status", "PURCHASING",
                    "statusNote", "学生无权操作"
                )))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(student)))
                .with(httpRequest -> {
                    httpRequest.setRemoteAddr("11.0.0.5");
                    return httpRequest;
                }))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateStatus() throws Exception {
        User proposer = userRepository.findByStudentId("2021001").orElseThrow();
        PurchaseRequest request = saveRequest(proposer.getId(), "设计模式", "GoF", "9787111075752");
        User admin = userRepository.findByStudentId("admin001").orElseThrow();

        mockMvc.perform(patch("/api/purchase-requests/{id}/status", request.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "status", "PURCHASING",
                    "statusNote", "已提交采购单"
                )))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(httpRequest -> {
                    httpRequest.setRemoteAddr("11.0.0.6");
                    return httpRequest;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PURCHASING"))
            .andExpect(jsonPath("$.data.statusNote").value("已提交采购单"));
    }

    @Test
    void adminCanStillAccessAlgorithmSuggestions() throws Exception {
        User admin = userRepository.findByStudentId("admin001").orElseThrow();

        mockMvc.perform(get("/api/statistics/purchase-suggestions")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .with(authentication(testAuthentication(admin)))
                .with(request -> {
                    request.setRemoteAddr("11.0.0.7");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    private Authentication testAuthentication(User user) {
        String token = jwtUtil.generateToken(user.getStudentId(), user.getRole(), user.getId());
        return new UsernamePasswordAuthenticationToken(
            user.getStudentId(),
            token,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    private User createStudent(String username) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        User user = new User();
        user.setStudentId("7" + sequence);
        user.setUsername(username);
        user.setPassword("EncodedPassword123!");
        user.setEmail("controller-" + sequence + "@example.com");
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setLoginCount(0);
        return userRepository.save(user);
    }

    private PurchaseRequest saveRequest(Long proposerUserId, String title, String author, String isbn) {
        PurchaseRequest request = new PurchaseRequest();
        request.setTitle(title);
        request.setAuthor(author);
        request.setIsbn(isbn);
        request.setReason("测试请求");
        request.setProposerUserId(proposerUserId);
        request.setSupportCount(1);
        request.setStatus(PurchaseRequestStatus.PENDING_REVIEW);
        request.setDedupeKey("ISBN:" + isbn.replace("-", ""));
        request.setStatusManagedManually(false);
        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);

        PurchaseRequestVote vote = new PurchaseRequestVote();
        vote.setPurchaseRequestId(savedRequest.getId());
        vote.setUserId(proposerUserId);
        purchaseRequestVoteRepository.save(vote);

        return savedRequest;
    }
}
