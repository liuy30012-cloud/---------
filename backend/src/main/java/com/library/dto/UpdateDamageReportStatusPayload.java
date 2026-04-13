package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDamageReportStatusPayload {

    @NotBlank(message = "状态不能为空")
    private String status;

    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String adminNotes;
}
