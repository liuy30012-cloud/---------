package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteResponse {
    private int successCount;
    private int failedCount;
    private List<BatchOperationFailure> failures;
}
