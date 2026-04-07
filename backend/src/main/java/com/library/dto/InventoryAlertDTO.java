package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertDTO {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private Integer totalCopies;
    private Integer availableCopies;
    private Integer borrowedCopies;
    private Long borrowCount; // 借阅次数
    private String alertType; // LOW_STOCK, OUT_OF_STOCK, HIGH_DEMAND
    private String alertLevel; // WARNING, CRITICAL
    private String message;
    private String coverUrl;
}
