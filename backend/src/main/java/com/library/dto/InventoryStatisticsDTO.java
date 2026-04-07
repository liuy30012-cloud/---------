package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryStatisticsDTO {
    private Long totalBooks;
    private Long availableBooks;
    private Long borrowedBooks;
    private Long overdueBooks;
    private Long reservedBooks;
    private Double utilizationRate;
}
