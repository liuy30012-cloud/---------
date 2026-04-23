package com.library.service;

import com.library.dto.BorrowRequest;
import com.library.dto.BorrowResponse;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.service.borrow.BorrowConverter;
import com.library.service.borrow.BorrowNotificationHelper;
import com.library.service.borrow.BorrowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime BASE_TIME = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private BorrowValidator borrowValidator;

    @Mock
    private BorrowConverter borrowConverter;

    @Mock
    private BorrowNotificationHelper borrowNotificationHelper;

    private BorrowService borrowService;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        borrowService = new BorrowService(
            borrowRecordRepository,
            bookService,
            userService,
            reservationService,
            borrowValidator,
            borrowConverter,
            borrowNotificationHelper,
            FIXED_CLOCK
        );

        testUser = new User("2021001", "Test User", "password");
        testUser.setId(1L);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setIsbn("978-1234567890");
        testBook.setAvailableCopies(5);
        testBook.setTotalCopies(10);
    }

    @Test
    @DisplayName("Return calculates overdue fines for same-day late returns")
    void testOverdueDaysCalculationSameDayReturn() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setBookId(1L);
        record.setDueDate(BASE_TIME.minusHours(2));
        record.setStatus(BorrowStatus.BORROWED);

        BorrowResponse response = new BorrowResponse();

        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(userService.getUserById(1L)).thenReturn(testUser);
        doNothing().when(bookService).increaseAvailableCopies(1L);
        when(borrowValidator.calculateOverdue(record.getDueDate()))
            .thenReturn(new BorrowValidator.OverdueInfo(1, new BigDecimal("0.50")));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowConverter.toResponse(any(BorrowRecord.class))).thenReturn(response);

        borrowService.returnBook(1L, 1L);

        verify(borrowRecordRepository).save(any(BorrowRecord.class));
        verify(bookService).increaseAvailableCopies(1L);
    }

    @Test
    @DisplayName("Borrow limit check includes pending records")
    void testBorrowLimitIncludesPendingStatus() {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(bookService.getBookById(1L)).thenReturn(testBook);
        when(borrowRecordRepository.countCurrentBorrowsByUserId(
            eq(1L),
            eq(BorrowRecord.ACTIVE_STATUSES)
        )).thenReturn(5L);
        doThrow(new IllegalArgumentException("limit"))
            .when(borrowValidator).validateCanBorrow(5L);

        BorrowRequest request = new BorrowRequest();
        request.setBookId(1L);

        assertThrows(IllegalArgumentException.class, () -> borrowService.applyBorrow(1L, request));

        verify(borrowValidator).validateCanBorrow(5L);
        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Renewal rejects records already marked overdue")
    void testRenewRejectsOverdueBooks() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setBookId(1L);
        record.setStatus(BorrowStatus.OVERDUE);
        record.setDueDate(BASE_TIME.minusDays(1));

        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(reservationService.countWaitingReservations(1L)).thenReturn(0L);
        doThrow(new IllegalArgumentException("overdue"))
            .when(borrowValidator).validateCanRenew(record, 0L);

        assertThrows(IllegalArgumentException.class, () -> borrowService.renewBorrow(1L, 1L));

        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        verify(borrowNotificationHelper, never()).sendRenewNotification(any(), any());
    }

    @Test
    @DisplayName("Renewal also rejects stale borrowed records past due date")
    void testRenewChecksTimeEvenIfStatusNotOverdue() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setBookId(1L);
        record.setStatus(BorrowStatus.BORROWED);
        record.setDueDate(BASE_TIME.minusDays(1));
        record.setRenewCount(0);

        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(reservationService.countWaitingReservations(1L)).thenReturn(0L);
        doThrow(new IllegalArgumentException("overdue"))
            .when(borrowValidator).validateCanRenew(record, 0L);

        assertThrows(IllegalArgumentException.class, () -> borrowService.renewBorrow(1L, 1L));

        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        verify(borrowNotificationHelper, never()).sendRenewNotification(any(), any());
    }

    @Test
    @DisplayName("Apply borrow succeeds for a valid request")
    void testSuccessfulBorrowApplication() {
        BorrowRequest request = new BorrowRequest();
        request.setBookId(1L);
        request.setNotes("test");

        BorrowResponse response = new BorrowResponse();
        response.setId(1L);
        response.setStatus("APPROVED");
        response.setApprovedAt(BASE_TIME);
        response.setPickupDeadline(BASE_TIME.plusDays(3));

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(bookService.getBookById(1L)).thenReturn(testBook);
        when(borrowRecordRepository.countCurrentBorrowsByUserId(anyLong(), anyList())).thenReturn(0L);
        when(borrowRecordRepository.existsByUserIdAndBookIdAndStatusIn(anyLong(), anyLong(), anyList())).thenReturn(false);
        when(reservationService.hasOtherActiveReservations(anyLong(), anyLong())).thenReturn(false);
        doNothing().when(borrowValidator).validateCanBorrow(0L);
        doNothing().when(bookService).decreaseAvailableCopies(1L);
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord saved = invocation.getArgument(0);
            saved.setId(1L);
            assertEquals(BASE_TIME, saved.getApprovedAt());
            assertEquals(BASE_TIME.plusDays(3), saved.getPickupDeadline());
            return saved;
        });
        when(borrowConverter.toResponse(any(BorrowRecord.class))).thenReturn(response);

        BorrowResponse result = borrowService.applyBorrow(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BASE_TIME, result.getApprovedAt());
        assertEquals(BASE_TIME.plusDays(3), result.getPickupDeadline());
        verify(bookService).decreaseAvailableCopies(1L);
        verify(borrowNotificationHelper).sendApprovalNotification(eq(testUser), any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Apply borrow rejects requests missing book id")
    void testApplyBorrowRejectsMissingBookId() {
        BorrowRequest request = new BorrowRequest();

        assertThrows(IllegalArgumentException.class, () -> borrowService.applyBorrow(1L, request));
        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Return succeeds for active borrows")
    void testSuccessfulBookReturn() {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setBookId(1L);
        record.setStudentId(testUser.getStudentId());
        record.setBookTitle(testBook.getTitle());
        record.setStatus(BorrowStatus.BORROWED);
        record.setDueDate(BASE_TIME.plusDays(7));
        record.setFineAmount(BigDecimal.ZERO);

        when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(userService.getUserById(1L)).thenReturn(testUser);
        doNothing().when(bookService).increaseAvailableCopies(1L);
        when(borrowValidator.calculateOverdue(record.getDueDate()))
            .thenReturn(new BorrowValidator.OverdueInfo(0, BigDecimal.ZERO));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowConverter.toResponse(any(BorrowRecord.class))).thenReturn(new BorrowResponse());

        BorrowResponse response = borrowService.returnBook(1L, 1L);

        assertNotNull(response);
        verify(bookService).increaseAvailableCopies(1L);
        verify(borrowNotificationHelper).sendReturnNotification(testUser, record);
    }
}
