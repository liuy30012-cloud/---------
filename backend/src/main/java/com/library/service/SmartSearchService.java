package com.library.service;

import com.library.model.Book;
import com.library.model.SearchSuggestion;
import com.library.repository.BookRepository;
import com.library.repository.SearchSuggestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SmartSearchService {

    private final BookRepository bookRepository;
    private final SearchSuggestionRepository suggestionRepository;
    private final FuzzySearchService fuzzySearchService;

    // 自然语言搜索关键词映射
    private static final Map<String, String> INTENT_KEYWORDS = Map.ofEntries(
        Map.entry("入门", "category:入门"),
        Map.entry("初学", "category:入门"),
        Map.entry("基础", "category:基础"),
        Map.entry("进阶", "category:进阶"),
        Map.entry("高级", "category:高级"),
        Map.entry("考研", "tag:考研"),
        Map.entry("大一", "tag:大一新生"),
        Map.entry("新生", "tag:新生"),
        Map.entry("必读", "tag:必读"),
        Map.entry("经典", "tag:经典"),
        Map.entry("畅销", "sort:popular")
    );

    public SmartSearchService(
        BookRepository bookRepository,
        SearchSuggestionRepository suggestionRepository,
        FuzzySearchService fuzzySearchService
    ) {
        this.bookRepository = bookRepository;
        this.suggestionRepository = suggestionRepository;
        this.fuzzySearchService = fuzzySearchService;
    }

    /**
     * 智能搜索 - 支持模糊匹配、拼写纠错和自然语言
     */
    @Transactional
    public SmartSearchResult smartSearch(String query, Pageable pageable) {
        SmartSearchResult result = new SmartSearchResult();

        if (query == null || query.trim().isEmpty()) {
            result.setBooks(Page.empty(pageable));
            return result;
        }

        String normalizedQuery = normalizeQuery(query);

        // 1. 尝试直接搜索
        Page<Book> directResults = bookRepository.searchCatalog(
            normalizedQuery, "", "", "", "", "", pageable
        );

        // 2. 如果结果少于3个，尝试模糊搜索
        if (directResults.getTotalElements() < 3) {
            List<String> allTitles = bookRepository.findAll().stream()
                .map(Book::getTitle)
                .collect(Collectors.toList());

            List<FuzzySearchService.SimilarityMatch> similarTitles =
                fuzzySearchService.findSimilarMatches(normalizedQuery, allTitles);

            if (!similarTitles.isEmpty()) {
                result.setSuggestions(similarTitles.stream()
                    .map(FuzzySearchService.SimilarityMatch::text)
                    .limit(5)
                    .collect(Collectors.toList()));
                result.setDidYouMean(similarTitles.get(0).text());
            }
        }

        // 3. 解析自然语言查询
        NaturalLanguageQuery nlQuery = parseNaturalLanguage(query);
        if (nlQuery.hasIntent()) {
            result.setInterpretation(nlQuery.getInterpretation());
            // 使用解析后的参数重新搜索
            directResults = bookRepository.searchCatalog(
                nlQuery.getKeyword(),
                nlQuery.getAuthor(),
                "",
                nlQuery.getCategory(),
                "",
                "",
                pageable
            );
        }

        result.setBooks(directResults);
        result.setOriginalQuery(query);
        result.setNormalizedQuery(normalizedQuery);

        // 4. 记录搜索历史用于改进建议
        recordSearchQuery(normalizedQuery, (int) directResults.getTotalElements());

        return result;
    }

    /**
     * 获取搜索建议（自动完成）
     */
    public List<String> getSearchSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.length() < 2) {
            return Collections.emptyList();
        }

        String normalized = normalizeQuery(prefix);

        // 从历史搜索中获取建议
        List<SearchSuggestion> historySuggestions =
            suggestionRepository.findByQueryStartingWithOrderByFrequencyDesc(
                normalized,
                PageRequest.of(0, Math.max(limit, 1))
            );

        List<String> suggestions = historySuggestions.stream()
            .map(SearchSuggestion::getQuery)
            .collect(Collectors.toList());

        // 如果历史建议不足，从书名中补充
        if (suggestions.size() < limit) {
            List<Book> books = bookRepository.findAll();
            List<String> titleSuggestions = books.stream()
                .map(Book::getTitle)
                .filter(title -> title.toLowerCase().contains(normalized.toLowerCase()))
                .limit(limit - suggestions.size())
                .collect(Collectors.toList());
            suggestions.addAll(titleSuggestions);
        }

        return suggestions;
    }

    /**
     * 解析自然语言查询
     */
    private NaturalLanguageQuery parseNaturalLanguage(String query) {
        NaturalLanguageQuery nlQuery = new NaturalLanguageQuery();
        String remaining = query;
        List<String> interpretations = new ArrayList<>();

        // 检测意图关键词
        for (Map.Entry<String, String> entry : INTENT_KEYWORDS.entrySet()) {
            if (query.contains(entry.getKey())) {
                String[] parts = entry.getValue().split(":");
                if (parts.length == 2) {
                    switch (parts[0]) {
                        case "category" -> {
                            nlQuery.setCategory(parts[1]);
                            interpretations.add("分类: " + parts[1]);
                        }
                        case "tag" -> {
                            nlQuery.addTag(parts[1]);
                            interpretations.add("标签: " + parts[1]);
                        }
                        case "sort" -> {
                            nlQuery.setSort(parts[1]);
                            interpretations.add("排序: 热门优先");
                        }
                    }
                }
                remaining = remaining.replace(entry.getKey(), "").trim();
            }
        }

        // 提取作者（"XXX的书"、"XXX写的"）
        Pattern authorPattern = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z]+)(的书|写的|著)");
        Matcher authorMatcher = authorPattern.matcher(query);
        if (authorMatcher.find()) {
            String author = authorMatcher.group(1);
            nlQuery.setAuthor(author);
            interpretations.add("作者: " + author);
            remaining = remaining.replaceAll(authorPattern.pattern(), "").trim();
        }

        // 剩余部分作为关键词
        if (!remaining.isEmpty()) {
            nlQuery.setKeyword(remaining);
            interpretations.add("关键词: " + remaining);
        }

        if (!interpretations.isEmpty()) {
            nlQuery.setInterpretation("理解为: " + String.join(", ", interpretations));
        }

        return nlQuery;
    }

    /**
     * 标准化查询字符串
     */
    private String normalizeQuery(String query) {
        return query.trim()
            .toLowerCase()
            .replaceAll("\\s+", " ");
    }

    /**
     * 记录搜索查询
     */
    private void recordSearchQuery(String query, int resultCount) {
        try {
            Optional<SearchSuggestion> existing = suggestionRepository.findByQuery(query);
            if (existing.isPresent()) {
                SearchSuggestion suggestion = existing.get();
                suggestion.setFrequency(suggestion.getFrequency() + 1);
                suggestion.setResultCount(resultCount);
                suggestionRepository.save(suggestion);
            } else {
                SearchSuggestion newSuggestion = new SearchSuggestion();
                newSuggestion.setQuery(query);
                newSuggestion.setNormalizedQuery(normalizeQuery(query));
                newSuggestion.setFrequency(1);
                newSuggestion.setResultCount(resultCount);
                suggestionRepository.save(newSuggestion);
            }
        } catch (Exception e) {
            // 记录失败不影响搜索结果
        }
    }

    // 内部类：自然语言查询解析结果
    private static class NaturalLanguageQuery {
        private String keyword = "";
        private String author = "";
        private String category = "";
        private String sort = "";
        private List<String> tags = new ArrayList<>();
        private String interpretation = "";

        public boolean hasIntent() {
            return !author.isEmpty() || !category.isEmpty() || !sort.isEmpty() || !tags.isEmpty();
        }

        // Getters and setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
        public List<String> getTags() { return tags; }
        public void addTag(String tag) { this.tags.add(tag); }
        public String getInterpretation() { return interpretation; }
        public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    }

    // 搜索结果包装类
    public static class SmartSearchResult {
        private Page<Book> books;
        private String originalQuery;
        private String normalizedQuery;
        private String didYouMean;
        private List<String> suggestions = new ArrayList<>();
        private String interpretation;

        // Getters and setters
        public Page<Book> getBooks() { return books; }
        public void setBooks(Page<Book> books) { this.books = books; }
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        public String getNormalizedQuery() { return normalizedQuery; }
        public void setNormalizedQuery(String normalizedQuery) { this.normalizedQuery = normalizedQuery; }
        public String getDidYouMean() { return didYouMean; }
        public void setDidYouMean(String didYouMean) { this.didYouMean = didYouMean; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        public String getInterpretation() { return interpretation; }
        public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    }
}
