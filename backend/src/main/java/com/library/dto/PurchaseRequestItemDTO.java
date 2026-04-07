package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestItemDTO {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String reason;
    private Long proposerUserId;
    private String proposerName;
    private Integer supportCount;
    private String status;
    private String statusLabel;
    private String statusNote;
    private Integer progressPercent;
    private String progressLabel;
    private Boolean votedByCurrentUser;
    private Boolean canVote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
