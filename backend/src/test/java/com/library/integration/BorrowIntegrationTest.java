package com.library.integration;

import com.library.dto.BorrowRequest;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.ReservationRequest;
import com.library.model.Book;
import com.library.model.ReservationRecord;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import com.library.service.BorrowService;
import com.library.service.ReservationService;
import com.library.service.UserService;
import com.library.testsupport.BorrowTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BorrowTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class BorrowIntegrationTest {

    private static final String TEST_PASSWORD = "Test123!A";
    private static final AtomicInteger STUDENT_SEQUENCE = new AtomicInteger(0);

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private ReservationRecordRepository reservationRecordRepository;

    private Long userId;
    private Long bookId;

    @BeforeEach
    void setUp() {
        String studentId = nextStudentId();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setStudentId(studentId);
        registerRequest.setUsername("Integration User");
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setConfirmPassword(TEST_PASSWORD);
        registerRequest.setEmail(studentId + "@test.com");

        var registerResult = userService.register(registerRequest);
        assertTrue((Boolean) registerResult.get("success"));

        LoginRequest loginRequest = new LoginRequest(studentId, TEST_PASSWORD);
        var authResponse = userService.login(loginRequest, "127.0.0.1", "test-agent");
        userId = authResponse.getUser().getId();

        Book book = createBook("Integration Test Book", "Test Author", "978-9999999999");
        bookId = book.getId();
    }

    @Test
    @DisplayName("Complete borrow flow: apply, approve, pickup, return")
    void testCompleteBorrowFlow() {
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setBookId(bookId);
        borrowRequest.setNotes("Integration test borrow");

        var applyResponse = borrowService.applyBorrow(userId, borrowRequest);
        assertNotNull(applyResponse);
        assertEquals("APPROVED", applyResponse.getStatus());
        assertNull(applyResponse.getBorrowDate());
        assertNull(applyResponse.getDueDate());
        assertNotNull(applyResponse.getApprovedAt());
        assertNotNull(applyResponse.getPickupDeadline());
        assertEquals(applyResponse.getApprovedAt().plusDays(3), applyResponse.getPickupDeadline());

        Long recordId = applyResponse.getId();

        Book book = bookRepository.findById(bookId).orElseThrow();
        assertEquals(4, book.getAvailableCopies());

        var pickupResponse = borrowService.confirmPickup(recordId, userId);
        assertEquals("BORROWED", pickupResponse.getStatus());
        assertNotNull(pickupResponse.getBorrowDate());
        assertNotNull(pickupResponse.getDueDate());

        var returnResponse = borrowService.returnBook(recordId, userId);
        assertEquals("RETURNED", returnResponse.getStatus());

        book = bookRepository.findById(bookId).orElseThrow();
        assertEquals(5, book.getAvailableCopies());
    }

    @Test
    @DisplayName("Borrow limit blocks the sixth active request")
    void testConcurrentBorrowLimit() {
        List<Long> createdBookIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Book book = createBook("Borrow Limit Book " + i, "Author " + i, "978-88888888" + i);
            createdBookIds.add(book.getId());
        }

        for (Long createdBookId : createdBookIds) {
            BorrowRequest request = new BorrowRequest();
            request.setBookId(createdBookId);
            borrowService.applyBorrow(userId, request);
        }

        BorrowRequest request = new BorrowRequest();
        request.setBookId(bookId);

        assertThrows(IllegalArgumentException.class, () ->
            borrowService.applyBorrow(userId, request)
        );

        assertEquals(
            5L,
            borrowRecordRepository.countCurrentBorrowsByUserId(
                userId,
                BorrowRecord.ACTIVE_STATUSES
            )
        );
    }

    @Test
    @DisplayName("Duplicate borrow requests for the same book are rejected")
    void testDuplicateBorrowPrevention() {
        BorrowRequest request1 = new BorrowRequest();
        request1.setBookId(bookId);
        borrowService.applyBorrow(userId, request1);

        BorrowRequest request2 = new BorrowRequest();
        request2.setBookId(bookId);

        assertThrows(IllegalArgumentException.class, () ->
            borrowService.applyBorrow(userId, request2)
        );

        assertEquals(
            1L,
            borrowRecordRepository.countCurrentBorrowsByUserId(
                userId,
                BorrowRecord.ACTIVE_STATUSES
            )
        );
    }

    @Test
    @DisplayName("Auto-approved borrowing blocks the second request when inventory is exhausted")
    void testInsufficientInventoryHandling() {
        Book limitedBook = createBook("Limited Book", "Limited Author", "978-1111111111");
        limitedBook.setTotalCopies(1);
        limitedBook.setAvailableCopies(1);
        limitedBook = bookRepository.save(limitedBook);

        BorrowRequest request1 = new BorrowRequest();
        request1.setBookId(limitedBook.getId());
        var response1 = borrowService.applyBorrow(userId, request1);

        Long secondUserId = registerAndLoginUser(nextStudentId(), "Second User");

        BorrowRequest request2 = new BorrowRequest();
        request2.setBookId(limitedBook.getId());
        assertThrows(IllegalArgumentException.class, () -> borrowService.applyBorrow(secondUserId, request2));
        assertEquals("APPROVED", response1.getStatus());
    }

    @Test
    @DisplayName("Reservation pickup creates a borrowed record and completes the reservation")
    void testReservationPickupFlow() {
        Book reservedBook = createBook("Reserved Book", "Queue Author", "978-2222222222");
        reservedBook.setTotalCopies(1);
        reservedBook.setAvailableCopies(0);
        reservedBook = bookRepository.save(reservedBook);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setBookId(reservedBook.getId());
        var reservationResponse = reservationService.reserveBook(userId, reservationRequest);
        assertEquals("WAITING", reservationResponse.getStatus());

        reservedBook.setAvailableCopies(1);
        bookRepository.save(reservedBook);
        reservationService.notifyNextReservation(reservedBook.getId());

        ReservationRecord reservation = reservationRecordRepository.findById(reservationResponse.getId()).orElseThrow();
        assertEquals(ReservationRecord.ReservationStatus.AVAILABLE, reservation.getStatus());

        var borrowResponse = reservationService.pickupReservation(reservation.getId(), userId);
        assertEquals("BORROWED", borrowResponse.getStatus());
        assertNotNull(borrowResponse.getBorrowDate());
        assertNotNull(borrowResponse.getDueDate());

        ReservationRecord completedReservation = reservationRecordRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationRecord.ReservationStatus.COMPLETED, completedReservation.getStatus());

        Book reloadedBook = bookRepository.findById(reservedBook.getId()).orElseThrow();
        assertEquals(0, reloadedBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Reservation promotion fills every newly available slot")
    void testReservationPromotionUsesAllAvailableCopies() {
        Book reservedBook = createBook("Queued Book", "Queue Author", "978-3333333333");
        reservedBook.setTotalCopies(2);
        reservedBook.setAvailableCopies(0);
        reservedBook = bookRepository.save(reservedBook);

        ReservationRequest request = new ReservationRequest();
        request.setBookId(reservedBook.getId());

        Long firstReservationId = reservationService.reserveBook(userId, request).getId();

        Long secondUserId = registerAndLoginUser(nextStudentId(), "Second Queue User");
        Long secondReservationId = reservationService.reserveBook(secondUserId, request).getId();

        Long thirdUserId = registerAndLoginUser(nextStudentId(), "Third Queue User");
        Long thirdReservationId = reservationService.reserveBook(thirdUserId, request).getId();

        reservedBook.setAvailableCopies(2);
        bookRepository.save(reservedBook);
        reservationService.notifyNextReservation(reservedBook.getId());

        ReservationRecord firstReservation = reservationRecordRepository.findById(firstReservationId).orElseThrow();
        ReservationRecord secondReservation = reservationRecordRepository.findById(secondReservationId).orElseThrow();
        ReservationRecord thirdReservation = reservationRecordRepository.findById(thirdReservationId).orElseThrow();

        assertEquals(ReservationRecord.ReservationStatus.AVAILABLE, firstReservation.getStatus());
        assertEquals(ReservationRecord.ReservationStatus.AVAILABLE, secondReservation.getStatus());
        assertEquals(ReservationRecord.ReservationStatus.WAITING, thirdReservation.getStatus());
        assertEquals(1, thirdReservation.getQueuePosition());
    }

    private Long registerAndLoginUser(String studentId, String username) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setStudentId(studentId);
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setConfirmPassword(TEST_PASSWORD);
        registerRequest.setEmail(studentId + "@test.com");
        userService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest(studentId, TEST_PASSWORD);
        return userService.login(loginRequest, "127.0.0.1", "test-agent").getUser().getId();
    }

    private Book createBook(String title, String author, String isbn) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setCategory("Technology");
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        book.setLocation("A-01-001");
        return bookRepository.save(book);
    }

    private String nextStudentId() {
        return String.format("7%09d", STUDENT_SEQUENCE.incrementAndGet());
    }
}
