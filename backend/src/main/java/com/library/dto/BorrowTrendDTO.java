package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowTrendDTO {
    private String date;
    private Long borrowCount;
    private Long returnCount;
}
