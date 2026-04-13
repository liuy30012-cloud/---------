package com.library.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DamageReportResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long reporterId;
    private String reporterName;
    private List<String> damageTypes;
    private String description;
    private List<String> photoUrls;
    private String status;
    private String adminNotes;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
