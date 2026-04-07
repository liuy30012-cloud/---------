package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookReviewRequest {

    @NotNull(message = "图书 ID 不能为空")
    private Long bookId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于 1 分")
    @Max(value = 5, message = "评分不能高于 5 分")
    private Integer rating;

    @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
    private String content;
}
