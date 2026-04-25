package com.library.service;

import com.library.dto.BookDetailResponse;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.service.borrow.BorrowValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BookDetailFacade {

    private static final int RELATED_BOOK_LIMIT = 6;
    private static final int RECENT_BORROW_LIMIT = 5;

    private final BookService bookService;
    private final BookReviewService bookReviewService;

    public BookDetailFacade(BookService bookService, BookReviewService bookReviewService) {
        this.bookService = bookService;
        this.bookReviewService = bookReviewService;
    }

    public BookDetailResponse getBookDetail(Long bookId) {
        Book book = bookService.getBookById(bookId);
        if (book == null) {
            return null;
        }

        BookDetailResponse response = new BookDetailResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setIsbn(book.getIsbn());
        response.setLocation(book.getLocation());
        response.setCoverUrl(book.getCoverUrl());
        response.setStatus(book.getStatus());
        response.setYear(book.getYear());
        response.setDescription(book.getDescription());
        response.setLanguageCode(book.getLanguageCode());
        response.setAvailability(book.getAvailability());
        response.setCategory(book.getCategory());
        response.setTotalCopies(book.getTotalCopies());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setBorrowedCount(book.getBorrowedCount());
        response.setCirculationPolicy(book.getCirculationPolicy().name());

        var ratingStatistics = bookReviewService.getBookRatingStatistics(bookId);
        response.setAverageRating(ratingStatistics.getAverageRating());
        response.setTotalReviews(ratingStatistics.getTotalReviews());
        response.setLatestReviews(bookReviewService.getLatestReviews(bookId, 5));
        response.setBorrowHistorySummary(buildBorrowHistorySummary(bookId));
        response.setRelatedBooks(buildRelatedBooks(book));
        response.setAvailabilityContext(buildAvailabilityContext(book));
        response.setQueueContext(buildQueueContext(book));
        response.setLocationContext(buildLocationContext(book, response.getRelatedBooks()));
        return response;
    }

    private BookDetailResponse.BorrowHistorySummary buildBorrowHistorySummary(Long bookId) {
        BookDetailResponse.BorrowHistorySummary summary = new BookDetailResponse.BorrowHistorySummary();
        summary.setTotalBorrows(bookService.countTotalBorrowsByBookId(bookId));
        summary.setActiveBorrowCount(bookService.countActiveBorrowsByBookId(bookId, BorrowRecord.ACTIVE_STATUSES));

        List<BorrowRecord> recentRecords = bookService.findRecentBorrowRecords(bookId, RECENT_BORROW_LIMIT);
        if (!recentRecords.isEmpty()) {
            LocalDateTime lastBorrowedAt = recentRecords.stream()
                .map(record -> record.getBorrowDate() != null ? record.getBorrowDate() : record.getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
            summary.setLastBorrowedAt(lastBorrowedAt);
        }

        List<BookDetailResponse.BorrowTimelineItem> timeline = new ArrayList<>();
        for (BorrowRecord record : recentRecords) {
            BookDetailResponse.BorrowTimelineItem item = new BookDetailResponse.BorrowTimelineItem();
            item.setId(record.getId());
            item.setStatus(record.getStatus().name());
            item.setBorrowDate(record.getBorrowDate());
            item.setDueDate(record.getDueDate());
            item.setReturnDate(record.getReturnDate());
            timeline.add(item);
        }
        summary.setRecentActivity(timeline);
        return summary;
    }

    private List<BookDetailResponse.RelatedBookSummary> buildRelatedBooks(Book book) {
        List<Book> candidates = StringUtils.hasText(book.getCategory())
            ? bookService.findTopBooksByCategoryExcluding(book.getCategory(), book.getId(), RELATED_BOOK_LIMIT)
            : new ArrayList<>();

        if (candidates.isEmpty() && StringUtils.hasText(book.getAuthor())) {
            candidates = bookService.findTopBooksByAuthorExcluding(book.getAuthor(), book.getId(), RELATED_BOOK_LIMIT);
        }

        List<BookDetailResponse.RelatedBookSummary> relatedBooks = new ArrayList<>();
        for (Book candidate : candidates.stream().limit(RELATED_BOOK_LIMIT).toList()) {
            BookDetailResponse.RelatedBookSummary item = new BookDetailResponse.RelatedBookSummary();
            item.setId(candidate.getId());
            item.setTitle(candidate.getTitle());
            item.setAuthor(candidate.getAuthor());
            item.setCoverUrl(candidate.getCoverUrl());
            item.setLocation(candidate.getLocation());
            item.setAvailableCopies(candidate.getAvailableCopies());
            item.setCirculationPolicy(candidate.getCirculationPolicy().name());
            relatedBooks.add(item);
        }
        return relatedBooks;
    }

    private BookDetailResponse.AvailabilityContext buildAvailabilityContext(Book book) {
        BookDetailResponse.AvailabilityContext context = new BookDetailResponse.AvailabilityContext();
        boolean canBorrow = book.getCirculationPolicy() != Book.CirculationPolicy.REFERENCE_ONLY
            && book.getAvailableCopies() != null
            && book.getAvailableCopies() > 0;

        boolean canReserve = book.getCirculationPolicy() != Book.CirculationPolicy.REFERENCE_ONLY
            && (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0);

        context.setCanBorrow(canBorrow);
        context.setCanReserve(canReserve);
        context.setAvailableCopies(book.getAvailableCopies());
        context.setTotalCopies(book.getTotalCopies());
        context.setState(canBorrow ? "AVAILABLE" : canReserve ? "WAITLIST" : "READING_ROOM_ONLY");

        if (book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY) {
            context.setSummary("此馆藏仅限馆内阅览。");
            context.setNextAction("READ_IN_LIBRARY");
        } else if (canBorrow) {
            context.setSummary("当前有可借副本。自动审批通过后可直接到馆取书。");
            context.setNextAction("BORROW_NOW");
        } else {
            context.setSummary("副本当前均已借出，你可以加入预约队列。");
            context.setNextAction("RESERVE");
        }
        return context;
    }

    private BookDetailResponse.QueueContext buildQueueContext(Book book) {
        BookDetailResponse.QueueContext context = new BookDetailResponse.QueueContext();
        long waitingCount = bookService.countWaitingReservationsByBookId(book.getId());
        long availableReservationCount = bookService.countAvailableReservationsByBookId(book.getId());
        int copies = Math.max(book.getTotalCopies() == null ? 1 : book.getTotalCopies(), 1);
        int estimatedWaitDays = waitingCount == 0
            ? 0
            : (int) Math.ceil((double) waitingCount * BorrowValidator.DEFAULT_BORROW_DAYS / copies);

        context.setWaitingCount(waitingCount);
        context.setAvailableReservationCount(availableReservationCount);
        context.setEstimatedWaitDays(estimatedWaitDays);
        if (waitingCount > 0) {
            context.setSummary("当前有 " + waitingCount + " 位读者等待，预计等待约 " + estimatedWaitDays + " 天。");
        } else if (availableReservationCount > 0) {
            context.setSummary("已有预约副本等待取书。");
        } else {
            context.setSummary("当前暂无活跃预约队列。");
        }
        return context;
    }

    private BookDetailResponse.LocationContext buildLocationContext(
        Book book,
        List<BookDetailResponse.RelatedBookSummary> relatedBooks
    ) {
        BookDetailResponse.LocationContext context = new BookDetailResponse.LocationContext();
        context.setBreadcrumbs(splitLocation(book.getLocation()));
        context.setPickupCardTitle("取书路线");
        context.setPickupHint(buildPickupHint(book));
        if (!relatedBooks.isEmpty()) {
            context.setAdjacentRecommendation("附近书架推荐：" + relatedBooks.get(0).getTitle());
        }
        return context;
    }

    private List<String> splitLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return List.of("位置待分配");
        }

        String normalized = location
            .replace("->", "|")
            .replace("/", "|")
            .replace(">", "|")
            .replace("-", "|");
        return Arrays.stream(normalized.split("\\|"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String buildPickupHint(Book book) {
        if (book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY) {
            return "请到服务台咨询馆内阅览安排。";
        }
        if (book.getAvailableCopies() != null && book.getAvailableCopies() > 0) {
            return "请按上方位置路径前往对应书架，并根据本页提示完成取书。";
        }
        return "当前副本均已借出。提交预约后，可取书时系统会通知你。";
    }
}
