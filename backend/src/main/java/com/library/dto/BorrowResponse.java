package com.library.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;
    private Boolean renewed;
    private Integer renewCount;
    private Integer overdueDays;
    private BigDecimal fineAmount;
    private Boolean finePaid;
    private String notes;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private String nextAction;
    private String statusHint;
    private LocalDateTime pickupDeadline;
    private String coverUrl;
    private String location;
    private String circulationPolicy;
    private LocalDateTime createdAt;
}
