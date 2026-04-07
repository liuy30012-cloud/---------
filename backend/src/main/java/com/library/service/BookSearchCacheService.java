package com.library.service;

import com.library.model.Book;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 书籍搜索缓存服务
 * 使用Redis缓存搜索结果，减少数据库压力
 */
@Service
@RequiredArgsConstructor
public class BookSearchCacheService {

    private final BookRepository bookRepository;

    /**
     * 缓存书籍搜索结果
     * 缓存key: book_search::{keyword}::{author}::{year}::{category}::{language}::{status}::{page}::{size}
     */
    @Cacheable(
        value = "book_search",
        key = "#keyword + ':' + #author + ':' + #year + ':' + #category + ':' + #language + ':' + #status + ':' + #page + ':' + #size",
        unless = "#result == null || #result.isEmpty()"
    )
    public Page<Book> searchBooks(
        String keyword,
        String author,
        String year,
        String category,
        String language,
        String status,
        int page,
        int size,
        Pageable pageable
    ) {
        return bookRepository.searchCatalog(keyword, author, year, category, language, status, pageable);
    }

    /**
     * 获取分类列表（缓存1小时）
     */
    @Cacheable(value = "book_categories", key = "'all'")
    public List<String> getCategoriesWithCache() {
        return bookRepository.findDistinctCategories();
    }

    /**
     * 获取语言列表（缓存1小时）
     */
    @Cacheable(value = "book_languages", key = "'all'")
    public List<String> getLanguagesWithCache() {
        return bookRepository.findDistinctLanguages();
    }

    /**
     * 获取热门书籍（缓存10分钟）
     */
    @Cacheable(
        value = "popular_books",
        key = "#limit",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Book> getPopularBooks(int limit, Pageable pageable) {
        return bookRepository.searchCatalog("", "", "", "", "", "", pageable)
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * 清除搜索缓存（书籍更新时调用）
     */
    @CacheEvict(value = "book_search", allEntries = true)
    public void clearSearchCache() {
        // 清除所有搜索结果缓存
    }

    /**
     * 清除分类/语言缓存（分类数据更新时调用）
     */
    @CacheEvict(value = {"book_categories", "book_languages", "popular_books"}, allEntries = true)
    public void clearCategoryCache() {
        // 清除分类、语言、热门书籍缓存
    }
}
