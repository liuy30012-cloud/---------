package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationFailure {
    private Long bookId;
    private Integer row;
    private String reason;

    public BatchOperationFailure(Long bookId, String reason) {
        this.bookId = bookId;
        this.reason = reason;
    }

    public BatchOperationFailure(Integer row, String reason) {
        this.row = row;
        this.reason = reason;
    }
}
