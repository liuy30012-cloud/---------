package com.library.service.borrow;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 借阅通知助手
 *
 * 职责：
 * - 统一管理借阅相关的通知发送
 * - 安全处理用户不存在的情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowNotificationHelper {

    private final NotificationService notificationService;

    /**
     * 发送借阅申请通知
     */
    public void sendApplicationNotification(User user, Book book) {
        if (user != null && book != null) {
            notificationService.sendBorrowApplicationNotification(user, book);
        } else {
            log.warn("无法发送借阅申请通知：用户或书籍为空");
        }
    }

    /**
     * 发送审批通知
     */
    public void sendApprovalNotification(User user, BorrowRecord record) {
        if (user != null) {
            notificationService.sendBorrowApprovalNotification(user, record);
        } else {
            log.warn("用户 {} 不存在，无法发送审批通知", record.getUserId());
        }
    }

    /**
     * 发送归还通知
     */
    public void sendReturnNotification(User user, BorrowRecord record) {
        if (user != null) {
            notificationService.sendReturnNotification(user, record);
        } else {
            log.error("用户 {} 不存在，无法发送归还通知，但归还操作已完成", record.getUserId());
        }
    }

    /**
     * 发送续借通知
     */
    public void sendRenewNotification(User user, BorrowRecord record) {
        if (user != null) {
            notificationService.sendRenewNotification(user, record);
        } else {
            log.warn("用户 {} 不存在，无法发送续借通知", record.getUserId());
        }
    }

    /**
     * 发送逾期通知
     */
    public void sendOverdueNotification(User user, BorrowRecord record) {
        if (user != null) {
            notificationService.sendOverdueNotification(user, record);
        } else {
            log.warn("用户 {} 不存在，无法发送逾期通知", record.getUserId());
        }
    }
}
