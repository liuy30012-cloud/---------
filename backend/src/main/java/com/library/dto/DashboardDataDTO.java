package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDataDTO {
    private InventoryStatisticsDTO inventory;
    private List<PopularBookDTO> popularBooks;
    private List<BorrowTrendDTO> borrowTrends;
    private List<CategoryStatisticsDTO> categoryStatistics;
    private Long totalUsers;
    private Long activeUsers;
}
