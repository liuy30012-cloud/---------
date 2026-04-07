package com.library.service.borrow;

import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class BorrowValidator {

    private final Clock clock;

    public static final int MAX_BORROW_COUNT = 5;
    public static final int DEFAULT_BORROW_DAYS = 30;
    public static final int RENEW_DAYS = 15;
    public static final int MAX_RENEW_COUNT = 1;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");

    public void validateCanBorrow(long currentBorrowCount) {
        if (currentBorrowCount >= MAX_BORROW_COUNT) {
            throw new IllegalArgumentException("已达到借阅上限（包含待审核申请）。");
        }
    }

    public void validateCanRenew(BorrowRecord record, long waitingReservations) {
        if (record.getStatus() == BorrowStatus.OVERDUE || now().isAfter(record.getDueDate())) {
            throw new IllegalArgumentException("当前借阅已逾期，不能续借。");
        }

        if (record.getStatus() != BorrowStatus.BORROWED) {
            throw new IllegalArgumentException("只有借阅中的记录才能续借。");
        }

        if (record.getRenewCount() >= MAX_RENEW_COUNT) {
            throw new IllegalArgumentException("已达到最大续借次数。");
        }

        if (waitingReservations > 0) {
            throw new IllegalArgumentException("该图书已有预约排队，暂时不能续借。");
        }
    }

    public void validateUserPermission(Long recordUserId, Long currentUserId, String operation) {
        if (!recordUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("无权操作其他用户的借阅记录。");
        }
    }

    public OverdueInfo calculateOverdue(LocalDateTime dueDate) {
        LocalDateTime now = now();
        if (now.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate.toLocalDate(), now.toLocalDate());
            if (overdueDays <= 0) {
                overdueDays = 1;
            }

            BigDecimal fineAmount = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
            return new OverdueInfo((int) overdueDays, fineAmount);
        }

        return new OverdueInfo(0, BigDecimal.ZERO);
    }

    public static class OverdueInfo {
        private final int days;
        private final BigDecimal fineAmount;

        public OverdueInfo(int days, BigDecimal fineAmount) {
            this.days = days;
            this.fineAmount = fineAmount;
        }

        public int getDays() {
            return days;
        }

        public BigDecimal getFineAmount() {
            return fineAmount;
        }

        public boolean isOverdue() {
            return days > 0;
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
