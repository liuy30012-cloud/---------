package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BookDetailResponse;
import com.library.dto.BookReviewResponse;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowRecord.BorrowStatus;
import com.library.model.ReservationRecord.ReservationStatus;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRecordRepository;
import com.library.service.BookReviewService;
import com.library.service.BookService;
import com.library.service.BookSearchCacheService;
import com.library.service.borrow.BorrowValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final List<BorrowStatus> ACTIVE_BORROW_STATUSES = List.of(
        BorrowStatus.PENDING,
        BorrowStatus.APPROVED,
        BorrowStatus.BORROWED,
        BorrowStatus.OVERDUE
    );

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final BookReviewService bookReviewService;
    private final BookSearchCacheService bookSearchCacheService;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRecordRepository reservationRecordRepository;

    public BookController(
        BookService bookService,
        BookRepository bookRepository,
        BookReviewService bookReviewService,
        BookSearchCacheService bookSearchCacheService,
        BorrowRecordRepository borrowRecordRepository,
        ReservationRecordRepository reservationRecordRepository
    ) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.bookReviewService = bookReviewService;
        this.bookSearchCacheService = bookSearchCacheService;
        this.borrowRecordRepository = borrowRecordRepository;
        this.reservationRecordRepository = reservationRecordRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Book>>> searchBooks(
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false, defaultValue = "") String author,
        @RequestParam(required = false, defaultValue = "") String year,
        @RequestParam(required = false, defaultValue = "") String category,
        @RequestParam(required = false, defaultValue = "") String status,
        @RequestParam(required = false, defaultValue = "") String language,
        @RequestParam(required = false, defaultValue = "relevance") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, resolveSort(sort));

        // 使用缓存服务进行搜索（5分钟缓存）
        Page<Book> bookPage = bookSearchCacheService.searchBooks(
            keyword == null ? "" : keyword.trim(),
            author == null ? "" : author.trim(),
            year == null ? "" : year.trim(),
            category == null ? "" : category.trim(),
            language == null ? "" : language.trim(),
            status == null ? "" : status.trim(),
            normalizedPage,
            normalizedSize,
            pageable
        );

        return ApiResponse.okWithPagination(
            bookPage.getContent(),
            (int) bookPage.getTotalElements(),
            normalizedPage,
            normalizedSize,
            bookPage.getTotalPages()
        );
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<ApiResponse<List<Book>>> advancedSearch(
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false, defaultValue = "") String author,
        @RequestParam(required = false, defaultValue = "") String year,
        @RequestParam(required = false, defaultValue = "") String category,
        @RequestParam(required = false, defaultValue = "") String status,
        @RequestParam(required = false, defaultValue = "") String language,
        @RequestParam(required = false, defaultValue = "relevance") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return searchBooks(keyword, author, year, category, status, language, sort, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDetailResponse>> getBookDetail(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            return ApiResponse.notFound("Book does not exist.");
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

        var ratingStatistics = bookReviewService.getBookRatingStatistics(id);
        response.setAverageRating(ratingStatistics.getAverageRating());
        response.setTotalReviews(ratingStatistics.getTotalReviews());
        response.setLatestReviews(bookReviewService.getLatestReviews(id, 5));
        response.setBorrowHistorySummary(buildBorrowHistorySummary(id));
        response.setRelatedBooks(buildRelatedBooks(book));
        response.setAvailabilityContext(buildAvailabilityContext(book));
        response.setQueueContext(buildQueueContext(book));
        response.setLocationContext(buildLocationContext(book, response.getRelatedBooks()));

        return ApiResponse.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        // 使用缓存服务（1小时缓存）
        return ApiResponse.ok(bookSearchCacheService.getCategoriesWithCache());
    }

    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<List<String>>> getLanguages() {
        // 使用缓存服务（1小时缓存）
        return ApiResponse.ok(bookSearchCacheService.getLanguagesWithCache());
    }

    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort) || "relevance".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Order.desc("borrowedCount"), Sort.Order.asc("title"));
        }
        return switch (sort) {
            case "title_asc" -> Sort.by(Sort.Order.asc("title"));
            case "title_desc" -> Sort.by(Sort.Order.desc("title"));
            case "year_desc" -> Sort.by(Sort.Order.desc("year"), Sort.Order.asc("title"));
            case "year_asc" -> Sort.by(Sort.Order.asc("year"), Sort.Order.asc("title"));
            case "popular" -> Sort.by(Sort.Order.desc("borrowedCount"), Sort.Order.asc("title"));
            case "availability" -> Sort.by(Sort.Order.desc("availableCopies"), Sort.Order.asc("title"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("borrowedCount"), Sort.Order.asc("title"));
        };
    }

    private BookDetailResponse.BorrowHistorySummary buildBorrowHistorySummary(Long bookId) {
        BookDetailResponse.BorrowHistorySummary summary = new BookDetailResponse.BorrowHistorySummary();
        summary.setTotalBorrows(borrowRecordRepository.countByBookId(bookId));
        summary.setActiveBorrowCount(borrowRecordRepository.countByBookIdAndStatusIn(bookId, ACTIVE_BORROW_STATUSES));

        List<BorrowRecord> recentRecords = borrowRecordRepository.findTop5ByBookIdOrderByCreatedAtDesc(bookId);
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
            ? bookRepository.findTop6ByCategoryAndIdNotOrderByBorrowedCountDesc(book.getCategory(), book.getId())
            : new ArrayList<>();

        if (candidates.isEmpty() && StringUtils.hasText(book.getAuthor())) {
            candidates = bookRepository.findTop6ByAuthorAndIdNotOrderByBorrowedCountDesc(book.getAuthor(), book.getId());
        }

        List<BookDetailResponse.RelatedBookSummary> relatedBooks = new ArrayList<>();
        for (Book candidate : candidates.stream().limit(6).toList()) {
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
            context.setSummary("This copy is for reading room use only.");
            context.setNextAction("READ_IN_LIBRARY");
        } else if (canBorrow) {
            context.setSummary("Copies are available now. Auto-approved requests can move straight to pickup.");
            context.setNextAction("BORROW_NOW");
        } else {
            context.setSummary("Copies are currently checked out. You can join the reservation queue.");
            context.setNextAction("RESERVE");
        }
        return context;
    }

    private BookDetailResponse.QueueContext buildQueueContext(Book book) {
        BookDetailResponse.QueueContext context = new BookDetailResponse.QueueContext();
        long waitingCount = reservationRecordRepository.countWaitingReservationsByBookId(book.getId());
        long availableReservationCount = reservationRecordRepository.countByBookIdAndStatus(book.getId(), ReservationStatus.AVAILABLE);
        int copies = Math.max(book.getTotalCopies() == null ? 1 : book.getTotalCopies(), 1);
        int estimatedWaitDays = waitingCount == 0 ? 0 : (int) Math.ceil((double) waitingCount * BorrowValidator.DEFAULT_BORROW_DAYS / copies);

        context.setWaitingCount(waitingCount);
        context.setAvailableReservationCount(availableReservationCount);
        context.setEstimatedWaitDays(estimatedWaitDays);
        if (waitingCount > 0) {
            context.setSummary("There are " + waitingCount + " readers waiting. Estimated wait: about " + estimatedWaitDays + " day(s).");
        } else if (availableReservationCount > 0) {
            context.setSummary("Reserved copies are waiting for pickup.");
        } else {
            context.setSummary("No active reservation queue at the moment.");
        }
        return context;
    }

    private BookDetailResponse.LocationContext buildLocationContext(
        Book book,
        List<BookDetailResponse.RelatedBookSummary> relatedBooks
    ) {
        BookDetailResponse.LocationContext context = new BookDetailResponse.LocationContext();
        context.setBreadcrumbs(splitLocation(book.getLocation()));
        context.setPickupCardTitle("Pickup route");
        context.setPickupHint(buildPickupHint(book));
        if (!relatedBooks.isEmpty()) {
            context.setAdjacentRecommendation("Nearby shelf suggestion: " + relatedBooks.get(0).getTitle());
        }
        return context;
    }

    private List<String> splitLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return List.of("Location pending");
        }

        String normalized = location
            .replace("->", "|")
            .replace("/", "|")
            .replace(">", "|")
            .replace("-", "|");
        return java.util.Arrays.stream(normalized.split("\\|"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String buildPickupHint(Book book) {
        if (book.getCirculationPolicy() == Book.CirculationPolicy.REFERENCE_ONLY) {
            return "Ask the service desk for in-library reading support.";
        }
        if (book.getAvailableCopies() != null && book.getAvailableCopies() > 0) {
            return "Take the breadcrumb route above to the shelf, then follow the pickup action shown on this page.";
        }
        return "Copies are currently out. Place a reservation and we will notify you when pickup is available.";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@Valid @RequestBody com.library.dto.CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setLocation(request.getLocation());
        book.setCoverUrl(request.getCoverUrl());
        book.setStatus(request.getStatus());
        book.setYear(request.getYear());
        book.setDescription(request.getDescription());
        book.setLanguageCode(request.getLanguageCode());
        book.setAvailability(request.getAvailability());
        book.setCategory(request.getCategory());
        book.setCirculationPolicy(request.getCirculationPolicy());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setBorrowedCount(0);

        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            return ApiResponse.error("ISBN '" + request.getIsbn() + "' 已存在", 409);
        }

        bookService.validateBook(book);
        Book createdBook = bookService.createBook(book);
        return ApiResponse.ok(createdBook);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(
        @PathVariable Long id,
        @Valid @RequestBody com.library.dto.UpdateBookRequest request
    ) {
        Book existingBook = bookService.getBookById(id);
        if (existingBook == null) {
            return ApiResponse.notFound("图书不存在");
        }

        java.util.Optional<Book> duplicateIsbn = bookRepository.findByIsbn(request.getIsbn());
        if (duplicateIsbn.isPresent() && !duplicateIsbn.get().getId().equals(id)) {
            return ApiResponse.error("ISBN '" + request.getIsbn() + "' 已被其他图书使用", 409);
        }

        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setLocation(request.getLocation());
        existingBook.setCoverUrl(request.getCoverUrl());
        existingBook.setStatus(request.getStatus());
        existingBook.setYear(request.getYear());
        existingBook.setDescription(request.getDescription());
        existingBook.setLanguageCode(request.getLanguageCode());
        existingBook.setAvailability(request.getAvailability());
        existingBook.setCategory(request.getCategory());

        if (request.getCirculationPolicy() != null) {
            existingBook.setCirculationPolicy(request.getCirculationPolicy());
        }

        if (request.getTotalCopies() != null) {
            existingBook.setTotalCopies(request.getTotalCopies());
            bookService.adjustCopiesOnUpdate(existingBook, existingBook);
        }

        bookService.validateBook(existingBook);
        Book updatedBook = bookService.updateBook(existingBook);
        return ApiResponse.ok(updatedBook);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            return ApiResponse.notFound("图书不存在");
        }

        try {
            bookService.checkRelatedData(id);
            bookService.deleteBook(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 409);
        }
    }
}
