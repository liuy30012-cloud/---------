package com.library.dto;

import com.library.constant.AdminUserManagementMessageKeys;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {

    @NotNull(message = AdminUserManagementMessageKeys.STATUS_REQUIRED)
    @Min(value = 0, message = AdminUserManagementMessageKeys.STATUS_INVALID)
    @Max(value = 1, message = AdminUserManagementMessageKeys.STATUS_INVALID)
    private Integer status;
}
