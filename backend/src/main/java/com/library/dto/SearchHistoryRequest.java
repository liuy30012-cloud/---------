package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchHistoryRequest {

    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 120, message = "搜索关键词不能超过 120 个字符")
    private String keyword;

    @PositiveOrZero(message = "结果数量不能为负数")
    private Integer resultCount = 0;

    @Size(max = 500, message = "目标路径不能超过 500 个字符")
    private String targetPath;

    private String queryPayload;

    private Boolean saved = false;

    @Size(max = 120, message = "标签不能超过 120 个字符")
    private String label;
}
