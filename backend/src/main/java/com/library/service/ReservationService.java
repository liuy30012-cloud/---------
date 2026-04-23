package com.library.service;

import com.library.dto.BorrowResponse;
import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.model.ReservationRecord;
import com.library.model.ReservationRecord.ReservationStatus;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import com.library.service.borrow.BorrowConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.library.service.borrow.BorrowValidator.DEFAULT_BORROW_DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int RESERVATION_EXPIRE_DAYS = 3;

    private final ReservationRecordRepository reservationRecordRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BorrowConverter borrowConverter;
    private final Clock clock;

    @Transactional
    public ReservationResponse reserveBook(Long userId, ReservationRequest request) {
        if (request == null || request.getBookId() == null) {
            throw new IllegalArgumentException("预约申请必须包含图书 ID。");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }

        Book book = bookRepository.findByIdWithLock(request.getBookId())
            .orElse(null);
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY) {
            throw new IllegalArgumentException("该图书仅供馆内阅览，不能预约。");
        }

        if (book.getAvailableCopies() == null || book.getTotalCopies() == null
            || book.getAvailableCopies() < 0 || book.getTotalCopies() <= 0) {
            throw new IllegalArgumentException("图书库存数据异常。");
        }

        if (book.getAvailableCopies() > 0) {
            throw new IllegalArgumentException("该图书当前有馆藏可借，请直接借阅。");
        }

        boolean hasActiveReservation =
            reservationRecordRepository.existsByUserIdAndBookIdAndStatus(userId, book.getId(), ReservationStatus.WAITING)
                || reservationRecordRepository.existsByUserIdAndBookIdAndStatus(
                    userId, book.getId(), ReservationStatus.AVAILABLE
                );
        if (hasActiveReservation) {
            throw new IllegalArgumentException("你已预约过这本书。");
        }

        if (borrowRecordRepository.existsByUserIdAndBookIdAndStatusIn(userId, book.getId(), BorrowRecord.ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("你已存在该书的借阅或借阅申请记录。");
        }

        List<ReservationRecord> queue = reservationRecordRepository
            .findByBookIdAndStatusOrderByCreatedAtAsc(book.getId(), ReservationStatus.WAITING);
        int queuePosition = queue.size() + 1;

        ReservationRecord record = new ReservationRecord();
        record.setUserId(userId);
        record.setBookId(book.getId());
        record.setStudentId(user.getStudentId());
        record.setBookTitle(book.getTitle());
        record.setReservationDate(now());
        record.setExpireDate(null);
        record.setStatus(ReservationStatus.WAITING);
        record.setQueuePosition(queuePosition);

        ReservationRecord saved = reservationRecordRepository.save(record);
        notificationService.sendReservationNotification(user, book, queuePosition);
        return convertToResponse(saved, book);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId, String reason) {
        ReservationRecord record = reservationRecordRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("预约记录不存在。"));

        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能取消自己的预约。");
        }

        if (record.getStatus() != ReservationStatus.WAITING && record.getStatus() != ReservationStatus.AVAILABLE) {
            throw new IllegalArgumentException("只有排队中或待取书的预约才能取消。");
        }

        ReservationStatus oldStatus = record.getStatus();
        record.setStatus(ReservationStatus.CANCELLED);
        record.setCancelReason(reason);
        reservationRecordRepository.save(record);

        if (oldStatus == ReservationStatus.AVAILABLE) {
            notifyNextReservation(record.getBookId());
        }

        updateQueuePositions(record.getBookId());
    }

    @Transactional
    public void notifyNextReservation(Long bookId) {
        Book book = bookService.getBookById(bookId);
        if (book == null || book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            return;
        }

        long reservedSlots = reservationRecordRepository.countByBookIdAndStatus(bookId, ReservationStatus.AVAILABLE);
        int promotableSlots = book.getAvailableCopies() - (int) reservedSlots;
        if (promotableSlots <= 0) {
            return;
        }

        List<ReservationRecord> queue = reservationRecordRepository
            .findByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING);
        if (queue.isEmpty()) {
            return;
        }

        LocalDateTime notifyTime = now();
        int promotedCount = Math.min(promotableSlots, queue.size());
        List<ReservationRecord> promotedReservations = queue.subList(0, promotedCount);

        for (ReservationRecord reservation : promotedReservations) {
            reservation.setStatus(ReservationStatus.AVAILABLE);
            reservation.setNotifyDate(notifyTime);

            // 根据用户历史行为动态设置取书窗口
            int windowDays = calculatePickupWindow(reservation.getUserId());
            reservation.setPickupWindowDays(windowDays);

            LocalDateTime expireTime = notifyTime.plusDays(windowDays);
            reservation.setExpireDate(expireTime);
            reservation.setOriginalExpireDate(expireTime);
            reservation.setExpiryReminderSent(false);
        }
        reservationRecordRepository.saveAll(promotedReservations);
        updateQueuePositions(bookId);

        for (ReservationRecord reservation : promotedReservations) {
            User user = userService.getUserById(reservation.getUserId());
            if (user != null) {
                notificationService.sendBookAvailableNotification(user, book);
            }
        }
    }

    public List<ReservationResponse> getUserReservations(Long userId) {
        return reservationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(record -> convertToResponse(record, bookService.getBookById(record.getBookId())))
            .collect(Collectors.toList());
    }

    @Transactional
    public BorrowResponse pickupReservation(Long reservationId, Long userId) {
        ReservationRecord reservation = reservationRecordRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("预约记录不存在。"));

        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能领取自己的预约图书。");
        }

        if (reservation.getStatus() != ReservationStatus.AVAILABLE) {
            throw new IllegalArgumentException("只有待取书的预约才能领取。");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }

        Book book = bookService.getBookById(reservation.getBookId());
        if (book == null) {
            throw new IllegalArgumentException("图书不存在。");
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException("预约图书当前已无法领取。");
        }

        if (borrowRecordRepository.existsByUserIdAndBookIdAndStatusIn(userId, reservation.getBookId(), BorrowRecord.ACTIVE_STATUSES)) {
            throw new IllegalArgumentException("你已存在该书的借阅或借阅申请记录。");
        }

        bookService.decreaseAvailableCopies(book.getId());
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRecordRepository.save(reservation);

        LocalDateTime pickupTime = now();
        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUserId(userId);
        borrowRecord.setBookId(book.getId());
        borrowRecord.setStudentId(user.getStudentId());
        borrowRecord.setBookTitle(book.getTitle());
        borrowRecord.setBookIsbn(book.getIsbn());
        borrowRecord.setBorrowDate(pickupTime);
        borrowRecord.setDueDate(pickupTime.plusDays(DEFAULT_BORROW_DAYS));
        borrowRecord.setStatus(BorrowStatus.BORROWED);
        borrowRecord.setApprovedAt(pickupTime);
        borrowRecord.setApprovedBy("RESERVATION");
        borrowRecord.setNotes("预约取书 #" + reservationId);

        BorrowRecord savedBorrow = borrowRecordRepository.save(borrowRecord);
        return borrowConverter.toResponse(savedBorrow);
    }

    @Transactional
    public void checkExpiredReservations() {
        LocalDateTime currentTime = now();
        List<ReservationRecord> expired = reservationRecordRepository
            .findByStatusAndExpireDateBefore(ReservationStatus.AVAILABLE, currentTime);

        for (ReservationRecord record : expired) {
            if (record.getExpireDate() != null && record.getExpireDate().isBefore(currentTime)) {
                record.setStatus(ReservationStatus.EXPIRED);
                reservationRecordRepository.save(record);
                notifyNextReservation(record.getBookId());
            }
        }
    }

    public long countWaitingReservations(Long bookId) {
        return reservationRecordRepository.countWaitingReservationsByBookId(bookId);
    }

    public boolean hasOtherActiveReservations(Long bookId, Long excludeUserId) {
        return reservationRecordRepository.countActiveOtherReservations(bookId, excludeUserId) > 0;
    }

    public List<ReservationRecord> findExpiringSoonReservations(LocalDateTime startDate, LocalDateTime endDate) {
        List<ReservationRecord> expiringSoon = reservationRecordRepository
            .findByStatusAndExpiryReminderSentAndExpireDateBetween(
                ReservationStatus.AVAILABLE,
                false,
                startDate,
                endDate
            );

        // 标记为已发送提醒
        for (ReservationRecord record : expiringSoon) {
            record.setExpiryReminderSent(true);
        }

        if (!expiringSoon.isEmpty()) {
            reservationRecordRepository.saveAll(expiringSoon);
        }

        return expiringSoon;
    }

    @Transactional
    public ReservationResponse extendPickupDeadline(Long reservationId, Long userId) {
        ReservationRecord record = reservationRecordRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("预约记录不存在。"));

        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能延长自己的预约取书时限。");
        }

        if (record.getStatus() != ReservationStatus.AVAILABLE) {
            throw new IllegalArgumentException("只有待取书的预约才能延长时限。");
        }

        if (record.getExpireDate() == null || record.getExpireDate().isBefore(now())) {
            throw new IllegalArgumentException("预约已过期,无法延长时限。");
        }

        Integer extensionCount = record.getExtensionCount() == null ? 0 : record.getExtensionCount();
        if (extensionCount >= 1) {
            throw new IllegalArgumentException("每个预约最多只能延长1次。");
        }

        // 延长2天
        LocalDateTime newExpireDate = record.getExpireDate().plusDays(2);
        record.setExpireDate(newExpireDate);
        record.setExtensionCount(extensionCount + 1);
        record.setExtensionDate(now());
        record.setExpiryReminderSent(false);

        ReservationRecord saved = reservationRecordRepository.save(record);

        User user = userService.getUserById(userId);
        if (user != null) {
            notificationService.sendReservationExtendedNotification(user, saved);
        }

        Book book = bookService.getBookById(record.getBookId());
        return convertToResponse(saved, book);
    }

    private int calculatePickupWindow(Long userId) {
        // 查询用户最近10条预约历史
        List<ReservationRecord> history = reservationRecordRepository
            .findTop10ByUserIdAndStatusInOrderByCreatedAtDesc(
                userId,
                List.of(ReservationStatus.COMPLETED, ReservationStatus.EXPIRED)
            );

        if (history.isEmpty()) {
            return RESERVATION_EXPIRE_DAYS; // 默认3天
        }

        // 计算过期率
        long expiredCount = history.stream()
            .filter(r -> r.getStatus() == ReservationStatus.EXPIRED)
            .count();
        double expireRate = (double) expiredCount / history.size();

        // 如果过期率 > 30%,缩短为2天;如果过期率 = 0且样本>=3,延长为5天
        if (expireRate > 0.3) {
            return 2;
        } else if (expireRate == 0 && history.size() >= 3) {
            return 5;
        }
        return RESERVATION_EXPIRE_DAYS;
    }

    @Transactional
    private void updateQueuePositions(Long bookId) {
        List<ReservationRecord> queue = reservationRecordRepository
            .findByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING);

        boolean hasChanges = false;
        for (int i = 0; i < queue.size(); i++) {
            ReservationRecord record = queue.get(i);
            int newPosition = i + 1;
            if (record.getQueuePosition() != newPosition) {
                record.setQueuePosition(newPosition);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            reservationRecordRepository.saveAll(queue);
        }
    }

    private ReservationResponse convertToResponse(ReservationRecord record, Book book) {
        ReservationResponse response = new ReservationResponse();
        response.setId(record.getId());
        response.setBookId(record.getBookId());
        response.setBookTitle(record.getBookTitle());
        response.setReservationDate(record.getReservationDate());
        response.setExpireDate(record.getExpireDate());
        response.setNotifyDate(record.getNotifyDate());
        response.setPickupDeadline(record.getExpireDate());
        response.setStatus(record.getStatus().name());
        response.setQueuePosition(record.getQueuePosition());
        response.setQueueAhead(record.getQueuePosition() == null ? 0 : Math.max(record.getQueuePosition() - 1, 0));
        response.setEstimatedWaitDays(estimateWaitDays(record, book));
        response.setNextAction(resolveNextAction(record.getStatus()));
        response.setStatusHint(resolveStatusHint(record));
        response.setCreatedAt(record.getCreatedAt());

        // 延期相关字段
        response.setExtensionCount(record.getExtensionCount() == null ? 0 : record.getExtensionCount());
        response.setCanExtend(canExtendDeadline(record));
        response.setPickupWindowDays(record.getPickupWindowDays() == null ? RESERVATION_EXPIRE_DAYS : record.getPickupWindowDays());

        // 过期提醒相关字段
        if (record.getStatus() == ReservationStatus.AVAILABLE && record.getExpireDate() != null) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now(), record.getExpireDate());
            response.setDaysUntilExpiry((int) daysUntil);
            response.setIsExpiringSoon(daysUntil <= 1 && daysUntil >= 0);
        }

        if (book != null) {
            response.setCoverUrl(book.getCoverUrl());
            response.setLocation(book.getLocation());
        }
        return response;
    }

    private boolean canExtendDeadline(ReservationRecord record) {
        // 只有 AVAILABLE 状态、未过期、延期次数<1 才能延期
        Integer extensionCount = record.getExtensionCount() == null ? 0 : record.getExtensionCount();
        return record.getStatus() == ReservationStatus.AVAILABLE
            && record.getExpireDate() != null
            && record.getExpireDate().isAfter(now())
            && extensionCount < 1;
    }

    private int estimateWaitDays(ReservationRecord record, Book book) {
        if (record.getStatus() != ReservationStatus.WAITING || record.getQueuePosition() == null || record.getQueuePosition() <= 1) {
            return 0;
        }
        int copies = Math.max(book == null || book.getTotalCopies() == null ? 1 : book.getTotalCopies(), 1);
        return (int) Math.ceil((double) (record.getQueuePosition() - 1) * DEFAULT_BORROW_DAYS / copies);
    }

    private String resolveNextAction(ReservationStatus status) {
        return switch (status) {
            case WAITING -> "WAIT";
            case AVAILABLE -> "PICKUP";
            case COMPLETED -> "VIEW_BORROW";
            case CANCELLED, EXPIRED -> "SEARCH_AGAIN";
        };
    }

    private String resolveStatusHint(ReservationRecord record) {
        return switch (record.getStatus()) {
            case WAITING -> "正在预约队列中排队。";
            case AVAILABLE -> "图书已可领取，请在截止时间前完成取书。";
            case COMPLETED -> "预约已完成取书。";
            case CANCELLED -> "预约已取消。";
            case EXPIRED -> "预约取书时间已过期。";
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
