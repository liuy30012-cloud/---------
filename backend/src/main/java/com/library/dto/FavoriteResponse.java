package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String author;
    private String coverUrl;
    private String category;
    private Integer availableCopies;
    private String readingStatus;
    private String notes;
    private LocalDateTime favoritedAt;
}
