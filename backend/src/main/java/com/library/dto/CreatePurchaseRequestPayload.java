package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePurchaseRequestPayload {

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过 200 个字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过 100 个字符")
    private String author;

    @Size(max = 32, message = "ISBN 长度不能超过 32 个字符")
    private String isbn;

    @Size(max = 500, message = "申请理由长度不能超过 500 个字符")
    private String reason;
}
