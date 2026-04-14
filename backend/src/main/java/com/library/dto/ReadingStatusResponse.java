package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingStatusResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String author;
    private String coverUrl;
    private String category;
    private String status;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
