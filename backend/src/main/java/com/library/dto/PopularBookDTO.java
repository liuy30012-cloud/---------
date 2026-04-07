package com.library.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularBookDTO {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private Long borrowCount;
    private String coverUrl;
}
