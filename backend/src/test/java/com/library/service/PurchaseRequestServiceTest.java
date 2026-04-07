package com.library.service;

import com.library.dto.CreatePurchaseRequestPayload;
import com.library.dto.PurchaseRequestCreateResponseDTO;
import com.library.dto.PurchaseRequestVoteResponseDTO;
import com.library.dto.UpdatePurchaseRequestStatusPayload;
import com.library.model.Book;
import com.library.model.PurchaseRequestStatus;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.PurchaseRequestRepository;
import com.library.repository.PurchaseRequestVoteRepository;
import com.library.repository.UserRepository;
import com.library.testsupport.BorrowTestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BorrowTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PurchaseRequestServiceTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(3_000);

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PurchaseRequestVoteRepository purchaseRequestVoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @AfterEach
    void tearDown() {
        purchaseRequestVoteRepository.deleteAllInBatch();
        purchaseRequestRepository.deleteAllInBatch();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Create request saves the first support vote")
    void createRequestSavesInitialVote() {
        User proposer = createUser("采购发起人");

        PurchaseRequestCreateResponseDTO response = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("软件设计的哲学", "John Ousterhout", "9787302513927", "课程需要")
        );

        assertTrue(response.getCreated());
        assertEquals(1, purchaseRequestRepository.count());
        assertEquals(1, purchaseRequestVoteRepository.count());
        assertNotNull(response.getRequest());
        assertEquals(1, response.getRequest().getSupportCount());
        assertEquals("PENDING_REVIEW", response.getRequest().getStatus());
        assertTrue(response.getRequest().getVotedByCurrentUser());
    }

    @Test
    @DisplayName("Existing catalog book blocks purchase request creation")
    void existingBookBlocksCreate() {
        bookRepository.save(buildBook("重构", "Martin Fowler", "9787111213826"));
        User proposer = createUser("馆藏冲突用户");

        PurchaseRequestCreateResponseDTO response = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("重构", "Martin Fowler", "9787111213826", "副本不够")
        );

        assertFalse(response.getCreated());
        assertEquals("EXISTING_BOOK", response.getConflictType());
        assertNotNull(response.getExistingBookId());
        assertEquals(0, purchaseRequestRepository.count());
    }

    @Test
    @DisplayName("Duplicate requests return the existing pool item without adding support")
    void duplicateRequestReturnsExistingEntry() {
        User proposer = createUser("初始提议人");
        User duplicateSubmitter = createUser("重复提交人");

        PurchaseRequestCreateResponseDTO first = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("领域驱动设计", "Eric Evans", null, "课程项目参考")
        );
        PurchaseRequestCreateResponseDTO second = purchaseRequestService.createRequest(
            duplicateSubmitter.getId(),
            payload("领域驱动设计", "Eric Evans", null, "我也想看")
        );

        assertTrue(first.getCreated());
        assertFalse(second.getCreated());
        assertEquals("DUPLICATE_REQUEST", second.getConflictType());
        assertEquals(first.getRequest().getId(), second.getExistingRequestId());
        assertEquals(1, purchaseRequestRepository.count());
        assertEquals(1, purchaseRequestVoteRepository.count());
        assertEquals(1, purchaseRequestService.getBoard(duplicateSubmitter.getId()).getTotalSupportCount());
    }

    @Test
    @DisplayName("Votes are idempotent and the request enters priority pool at three supports")
    void votesAreIdempotentAndAutoPriorityWorks() {
        User proposer = createUser("投票发起人");
        User voterA = createUser("投票用户A");
        User voterB = createUser("投票用户B");

        PurchaseRequestCreateResponseDTO created = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("代码整洁之道", "Robert C. Martin", "9787115216878", "基础阅读")
        );

        PurchaseRequestVoteResponseDTO firstVote = purchaseRequestService.vote(voterA.getId(), created.getRequest().getId());
        PurchaseRequestVoteResponseDTO secondVote = purchaseRequestService.vote(voterA.getId(), created.getRequest().getId());
        PurchaseRequestVoteResponseDTO thirdVote = purchaseRequestService.vote(voterB.getId(), created.getRequest().getId());

        assertFalse(firstVote.getAlreadyVoted());
        assertEquals(2, firstVote.getRequest().getSupportCount());
        assertTrue(secondVote.getAlreadyVoted());
        assertEquals(2, secondVote.getRequest().getSupportCount());
        assertFalse(thirdVote.getAlreadyVoted());
        assertEquals(3, thirdVote.getRequest().getSupportCount());
        assertEquals("PRIORITY_POOL", thirdVote.getRequest().getStatus());
    }

    @Test
    @DisplayName("Request enters planned status at eight supports")
    void requestBecomesPlannedAtEightSupports() {
        User proposer = createUser("计划状态发起人");
        PurchaseRequestCreateResponseDTO created = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("计算机程序的构造和解释", "Harold Abelson", "9787111135104", "课程核心教材")
        );

        List<User> voters = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            voters.add(createUser("计划投票人" + i));
        }

        PurchaseRequestVoteResponseDTO latest = null;
        for (User voter : voters) {
            latest = purchaseRequestService.vote(voter.getId(), created.getRequest().getId());
        }

        assertNotNull(latest);
        assertEquals(8, latest.getRequest().getSupportCount());
        assertEquals("PLANNED", latest.getRequest().getStatus());
    }

    @Test
    @DisplayName("Manual planned status remains stable when later votes arrive")
    void manualPlannedStatusIsNotOverwritten() {
        User proposer = createUser("手动状态发起人");
        User voter = createUser("后续投票用户");

        PurchaseRequestCreateResponseDTO created = purchaseRequestService.createRequest(
            proposer.getId(),
            payload("深入理解计算机系统", "Randal Bryant", "9787111544937", "系统课补充阅读")
        );

        UpdatePurchaseRequestStatusPayload statusPayload = new UpdatePurchaseRequestStatusPayload();
        statusPayload.setStatus(PurchaseRequestStatus.PLANNED);
        statusPayload.setStatusNote("馆员已纳入本月采购计划");
        purchaseRequestService.updateStatus(created.getRequest().getId(), statusPayload, proposer.getId());

        PurchaseRequestVoteResponseDTO voteResponse = purchaseRequestService.vote(voter.getId(), created.getRequest().getId());

        assertEquals("PLANNED", voteResponse.getRequest().getStatus());
        assertEquals(2, voteResponse.getRequest().getSupportCount());
        assertEquals("馆员已纳入本月采购计划", voteResponse.getRequest().getStatusNote());
    }

    private CreatePurchaseRequestPayload payload(String title, String author, String isbn, String reason) {
        CreatePurchaseRequestPayload payload = new CreatePurchaseRequestPayload();
        payload.setTitle(title);
        payload.setAuthor(author);
        payload.setIsbn(isbn);
        payload.setReason(reason);
        return payload;
    }

    private User createUser(String username) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        User user = new User();
        user.setStudentId("9" + sequence);
        user.setUsername(username);
        user.setPassword("EncodedPassword123!");
        user.setEmail("purchase-" + sequence + "@example.com");
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setLoginCount(0);
        return userRepository.save(user);
    }

    private Book buildBook(String title, String author, String isbn) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setCategory("计算机");
        book.setStatus("IN_LIBRARY");
        book.setTotalCopies(2);
        book.setAvailableCopies(1);
        book.setBorrowedCount(1);
        return book;
    }
}
