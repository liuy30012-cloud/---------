package com.library.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookDetailResponse {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String location;
    private String coverUrl;
    private String status;
    private String year;
    private String description;
    private String languageCode;
    private String availability;
    private String category;
    private Integer totalCopies;
    private Integer availableCopies;
    private Integer borrowedCount;
    private String circulationPolicy;
    private Double averageRating;
    private Long totalReviews;
    private List<BookReviewResponse> latestReviews = new ArrayList<>();
    private BorrowHistorySummary borrowHistorySummary;
    private List<RelatedBookSummary> relatedBooks = new ArrayList<>();
    private AvailabilityContext availabilityContext;
    private QueueContext queueContext;
    private LocationContext locationContext;

    @Data
    public static class BorrowHistorySummary {
        private Long totalBorrows;
        private Long activeBorrowCount;
        private LocalDateTime lastBorrowedAt;
        private List<BorrowTimelineItem> recentActivity = new ArrayList<>();
    }

    @Data
    public static class BorrowTimelineItem {
        private Long id;
        private String status;
        private LocalDateTime borrowDate;
        private LocalDateTime dueDate;
        private LocalDateTime returnDate;
    }

    @Data
    public static class RelatedBookSummary {
        private Long id;
        private String title;
        private String author;
        private String coverUrl;
        private String location;
        private Integer availableCopies;
        private String circulationPolicy;
    }

    @Data
    public static class AvailabilityContext {
        private boolean canBorrow;
        private boolean canReserve;
        private String state;
        private String summary;
        private String nextAction;
        private Integer availableCopies;
        private Integer totalCopies;
    }

    @Data
    public static class QueueContext {
        private Long waitingCount;
        private Long availableReservationCount;
        private Integer estimatedWaitDays;
        private String summary;
    }

    @Data
    public static class LocationContext {
        private List<String> breadcrumbs = new ArrayList<>();
        private String pickupCardTitle;
        private String pickupHint;
        private String adjacentRecommendation;
    }
}
