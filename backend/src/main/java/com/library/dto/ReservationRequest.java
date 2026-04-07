package com.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {

    @NotNull(message = "图书 ID 不能为空")
    private Long bookId;
}
