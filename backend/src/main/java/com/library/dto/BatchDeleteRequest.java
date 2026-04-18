package com.library.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchDeleteRequest {
    @NotEmpty(message = "图书ID列表不能为空")
    private List<Long> bookIds;
}
