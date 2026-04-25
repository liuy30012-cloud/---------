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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.library.service.borrow.BorrowValidator.DEFAULT_BORROW_DAYS;
import static com.library.service.borrow.BorrowValidator.MAX_BORROW_COUNT;
import static com.library.service.borrow.BorrowValidator.RENEW_DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final int PICKUP_WINDOW_DAYS = 3;

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookService bookService;
    private final UserService userService;
    private final ReservationService reservationService;
    private final BorrowValidator validator;
    private final BorrowConverter converter;
    private final BorrowNotificationHelper notificationHelper;
    private final Clock clock;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BorrowResponse applyBorrow(Long userId, BorrowRequest request) {
        if (request == null || request.getBookId() == null) {
            throw new IllegalArgumentException("借阅申请必须包含图书 ID。");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }

        Book book = bookService.getBookById(request.getBookId());
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY) {
            throw new IllegalArgumentException("该图书仅供馆内阅览，不能借阅。");
        }

        long currentBorrowCount = borrowRecordRepository.countCurrentBorrowsByUserId(userId, BorrowRecord.ACTIVE_STATUSES);
        validator.validateCanBorrow(currentBorrowCount);

        boolean alreadyBorrowed = borrowRecordRepository.existsByUserIdAndBookIdAndStatusIn(
            userId,
            request.getBookId(),
            BorrowRecord.ACTIVE_STATUSES
        );
        if (alreadyBorrowed) {
            throw new IllegalArgumentException("你已存在该书的借阅、待取或待审核记录。");
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException("当前暂无可借副本。");
        }

        if (reservationService.hasOtherActiveReservations(book.getId(), userId)) {
            throw new IllegalArgumentException("当前已有其他读者排在该书预约队列前面。");
        }

        BorrowRecord record = buildBorrowRecord(userId, user, book, request);
        boolean manualApproval = requiresManualApproval(book, userId);

        if (manualApproval) {
            record.setStatus(BorrowStatus.PENDING);
            BorrowRecord saved = borrowRecordRepository.save(record);
            notificationHelper.sendApplicationNotification(user, book);
            log.info("Borrow request {} submitted for manual approval", saved.getId());
            return converter.toResponse(saved);
        }

        bookService.decreaseAvailableCopies(book.getId());
        record.setStatus(BorrowStatus.APPROVED);
        record.setApprovedBy("SYSTEM");
        LocalDateTime approvalTime = now();
        record.setApprovedAt(approvalTime);
        record.setPickupDeadline(approvalTime.plusDays(PICKUP_WINDOW_DAYS));

        BorrowRecord saved = borrowRecordRepository.save(record);
        notificationHelper.sendApprovalNotification(user, saved);
        log.info("Borrow request {} auto-approved", saved.getId());
        return converter.toResponse(saved);
    }

    @Transactional
    public BorrowResponse approveBorrow(Long recordId, String approver, boolean approved, String rejectReason) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("借阅记录不存在。"));

        if (record.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalArgumentException("只有待审核的借阅申请才能处理。");
        }

        User user = userService.getUserById(record.getUserId());

        if (approved) {
            long currentBorrowCount = borrowRecordRepository.countCurrentBorrowsByUserId(
                record.getUserId(),
                BorrowRecord.CHECKED_OUT_STATUSES
            );
            if (currentBorrowCount >= MAX_BORROW_COUNT) {
                record.setStatus(BorrowStatus.REJECTED);
                record.setRejectReason("已达到借阅上限，请先归还其他图书。");
                BorrowRecord saved = borrowRecordRepository.save(record);
                notificationHelper.sendApprovalNotification(user, saved);
                return converter.toResponse(saved);
            }

            if (reservationService.hasOtherActiveReservations(record.getBookId(), record.getUserId())) {
                record.setStatus(BorrowStatus.REJECTED);
                record.setRejectReason("当前已有其他读者排在预约队列前面。");
                BorrowRecord saved = borrowRecordRepository.save(record);
                notificationHelper.sendApprovalNotification(user, saved);
                return converter.toResponse(saved);
            }

            bookService.decreaseAvailableCopies(record.getBookId());
            record.setStatus(BorrowStatus.APPROVED);
            record.setApprovedBy(approver);
            LocalDateTime approvalTime = now();
            record.setApprovedAt(approvalTime);
            record.setPickupDeadline(approvalTime.plusDays(PICKUP_WINDOW_DAYS));
        } else {
            record.setStatus(BorrowStatus.REJECTED);
            record.setRejectReason((rejectReason == null || rejectReason.isBlank())
                ? "管理员审核未通过。"
                : rejectReason.trim());
            record.setPickupDeadline(null);
        }

        BorrowRecord saved = borrowRecordRepository.save(record);
        notificationHelper.sendApprovalNotification(user, saved);
        return converter.toResponse(saved);
    }

    @Transactional
    public BorrowResponse returnBook(Long recordId, Long userId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("借阅记录不存在。"));

        validator.validateUserPermission(record.getUserId(), userId, "return");

        if (record.getStatus() != BorrowStatus.BORROWED && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new IllegalArgumentException("只有借阅中的图书才能归还。");
        }

        BorrowValidator.OverdueInfo overdueInfo = validator.calculateOverdue(record.getDueDate());
        if (overdueInfo.isOverdue()) {
            record.setOverdueDays(overdueInfo.getDays());
            record.setFineAmount(overdueInfo.getFineAmount());
        }

        bookService.increaseAvailableCopies(record.getBookId());
        record.setReturnDate(now());
        record.setStatus(BorrowStatus.RETURNED);

        BorrowRecord saved = borrowRecordRepository.save(record);
        reservationService.notifyNextReservation(record.getBookId());

        User user = userService.getUserById(record.getUserId());
        notificationHelper.sendReturnNotification(user, saved);
        return converter.toResponse(saved);
    }

    @Transactional
    public BorrowResponse renewBorrow(Long recordId, Long userId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("借阅记录不存在。"));

        validator.validateUserPermission(record.getUserId(), userId, "renew");
        long waitingReservations = reservationService.countWaitingReservations(record.getBookId());
        validator.validateCanRenew(record, waitingReservations);

        record.setDueDate(record.getDueDate().plusDays(RENEW_DAYS));
        record.setRenewed(true);
        record.setRenewCount(record.getRenewCount() + 1);

        BorrowRecord saved = borrowRecordRepository.save(record);
        User user = userService.getUserById(record.getUserId());
        notificationHelper.sendRenewNotification(user, saved);
        return converter.toResponse(saved);
    }

    @Transactional
    public BorrowResponse confirmPickup(Long recordId, Long userId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("借阅记录不存在。"));

        validator.validateUserPermission(record.getUserId(), userId, "pickup");

        if (record.getStatus() != BorrowStatus.APPROVED) {
            throw new IllegalArgumentException("只有已通过审核的申请才能取书。");
        }

        LocalDateTime pickupTime = now();
        record.setBorrowDate(pickupTime);
        record.setDueDate(pickupTime.plusDays(DEFAULT_BORROW_DAYS));
        record.setStatus(BorrowStatus.BORROWED);

        BorrowRecord saved = borrowRecordRepository.save(record);
        User user = userService.getUserById(record.getUserId());
        notificationHelper.sendApprovalNotification(user, saved);
        return converter.toResponse(saved);
    }

    public List<BorrowResponse> getUserBorrowHistory(Long userId) {
        return borrowRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(converter::toResponse)
            .collect(Collectors.toList());
    }

    public Page<BorrowResponse> getUserBorrowHistory(Long userId, Pageable pageable) {
        Page<BorrowRecord> records = borrowRecordRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return records.map(converter::toResponse);
    }

    public List<BorrowResponse> getUserCurrentBorrows(Long userId) {
        return borrowRecordRepository.findByUserIdAndStatusIn(userId, BorrowRecord.ACTIVE_STATUSES).stream()
            .map(converter::toResponse)
            .collect(Collectors.toList());
    }

    public Page<BorrowResponse> getUserCurrentBorrows(Long userId, Pageable pageable) {
        List<BorrowStatus> activeStatuses = Arrays.asList(
            BorrowStatus.PENDING,
            BorrowStatus.APPROVED,
            BorrowStatus.BORROWED,
            BorrowStatus.OVERDUE
        );
        Page<BorrowRecord> records = borrowRecordRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses, pageable);
        return records.map(converter::toResponse);
    }

    public List<BorrowResponse> getPendingBorrows() {
        return borrowRecordRepository.findByStatusOrderByCreatedAtAsc(BorrowStatus.PENDING).stream()
            .map(converter::toResponse)
            .collect(Collectors.toList());
    }

    public Page<BorrowResponse> getPendingBorrows(Pageable pageable) {
        Page<BorrowRecord> records = borrowRecordRepository
            .findByStatusOrderByCreatedAtAsc(BorrowStatus.PENDING, pageable);
        return records.map(converter::toResponse);
    }

    /**
     * 获取用户可续借的书籍列表
     * 条件：状态为 BORROWED，未逾期，未续借过
     */
    public List<BorrowResponse> getRenewableBorrows(Long userId) {
        List<BorrowRecord> borrowRecords = borrowRecordRepository
            .findByUserIdAndStatusOrderByDueDateAsc(userId, BorrowStatus.BORROWED);

        return borrowRecords.stream()
            .filter(record -> {
                // 只返回未续借且有效的记录
                return record.getRenewCount() == 0 &&
                       record.getDueDate() != null &&
                       record.getDueDate().isAfter(now());
            })
            .map(converter::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void checkOverdue() {
        List<BorrowRecord> records = borrowRecordRepository.findByStatusAndDueDateBefore(
            BorrowStatus.BORROWED,
            now()
        );

        for (BorrowRecord record : records) {
            record.setStatus(BorrowStatus.OVERDUE);
            BorrowRecord saved = borrowRecordRepository.save(record);

            User user = userService.getUserById(saved.getUserId());
            notificationHelper.sendOverdueNotification(user, saved);
        }
    }

    private BorrowRecord buildBorrowRecord(Long userId, User user, Book book, BorrowRequest request) {
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(book.getId());
        record.setStudentId(user.getStudentId());
        record.setBookTitle(book.getTitle());
        record.setBookIsbn(book.getIsbn());
        record.setNotes(request.getNotes());
        return record;
    }

    private boolean requiresManualApproval(Book book, Long userId) {
        if (book.getCirculationPolicy() == Book.CirculationPolicy.MANUAL) {
            return true;
        }
        return !borrowRecordRepository.findByUserIdAndStatus(userId, BorrowStatus.OVERDUE).isEmpty();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
