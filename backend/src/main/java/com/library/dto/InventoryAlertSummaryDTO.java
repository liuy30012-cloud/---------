package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertSummaryDTO {
    private Integer totalAlerts;
    private Integer criticalAlerts;
    private Integer warningAlerts;
    private Integer outOfStockCount;
    private Integer lowStockCount;
    private Integer highDemandCount;
    private List<InventoryAlertDTO> alerts;
}
