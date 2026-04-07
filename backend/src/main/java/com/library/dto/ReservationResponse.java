package com.library.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReservationResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private LocalDateTime reservationDate;
    private LocalDateTime expireDate;
    private LocalDateTime notifyDate;
    private LocalDateTime pickupDeadline;
    private String status;
    private Integer queuePosition;
    private Integer queueAhead;
    private Integer estimatedWaitDays;
    private String nextAction;
    private String statusHint;
    private String coverUrl;
    private String location;
    private LocalDateTime createdAt;

    // 延期相关字段
    private Integer extensionCount;
    private Boolean canExtend;
    private Integer pickupWindowDays;

    // 过期提醒相关字段
    private Integer daysUntilExpiry;
    private Boolean isExpiringSoon;
}
