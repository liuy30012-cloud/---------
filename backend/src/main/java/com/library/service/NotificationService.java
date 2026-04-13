package com.library.service;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.NotificationRecord;
import com.library.model.User;
import com.library.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationRecordRepository notificationRecordRepository;

    public List<NotificationRecord> getUserNotifications(Long userId) {
        return notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationRecord record = notificationRecordRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("通知不存在。"));

        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能操作自己的通知。");
        }

        record.setRead(true);
        notificationRecordRepository.save(record);
    }

    @Transactional
    public long markAllAsRead(Long userId) {
        List<NotificationRecord> records = notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long updated = 0;
        for (NotificationRecord record : records) {
            if (!Boolean.TRUE.equals(record.getRead())) {
                record.setRead(true);
                updated++;
            }
        }
        notificationRecordRepository.saveAll(records);
        return updated;
    }

    public void sendBorrowApplicationNotification(User user, Book book) {
        safelySendNotification(() -> createNotification(
            user,
            "BORROW_SUBMITTED",
            "hourglass_top",
            "借阅申请已提交",
            "Borrow request submitted",
            "《" + book.getTitle() + "》已进入借阅流程，我们会同步最新状态。",
            "\"" + book.getTitle() + "\" is now in the borrow workflow. We will keep the status updated.",
            "/my-borrows"
        ), "borrow application");
    }

    public void sendBorrowApprovalNotification(User user, BorrowRecord record) {
        safelySendNotification(() -> {
            String targetPath = "/my-borrows";
            switch (record.getStatus()) {
                case APPROVED -> createNotification(
                    user,
                    "BORROW_APPROVED",
                    "check_circle",
                    "借阅已通过",
                    "Borrow approved",
                    "《" + record.getBookTitle() + "》已通过审批，请在 " + formatDateTime(record.getPickupDeadline()) + " 前到馆取书。",
                    "\"" + record.getBookTitle() + "\" is approved. Please pick it up before " + formatDateTime(record.getPickupDeadline()) + ".",
                    targetPath
                );
                case REJECTED -> createNotification(
                    user,
                    "BORROW_REJECTED",
                    "cancel",
                    "借阅未通过",
                    "Borrow rejected",
                    "《" + record.getBookTitle() + "》未通过审批。" + withPrefix(record.getRejectReason(), "原因："),
                    "\"" + record.getBookTitle() + "\" was not approved." + withPrefix(record.getRejectReason(), " Reason: "),
                    targetPath
                );
                case BORROWED -> createNotification(
                    user,
                    "BORROW_PICKED_UP",
                    "local_library",
                    "取书完成",
                    "Pickup confirmed",
                    "《" + record.getBookTitle() + "》已开始借阅，到期日为 " + formatDateTime(record.getDueDate()) + "。",
                    "\"" + record.getBookTitle() + "\" is now borrowed. It is due on " + formatDateTime(record.getDueDate()) + ".",
                    targetPath
                );
                default -> log.info("Skip notification for borrow status {}", record.getStatus());
            }
        }, "borrow approval");
    }

    public void sendReturnNotification(User user, BorrowRecord record) {
        safelySendNotification(() -> createNotification(
            user,
            "BORROW_RETURNED",
            "assignment_return",
            "图书已归还",
            "Book returned",
            "《" + record.getBookTitle() + "》已归还。" + withPrefix(formatFine(record), "费用："),
            "\"" + record.getBookTitle() + "\" has been returned." + withPrefix(formatFine(record), " Fine: "),
            "/my-borrows"
        ), "borrow return");
    }

    public void sendRenewNotification(User user, BorrowRecord record) {
        safelySendNotification(() -> createNotification(
            user,
            "BORROW_RENEWED",
            "autorenew",
            "续借成功",
            "Renewal completed",
            "《" + record.getBookTitle() + "》已续借，新到期日为 " + formatDateTime(record.getDueDate()) + "。",
            "\"" + record.getBookTitle() + "\" has been renewed. New due date: " + formatDateTime(record.getDueDate()) + ".",
            "/my-borrows"
        ), "borrow renew");
    }

    public void sendOverdueNotification(User user, BorrowRecord record) {
        safelySendNotification(() -> createNotification(
            user,
            "BORROW_OVERDUE",
            "warning",
            "图书已逾期",
            "Borrow overdue",
            "《" + record.getBookTitle() + "》已逾期，请尽快归还。",
            "\"" + record.getBookTitle() + "\" is overdue. Please return it as soon as possible.",
            "/my-borrows"
        ), "borrow overdue");
    }

    public void sendReservationNotification(User user, Book book, int queuePosition) {
        safelySendNotification(() -> createNotification(
            user,
            "RESERVATION_WAITING",
            "bookmark_add",
            "预约已进入队列",
            "Reservation queued",
            "《" + book.getTitle() + "》预约成功，当前前方还有 " + Math.max(queuePosition - 1, 0) + " 位读者。",
            "\"" + book.getTitle() + "\" is reserved. There are " + Math.max(queuePosition - 1, 0) + " readers ahead of you.",
            "/my-reservations"
        ), "reservation queued");
    }

    public void sendBookAvailableNotification(User user, Book book) {
        safelySendNotification(() -> createNotification(
            user,
            "RESERVATION_AVAILABLE",
            "notifications_active",
            "预约图书已到馆",
            "Reservation ready",
            "《" + book.getTitle() + "》已可领取，请尽快前往预约页查看取书期限。",
            "\"" + book.getTitle() + "\" is ready for pickup. Check the reservation page for the deadline.",
            "/my-reservations"
        ), "reservation available");
    }

    public void sendReservationExpiringSoonNotification(User user, com.library.model.ReservationRecord record) {
        safelySendNotification(() -> createNotification(
            user,
            "RESERVATION_EXPIRING_SOON",
            "schedule",
            "预约即将过期",
            "Reservation expiring soon",
            "《" + record.getBookTitle() + "》的取书时限即将到期，请尽快前往取书或申请延期。",
            "\"" + record.getBookTitle() + "\" pickup deadline is approaching. Please pick it up or request an extension.",
            "/my-reservations"
        ), "reservation expiring soon");
    }

    public void sendReservationExtendedNotification(User user, com.library.model.ReservationRecord record) {
        safelySendNotification(() -> createNotification(
            user,
            "RESERVATION_EXTENDED",
            "update",
            "取书时限已延长",
            "Pickup deadline extended",
            "《" + record.getBookTitle() + "》的取书时限已延长至 " + formatDateTime(record.getExpireDate()) + "。",
            "\"" + record.getBookTitle() + "\" pickup deadline has been extended to " + formatDateTime(record.getExpireDate()) + ".",
            "/my-reservations"
        ), "reservation extended");
    }

    public void sendDueDateReminderNotification(User user, BorrowRecord record, int daysLeft) {
        safelySendNotification(() -> createNotification(
            user,
            "BORROW_DUE_SOON",
            "schedule",
            "借阅即将到期",
            "Borrow due soon",
            "《" + record.getBookTitle() + "》还有 " + daysLeft + " 天到期，请留意归还时间。",
            "\"" + record.getBookTitle() + "\" is due in " + daysLeft + " day(s).",
            "/my-borrows"
        ), "due reminder");
    }

    public void sendSystemNotification(User user, String title, String content) {
        safelySendNotification(() -> createNotification(
            user,
            "SYSTEM",
            "info",
            title,
            title,
            content,
            content,
            null
        ), "system");
    }

    public void sendDamageReportSubmittedNotification(User admin, String bookTitle, String reporterName) {
        safelySendNotification(() -> createNotification(
            admin,
            "DAMAGE_REPORT_SUBMITTED",
            "report_problem",
            "新损坏报告",
            "New damage report",
            "《" + bookTitle + "》被 " + reporterName + " 报告存在损坏问题。",
            "\"" + bookTitle + "\" has been reported as damaged by " + reporterName + ".",
            "/damage-reports"
        ), "damage report submitted");
    }

    public void sendDamageReportInProgressNotification(User reporter, String bookTitle, String adminName) {
        safelySendNotification(() -> createNotification(
            reporter,
            "DAMAGE_REPORT_IN_PROGRESS",
            "progress_activity",
            "损坏报告处理中",
            "Damage report in progress",
            "您报告的《" + bookTitle + "》损坏问题正在由管理员 " + adminName + " 处理。",
            "Your damage report for \"" + bookTitle + "\" is being handled by " + adminName + ".",
            "/damage-reports"
        ), "damage report in progress");
    }

    public void sendDamageReportResolvedNotification(User reporter, String bookTitle) {
        safelySendNotification(() -> createNotification(
            reporter,
            "DAMAGE_REPORT_RESOLVED",
            "check_circle",
            "损坏报告已处理",
            "Damage report resolved",
            "您报告的《" + bookTitle + "》损坏问题已修复。",
            "The damage you reported for \"" + bookTitle + "\" has been repaired.",
            "/damage-reports"
        ), "damage report resolved");
    }

    public void sendDamageReportRejectedNotification(User reporter, String bookTitle) {
        safelySendNotification(() -> createNotification(
            reporter,
            "DAMAGE_REPORT_REJECTED",
            "cancel",
            "损坏报告已驳回",
            "Damage report rejected",
            "您报告的《" + bookTitle + "》损坏问题未通过审核。",
            "Your damage report for \"" + bookTitle + "\" has been rejected.",
            "/damage-reports"
        ), "damage report rejected");
    }

    private void safelySendNotification(Runnable task, String notificationType) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Failed to send {} notification: {}", notificationType, e.getMessage(), e);
        }
    }

    private void createNotification(
        User user,
        String type,
        String icon,
        String titleZh,
        String titleEn,
        String descZh,
        String descEn,
        String targetPath
    ) {
        if (user == null) {
            return;
        }

        NotificationRecord record = new NotificationRecord();
        record.setUserId(user.getId());
        record.setType(type);
        record.setIcon(icon);
        record.setTitleZh(titleZh);
        record.setTitleEn(titleEn);
        record.setDescZh(descZh);
        record.setDescEn(descEn);
        record.setTargetPath(targetPath);
        record.setRead(false);
        notificationRecordRepository.save(record);
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "待定" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String withPrefix(String value, String prefix) {
        return StringUtils.hasText(value) ? prefix + value : "";
    }

    private String formatFine(BorrowRecord record) {
        if (record.getFineAmount() == null || record.getFineAmount().signum() <= 0) {
            return null;
        }
        return record.getFineAmount().toPlainString();
    }
}
