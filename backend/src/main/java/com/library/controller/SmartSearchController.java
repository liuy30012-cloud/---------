package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.model.Book;
import com.library.model.BookTag;
import com.library.repository.BookTagRepository;
import com.library.service.BookRecommendationService;
import com.library.service.SmartSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-search")
public class SmartSearchController {

    private final SmartSearchService smartSearchService;
    private final BookRecommendationService recommendationService;
    private final BookTagRepository bookTagRepository;

    public SmartSearchController(
        SmartSearchService smartSearchService,
        BookRecommendationService recommendationService,
        BookTagRepository bookTagRepository
    ) {
        this.smartSearchService = smartSearchService;
        this.recommendationService = recommendationService;
        this.bookTagRepository = bookTagRepository;
    }

    /**
     * 智能搜索 - 支持模糊匹配、拼写纠错和自然语言
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> smartSearch(
        @RequestParam(required = false, defaultValue = "") String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        SmartSearchService.SmartSearchResult result = smartSearchService.smartSearch(query, pageable);

        Map<String, Object> response = new HashMap<>();
        Page<Book> books = result.getBooks();

        response.put("books", books.getContent());
        response.put("total", books.getTotalElements());
        response.put("page", books.getNumber());
        response.put("totalPages", books.getTotalPages());
        response.put("originalQuery", result.getOriginalQuery());
        response.put("normalizedQuery", result.getNormalizedQuery());

        if (result.getDidYouMean() != null) {
            response.put("didYouMean", result.getDidYouMean());
        }

        if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
            response.put("suggestions", result.getSuggestions());
        }

        if (result.getInterpretation() != null) {
            response.put("interpretation", result.getInterpretation());
        }

        return ApiResponse.ok(response);
    }

    /**
     * 搜索建议（自动完成）
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
        @RequestParam String prefix,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<String> suggestions = smartSearchService.getSearchSuggestions(prefix, limit);
        return ApiResponse.ok(suggestions);
    }

    /**
     * 获取图书推荐
     */
    @GetMapping("/recommendations/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendations(
        @PathVariable Long bookId,
        @RequestParam(defaultValue = "6") int limit
    ) {
        Map<String, Object> recommendations = new HashMap<>();

        recommendations.put("similar", recommendationService.getContentBasedRecommendations(bookId, limit));
        recommendations.put("collaborative", recommendationService.getCollaborativeRecommendations(bookId, limit));
        recommendations.put("hybrid", recommendationService.getHybridRecommendations(bookId, limit));

        return ApiResponse.ok(recommendations);
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/tags/popular")
    public ResponseEntity<ApiResponse<List<String>>> getPopularTags() {
        List<String> tags = bookTagRepository.findPopularSystemTags();
        return ApiResponse.ok(tags);
    }

    /**
     * 按标签搜索图书
     */
    @GetMapping("/tags/{tagName}/books")
    public ResponseEntity<ApiResponse<List<Long>>> getBooksByTag(@PathVariable String tagName) {
        List<Long> bookIds = bookTagRepository.findBookIdsByTagName(tagName);
        return ApiResponse.ok(bookIds);
    }

    /**
     * 为图书添加标签
     */
    @PostMapping("/books/{bookId}/tags")
    public ResponseEntity<ApiResponse<BookTag>> addBookTag(
        @PathVariable Long bookId,
        @RequestParam String tagName,
        @RequestParam(defaultValue = "USER_GENERATED") String tagType
    ) {
        BookTag tag = new BookTag();
        tag.setBookId(bookId);
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        tag.setUsageCount(0);

        BookTag saved = bookTagRepository.save(tag);
        return ApiResponse.ok(saved);
    }

    /**
     * 获取图书的所有标签
     */
    @GetMapping("/books/{bookId}/tags")
    public ResponseEntity<ApiResponse<List<BookTag>>> getBookTags(@PathVariable Long bookId) {
        List<BookTag> tags = bookTagRepository.findByBookId(bookId);
        return ApiResponse.ok(tags);
    }
}
