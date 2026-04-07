package com.library.scheduler;

import com.library.service.BorrowService;
import com.library.service.ReservationService;
import com.library.service.NotificationService;
import com.library.service.UserService;
import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final BorrowService borrowService;
    private final ReservationService reservationService;
    private final BorrowRecordRepository borrowRecordRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final Clock clock;

    /**
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "checkOverdue", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void checkOverdue() {
        log.info("开始检查逾期借阅记录");
        borrowService.checkOverdue();
        log.info("逾期检查完成");
    }

    /**
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "checkExpiredReservations", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void checkExpiredReservations() {
        log.info("开始检查过期预约");
        reservationService.checkExpiredReservations();
        log.info("过期预约检查完成");
    }

    /**
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @SchedulerLock(name = "sendDueDateReminders", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void sendDueDateReminders() {
        log.info("开始发送到期提醒");

        LocalDateTime now = now();
        LocalDateTime threeDaysLater = now.plusDays(3);

        List<BorrowRecord> records = borrowRecordRepository.findByStatusAndDueDateBetween(
            BorrowStatus.BORROWED,
            now,
            threeDaysLater
        );

        for (BorrowRecord record : records) {
            long daysLeft = ChronoUnit.DAYS.between(now, record.getDueDate());
            // 只提醒1-3天内到期的（排除当天到期的，由紧急提醒处理）
            if (daysLeft >= 1 && daysLeft <= 3) {
                var user = userService.getUserById(record.getUserId());
                if (user != null) {
                    notificationService.sendDueDateReminderNotification(user, record, (int) daysLeft);
                } else {
                    log.warn("用户 {} 不存在，无法发送到期提醒", record.getUserId());
                }
            }
        }

        log.info("到期提醒发送完成，共发送{}条提醒", records.size());
    }

    /**
     * 每天上午10点检查即将过期的预约(取书时限前1天)
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @SchedulerLock(name = "sendReservationExpiryReminders", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void sendReservationExpiryReminders() {
        log.info("开始发送预约快过期提醒");

        LocalDateTime now = now();
        LocalDateTime tomorrow = now.plusDays(1);

        // 查询状态为AVAILABLE、未发送提醒、1天内过期的预约
        List<com.library.model.ReservationRecord> expiringSoon =
            reservationService.findExpiringSoonReservations(now, tomorrow);

        int sentCount = 0;
        for (com.library.model.ReservationRecord record : expiringSoon) {
            var user = userService.getUserById(record.getUserId());
            if (user != null) {
                notificationService.sendReservationExpiringSoonNotification(user, record);
                sentCount++;
            } else {
                log.warn("用户 {} 不存在，无法发送预约过期提醒", record.getUserId());
            }
        }

        log.info("预约快过期提醒发送完成，共发送{}条提醒", sentCount);
    }

    /**
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @SchedulerLock(name = "sendUrgentDueDateReminders", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void sendUrgentDueDateReminders() {
        log.info("开始发送紧急到期提醒");

        LocalDateTime now = now();
        LocalDateTime tomorrow = now.plusDays(1);

        List<BorrowRecord> records = borrowRecordRepository.findByStatusAndDueDateBetween(
            BorrowStatus.BORROWED,
            now,
            tomorrow
        );

        for (BorrowRecord record : records) {
            var user = userService.getUserById(record.getUserId());
            if (user != null) {
                notificationService.sendDueDateReminderNotification(user, record, 1);
            } else {
                log.warn("用户 {} 不存在，无法发送紧急到期提醒", record.getUserId());
            }
        }

        log.info("紧急到期提醒发送完成，共发送{}条提醒", records.size());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
