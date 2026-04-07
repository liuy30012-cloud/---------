package com.library.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BorrowApprovalDecisionRequest {

    private boolean approved;

    @Size(max = 500, message = "驳回原因不能超过 500 个字符")
    private String rejectReason;
}
