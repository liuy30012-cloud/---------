package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSuggestionSummaryDTO {
    private Integer totalSuggestions;
    private Integer highPriority;
    private Integer mediumPriority;
    private Integer lowPriority;
    private Integer totalAdditionalCopies;
    private Double estimatedBudget; // 预估预算（假设每本书50元）
    private List<PurchaseSuggestionDTO> suggestions;
}
