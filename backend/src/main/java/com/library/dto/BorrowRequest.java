package com.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BorrowRequest {

    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    @Size(max = 500, message = "借阅备注不能超过500个字符")
    private String notes; // 借阅备注
}
