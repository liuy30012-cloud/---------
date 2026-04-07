package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestCreateResponseDTO {
    private Boolean created;
    private String conflictType;
    private Long existingRequestId;
    private Long existingBookId;
    private PurchaseRequestItemDTO request;
}
