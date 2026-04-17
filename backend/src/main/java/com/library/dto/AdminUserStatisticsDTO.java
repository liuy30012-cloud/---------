package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatisticsDTO {
    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;
    private long activeAdmins;
}
