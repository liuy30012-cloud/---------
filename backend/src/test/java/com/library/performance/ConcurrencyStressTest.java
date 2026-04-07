package com.library.performance;

import com.library.dto.BorrowRequest;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.model.Book;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.service.BorrowService;
import com.library.service.UserService;
import com.library.testsupport.BorrowTestApplication;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@Tag("concurrency-db")
@SpringBootTest(classes = BorrowTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ConcurrencyStressTest {

    private static final String TEST_PASSWORD = "Test123!A";
    private static final int USER_COUNT = 20;
    private static final AtomicInteger RUN_SEQUENCE = new AtomicInteger(0);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("library_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    private final List<Long> userIds = new ArrayList<>();
    private Long bookId;
    private int runNumber;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for concurrency-db tests");
        userIds.clear();
        runNumber = RUN_SEQUENCE.incrementAndGet();

        Book book = new Book();
        book.setTitle("Stress Test Book " + runNumber);
        book.setAuthor("Test Author");
        book.setIsbn(String.format("978-%010d", runNumber));
        book.setCategory("Technology");
        book.setTotalCopies(100);
        book.setAvailableCopies(100);
        book.setLocation("A-01-001");
        book = bookRepository.save(book);
        bookId = book.getId();

        for (int i = 0; i < USER_COUNT; i++) {
            String studentId = nextStudentId(i);
            RegisterRequest request = new RegisterRequest();
            request.setStudentId(studentId);
            request.setUsername("Stress User " + i);
            request.setPassword(TEST_PASSWORD);
            request.setConfirmPassword(TEST_PASSWORD);
            request.setEmail(studentId + "@test.com");

            userService.register(request);
            LoginRequest loginRequest = new LoginRequest(studentId, TEST_PASSWORD);
            userIds.add(userService.login(loginRequest, "127.0.0.1", "test-agent").getUser().getId());
        }

        assertFalse(userIds.isEmpty(), "stress test users should be created");
    }

    @Test
    @DisplayName("Concurrent borrow requests finish without unexpected exceptions")
    void testConcurrentBorrowRequests() throws Exception {
        int threadCount = 40;
        List<BorrowAttemptResult> results = runConcurrently(threadCount, 20, index -> {
            Long userId = userIds.get(index % userIds.size());
            BorrowRequest request = new BorrowRequest();
            request.setBookId(bookId);
            request.setNotes("Concurrent borrow " + index);

            try {
                borrowService.applyBorrow(userId, request);
                return BorrowAttemptResult.success();
            } catch (IllegalArgumentException e) {
                return BorrowAttemptResult.businessFailure();
            }
        });

        long successCount = results.stream().filter(BorrowAttemptResult::isSuccess).count();
        long failureCount = results.stream().filter(result -> !result.isSuccess()).count();

        assertEquals(USER_COUNT, successCount, "each user should only create one active borrow request");
        assertEquals(threadCount - USER_COUNT, failureCount, "duplicate requests should fail deterministically");
        assertEquals(
            USER_COUNT,
            borrowRecordRepository.findByBookIdAndStatusIn(
                bookId,
                List.of(BorrowStatus.PENDING, BorrowStatus.APPROVED, BorrowStatus.BORROWED, BorrowStatus.OVERDUE)
            ).size()
        );
    }

    @Test
    @DisplayName("Concurrent login failures are counted consistently")
    void testConcurrentLoginFailures() throws Exception {
        int threadCount = 30;
        List<Boolean> results = runConcurrently(threadCount, 15, index -> {
            try {
                LoginRequest request = new LoginRequest("9999999999", "wrongpass");
                userService.login(request, "127.0.0.1", "test-agent");
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        assertEquals(threadCount, results.stream().filter(Boolean::booleanValue).count());
    }

    @Test
    @DisplayName("Concurrent approvals keep inventory and record state consistent")
    void testConcurrentInventoryDeduction() throws Exception {
        int initialInventory = 10;

        Book limitedBook = new Book();
        limitedBook.setTitle("Limited Book " + runNumber);
        limitedBook.setAuthor("Inventory Author");
        limitedBook.setIsbn(String.format("979-%010d", runNumber));
        limitedBook.setCategory("Technology");
        limitedBook.setTotalCopies(initialInventory);
        limitedBook.setAvailableCopies(initialInventory);
        limitedBook.setLocation("A-01-999");
        limitedBook = bookRepository.save(limitedBook);
        Long limitedBookId = limitedBook.getId();

        List<Long> recordIds = runConcurrently(USER_COUNT, USER_COUNT, index -> {
            Long userId = userIds.get(index);
            BorrowRequest request = new BorrowRequest();
            request.setBookId(limitedBookId);
            return borrowService.applyBorrow(userId, request).getId();
        });

        assertEquals(USER_COUNT, recordIds.size(), "all distinct users should be able to submit a pending request");

        List<String> approvalStatuses = runConcurrently(USER_COUNT, USER_COUNT, index ->
            borrowService.approveBorrow(recordIds.get(index), "admin", true, null).getStatus()
        );

        long approvedCount = approvalStatuses.stream().filter("APPROVED"::equals).count();
        long rejectedCount = approvalStatuses.stream().filter("REJECTED"::equals).count();

        assertEquals(initialInventory, approvedCount, "approved borrow count should match available inventory");
        assertEquals(USER_COUNT - initialInventory, rejectedCount, "remaining approvals should be rejected");

        Book finalBook = bookRepository.findById(limitedBookId).orElseThrow();
        assertEquals(0, finalBook.getAvailableCopies(), "inventory should be exhausted after approved borrows");
        assertEquals(initialInventory, finalBook.getAvailableCopies() + (int) approvedCount);
        assertEquals(
            approvedCount,
            borrowRecordRepository.findByBookIdAndStatusIn(limitedBookId, List.of(BorrowStatus.APPROVED, BorrowStatus.BORROWED)).size()
        );
        assertEquals(
            rejectedCount,
            borrowRecordRepository.findByBookIdAndStatusIn(limitedBookId, List.of(BorrowStatus.REJECTED)).size()
        );
    }

    private String nextStudentId(int index) {
        return String.format("%02d%08d", runNumber % 100, index);
    }

    private <T> List<T> runConcurrently(int threadCount, int poolSize, IndexedTask<T> task)
        throws InterruptedException, ExecutionException {
        int effectivePoolSize = Math.min(poolSize, threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(effectivePoolSize);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startSignal.await();
                return task.execute(index);
            }));
        }

        try {
            startSignal.countDown();

            List<T> results = new ArrayList<>(threadCount);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    private static final class BorrowAttemptResult {
        private final boolean success;

        private BorrowAttemptResult(boolean success) {
            this.success = success;
        }

        static BorrowAttemptResult success() {
            return new BorrowAttemptResult(true);
        }

        static BorrowAttemptResult businessFailure() {
            return new BorrowAttemptResult(false);
        }

        boolean isSuccess() {
            return success;
        }
    }

    @FunctionalInterface
    private interface IndexedTask<T> {
        T execute(int index) throws Exception;
    }
}
