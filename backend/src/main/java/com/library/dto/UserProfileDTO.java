package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private Long userId;
    private String studentId;
    private String username;
    private Long totalBorrows;
    private Long currentBorrows;
    private String favoriteCategory;
    private Double averageBorrowDays;
}
