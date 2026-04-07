package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryStatisticsDTO {
    private String category;
    private Long totalBooks;
    private Long borrowedBooks;
    private Long availableBooks;
    private Double borrowRate;
}
