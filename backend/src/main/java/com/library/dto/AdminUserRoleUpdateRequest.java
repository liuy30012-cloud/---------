package com.library.dto;

import com.library.constant.AdminUserManagementMessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserRoleUpdateRequest {

    @NotBlank(message = AdminUserManagementMessageKeys.ROLE_REQUIRED)
    @Pattern(regexp = "STUDENT|TEACHER|ADMIN", message = AdminUserManagementMessageKeys.ROLE_INVALID)
    private String role;
}
