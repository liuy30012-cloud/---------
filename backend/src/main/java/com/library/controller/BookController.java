package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BookDetailResponse;
import com.library.model.Book;
import com.library.service.BookDetailFacade;
import com.library.service.BookSearchCacheService;
import com.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final BookDetailFacade bookDetailFacade;
    private final BookSearchCacheService bookSearchCacheService;

    public BookController(
        BookService bookService,
        BookDetailFacade bookDetailFacade,
        BookSearchCacheService bookSearchCacheService
    ) {
        this.bookService = bookService;
        this.bookDetailFacade = bookDetailFacade;
        this.bookSearchCacheService = bookSearchCacheService;
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
        BookDetailResponse response = bookDetailFacade.getBookDetail(id);
        if (response == null) {
            return ApiResponse.notFound("Book does not exist.");
        }
        return ApiResponse.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ApiResponse.ok(bookSearchCacheService.getCategoriesWithCache());
    }

    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<List<String>>> getLanguages() {
        return ApiResponse.ok(bookSearchCacheService.getLanguagesWithCache());
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank() || "relevance".equalsIgnoreCase(sort)) {
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

        if (bookService.findByIsbn(request.getIsbn()).isPresent()) {
            return ApiResponse.error("ISBN '" + request.getIsbn() + "' 已存在。", 409);
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
            return ApiResponse.notFound("图书不存在。");
        }

        if (bookService.isIsbnUsedByOther(request.getIsbn(), id)) {
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
            Book bookWithNewCopies = new Book();
            bookWithNewCopies.setTotalCopies(request.getTotalCopies());
            bookService.adjustCopiesOnUpdate(existingBook, bookWithNewCopies);
            existingBook.setTotalCopies(bookWithNewCopies.getTotalCopies());
            existingBook.setAvailableCopies(bookWithNewCopies.getAvailableCopies());
            existingBook.setBorrowedCount(bookWithNewCopies.getBorrowedCount());
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
            return ApiResponse.notFound("图书不存在。");
        }

        try {
            bookService.checkRelatedData(id);
            bookService.deleteBook(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 409);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<com.library.dto.BatchDeleteResponse>> batchDeleteBooks(
        @Valid @RequestBody com.library.dto.BatchDeleteRequest request
    ) {
        com.library.dto.BatchDeleteResponse response = bookService.batchDeleteBooks(request.getBookIds());
        return ApiResponse.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<com.library.dto.ImportResponse>> importBooks(
        @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ApiResponse.error("文件不能为空", 400);
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ApiResponse.error("文件名无效", 400);
        }

        try {
            List<Book> books;
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                books = com.library.util.BookFileParser.parseExcel(file);
            } else if (filename.endsWith(".csv")) {
                books = com.library.util.BookFileParser.parseCsv(file);
            } else {
                return ApiResponse.error("不支持的文件格式，仅支持 .xlsx、.xls、.csv", 400);
            }

            com.library.dto.ImportResponse response = bookService.batchCreateBooks(books);
            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 400);
        } catch (com.opencsv.exceptions.CsvException e) {
            return ApiResponse.error("CSV 文件格式错误: " + e.getMessage(), 400);
        } catch (java.io.IOException e) {
            return ApiResponse.error("文件解析失败: " + e.getMessage(), 400);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/import/template")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTemplate() {
        try {
            byte[] templateBytes = com.library.util.BookFileParser.generateTemplate();
            org.springframework.core.io.ByteArrayResource resource =
                new org.springframework.core.io.ByteArrayResource(templateBytes);

            return ResponseEntity.ok()
                .header(
                    org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=book_import_template.xlsx"
                )
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(templateBytes.length)
                .body(resource);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
