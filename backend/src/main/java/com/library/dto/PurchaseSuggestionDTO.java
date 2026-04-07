package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSuggestionDTO {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private Integer currentCopies;
    private Integer suggestedCopies;
    private Integer additionalCopies;
    private Long borrowCount;
    private Double averageWaitTime; // 平均等待时间（天）
    private Integer reservationCount; // 预约人数
    private String reason; // 建议原因
    private String priority; // HIGH, MEDIUM, LOW
    private Double score; // 采购优先级分数
    private String coverUrl;
}
