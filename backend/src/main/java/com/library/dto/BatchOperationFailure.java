package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 批量操作失败详情
 * 用于记录批量操作中单个项目的失败信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationFailure {
    /** 失败的图书ID（用于已存在图书的操作失败） */
    private Long bookId;

    /** 失败的行号（用于导入文件中的行失败） */
    private Integer row;

    /** 失败原因 */
    @NotBlank(message = "失败原因不能为空")
    private String reason;

    /**
     * 创建基于图书ID的失败记录
     * @param bookId 失败的图书ID，不能为 null
     * @param reason 失败原因，不能为空
     * @return 失败记录实例
     * @throws IllegalArgumentException 如果参数无效
     */
    public static BatchOperationFailure ofBookId(Long bookId, String reason) {
        if (bookId == null) {
            throw new IllegalArgumentException("bookId 不能为 null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        return new BatchOperationFailure(bookId, null, reason);
    }

    /**
     * 创建基于行号的失败记录
     * @param row 失败的行号，不能为 null
     * @param reason 失败原因，不能为空
     * @return 失败记录实例
     * @throws IllegalArgumentException 如果参数无效
     */
    public static BatchOperationFailure ofRow(Integer row, String reason) {
        if (row == null) {
            throw new IllegalArgumentException("row 不能为 null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        return new BatchOperationFailure(null, row, reason);
    }
}
