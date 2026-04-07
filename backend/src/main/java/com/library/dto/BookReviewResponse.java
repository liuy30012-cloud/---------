package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewResponse {

    private Long id;
    private Long bookId;
    private Long userId;
    private String username;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
