package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestVoteResponseDTO {
    private Boolean alreadyVoted;
    private PurchaseRequestItemDTO request;
}
