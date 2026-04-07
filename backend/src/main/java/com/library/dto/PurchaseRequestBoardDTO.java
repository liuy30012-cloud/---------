package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestBoardDTO {
    private Integer totalRequests;
    private Integer pendingReviewCount;
    private Integer priorityPoolCount;
    private Integer plannedCount;
    private Integer purchasingCount;
    private Integer arrivedCount;
    private Integer rejectedCount;
    private Integer totalSupportCount;
    private List<PurchaseRequestItemDTO> requests;
}
