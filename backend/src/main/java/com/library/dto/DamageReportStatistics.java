package com.library.dto;

import lombok.Data;

@Data
public class DamageReportStatistics {

    private long pendingCount;
    private long inProgressCount;
    private long resolvedCount;
    private long rejectedCount;
    private long totalCount;
}
