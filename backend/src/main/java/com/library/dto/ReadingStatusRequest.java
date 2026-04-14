package com.library.dto;

import com.library.model.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReadingStatusRequest {

    @NotNull(message = "图书 ID 不能为空")
    private Long bookId;

    @NotNull(message = "阅读状态不能为空")
    private ReadingStatus status;

    @Size(max = 2000, message = "备注不能超过 2000 个字符")
    private String notes;
}
