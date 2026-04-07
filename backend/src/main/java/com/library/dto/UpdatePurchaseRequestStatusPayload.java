package com.library.dto;

import com.library.model.PurchaseRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePurchaseRequestStatusPayload {

    @NotNull(message = "采购状态不能为空")
    private PurchaseRequestStatus status;

    @Size(max = 200, message = "状态备注长度不能超过 200 个字符")
    private String statusNote;
}
