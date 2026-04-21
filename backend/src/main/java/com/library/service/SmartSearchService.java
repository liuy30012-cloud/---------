package com.library.service;

import com.library.document.BookDocument;
import com.library.model.Book;
import com.library.model.SearchSuggestion;
import com.library.repository.BookRepository;
import com.library.repository.SearchSuggestionRepository;
import com.library.service.elasticsearch.ElasticsearchSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmartSearchService {

    private final BookRepository bookRepository;
    private final SearchSuggestionRepository suggestionRepository;
    private final FuzzySearchService fuzzySearchService;
    private final ElasticsearchSearchService elasticsearchSearchService;

    @Value("${library.search.elasticsearch.enabled:true}")
    private boolean elasticsearchEnabled;

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
        FuzzySearchService fuzzySearchService,
        @Autowired(required = false) ElasticsearchSearchService elasticsearchSearchService
    ) {
        this.bookRepository = bookRepository;
        this.suggestionRepository = suggestionRepository;
        this.fuzzySearchService = fuzzySearchService;
        this.elasticsearchSearchService = elasticsearchSearchService;
    }

    @Transactional
    public SmartSearchResult smartSearch(String query, Pageable pageable) {
        SmartSearchResult result = new SmartSearchResult();

        if (!StringUtils.hasText(query)) {
            result.setBooks(Page.empty(pageable));
            result.setSearchEngine(SearchEngine.MYSQL_FALLBACK);
            return result;
        }

        String normalizedQuery = normalizeQuery(query);
        NaturalLanguageQuery nlQuery = parseNaturalLanguage(query);

        result.setOriginalQuery(query);
        result.setNormalizedQuery(normalizedQuery);
        if (StringUtils.hasText(nlQuery.getInterpretation())) {
            result.setInterpretation(nlQuery.getInterpretation());
        }

        Page<Book> primaryResults = runPrimarySearch(normalizedQuery, pageable, nlQuery, result);
        result.setBooks(primaryResults);

        if (primaryResults.getTotalElements() < 3) {
            populateSuggestions(normalizedQuery, result);
        }

        recordSearchQuery(normalizedQuery, (int) primaryResults.getTotalElements());
        return result;
    }

    public List<String> getSearchSuggestions(String prefix, int limit) {
        if (!StringUtils.hasText(prefix) || prefix.trim().length() < 2) {
            return Collections.emptyList();
        }

        String normalized = normalizeQuery(prefix);
        int safeLimit = Math.max(limit, 1);

        if (elasticsearchEnabled && elasticsearchSearchService != null) {
            try {
                List<String> esSuggestions = elasticsearchSearchService.getSuggestions(normalized, safeLimit);
                if (!esSuggestions.isEmpty()) {
                    log.debug("Using Elasticsearch suggestions for prefix {}", prefix);
                    return esSuggestions;
                }
            } catch (Exception e) {
                log.warn("Elasticsearch suggestion query failed, falling back to history: {}", e.getMessage());
            }
        }

        return suggestionRepository.findByQueryStartingWithOrderByFrequencyDesc(
                normalized,
                PageRequest.of(0, safeLimit)
            ).stream()
            .map(SearchSuggestion::getQuery)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .limit(safeLimit)
            .collect(Collectors.toList());
    }

    private Page<Book> runPrimarySearch(
        String normalizedQuery,
        Pageable pageable,
        NaturalLanguageQuery nlQuery,
        SmartSearchResult result
    ) {
        if (canUseElasticsearchPrimarySearch(nlQuery)) {
            try {
                Page<BookDocument> esResults = elasticsearchSearchService.search(normalizedQuery, pageable);
                result.setSearchEngine(SearchEngine.ELASTICSEARCH);
                return hydrateBooksFromDocuments(esResults, pageable);
            } catch (Exception e) {
                log.warn("Elasticsearch primary search failed, falling back to MySQL: {}", e.getMessage());
            }
        }

        result.setSearchEngine(SearchEngine.MYSQL_FALLBACK);
        return searchCatalogFallback(normalizedQuery, pageable, nlQuery);
    }

    private boolean canUseElasticsearchPrimarySearch(NaturalLanguageQuery nlQuery) {
        if (!elasticsearchEnabled || elasticsearchSearchService == null) {
            return false;
        }

        if (nlQuery.hasStructuredFilters()) {
            return false;
        }

        try {
            return elasticsearchSearchService.isAvailable();
        } catch (Exception e) {
            log.debug("Elasticsearch availability probe failed: {}", e.getMessage());
            return false;
        }
    }

    private Page<Book> hydrateBooksFromDocuments(Page<BookDocument> documents, Pageable pageable) {
        if (documents.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, documents.getTotalElements());
        }

        List<Long> ids = documents.getContent().stream()
            .map(BookDocument::getId)
            .filter(java.util.Objects::nonNull)
            .toList();

        Map<Long, Book> booksById = new HashMap<>();
        for (Book book : bookRepository.findAllById(ids)) {
            booksById.put(book.getId(), book);
        }

        List<Book> ordered = ids.stream()
            .map(booksById::get)
            .filter(java.util.Objects::nonNull)
            .toList();

        return new PageImpl<>(ordered, pageable, documents.getTotalElements());
    }

    private Page<Book> searchCatalogFallback(
        String normalizedQuery,
        Pageable pageable,
        NaturalLanguageQuery nlQuery
    ) {
        String keyword = nlQuery.hasStructuredFilters()
            ? nlQuery.getKeyword()
            : (StringUtils.hasText(nlQuery.getKeyword()) ? nlQuery.getKeyword() : normalizedQuery);
        return bookRepository.searchCatalog(
            keyword,
            nlQuery.getAuthor(),
            "",
            nlQuery.getCategory(),
            "",
            "",
            pageable
        );
    }

    private void populateSuggestions(String normalizedQuery, SmartSearchResult result) {
        List<String> searchTerms = new ArrayList<>();

        if (elasticsearchEnabled && elasticsearchSearchService != null) {
            try {
                searchTerms = elasticsearchSearchService.getSuggestions(normalizedQuery, 20);
            } catch (Exception e) {
                log.debug("Failed to get search suggestions from Elasticsearch: {}", e.getMessage());
            }
        }

        if (searchTerms.isEmpty()) {
            searchTerms = suggestionRepository.findByQueryStartingWithOrderByFrequencyDesc(
                    normalizedQuery,
                    PageRequest.of(0, 20)
                ).stream()
                .map(SearchSuggestion::getQuery)
                .collect(Collectors.toList());
        }

        if (searchTerms.isEmpty()) {
            return;
        }

        List<FuzzySearchService.SimilarityMatch> similarTitles =
            fuzzySearchService.findSimilarMatches(normalizedQuery, searchTerms);

        if (!similarTitles.isEmpty()) {
            result.setSuggestions(similarTitles.stream()
                .map(FuzzySearchService.SimilarityMatch::text)
                .limit(5)
                .collect(Collectors.toList()));
            result.setDidYouMean(similarTitles.get(0).text());
        }
    }

    private NaturalLanguageQuery parseNaturalLanguage(String query) {
        NaturalLanguageQuery nlQuery = new NaturalLanguageQuery();
        String remaining = query;
        List<String> interpretations = new ArrayList<>();

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
                        default -> {
                        }
                    }
                }
                remaining = remaining.replace(entry.getKey(), "").trim();
            }
        }

        Pattern authorPattern = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z]+)(的书|写的|著)");
        Matcher authorMatcher = authorPattern.matcher(query);
        if (authorMatcher.find()) {
            String author = authorMatcher.group(1);
            nlQuery.setAuthor(author);
            interpretations.add("作者: " + author);
            remaining = remaining.replaceAll(authorPattern.pattern(), "").trim();
        }

        if (!remaining.isEmpty()) {
            nlQuery.setKeyword(remaining);
            interpretations.add("关键词: " + remaining);
        }

        if (!interpretations.isEmpty()) {
            nlQuery.setInterpretation("理解为: " + String.join(", ", interpretations));
        }

        return nlQuery;
    }

    private String normalizeQuery(String query) {
        return query.trim()
            .toLowerCase()
            .replaceAll("\\s+", " ");
    }

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
            log.debug("Failed to record search query {}", query, e);
        }
    }

    private static class NaturalLanguageQuery {
        private String keyword = "";
        private String author = "";
        private String category = "";
        private String sort = "";
        private final List<String> tags = new ArrayList<>();
        private String interpretation = "";

        boolean hasStructuredFilters() {
            return StringUtils.hasText(author) || StringUtils.hasText(category) || !tags.isEmpty();
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public void addTag(String tag) {
            this.tags.add(tag);
        }

        public String getInterpretation() {
            return interpretation;
        }

        public void setInterpretation(String interpretation) {
            this.interpretation = interpretation;
        }
    }

    public enum SearchEngine {
        ELASTICSEARCH,
        MYSQL_FALLBACK
    }

    public static class SmartSearchResult {
        private Page<Book> books;
        private String originalQuery;
        private String normalizedQuery;
        private String didYouMean;
        private List<String> suggestions = new ArrayList<>();
        private String interpretation;
        private SearchEngine searchEngine;

        public Page<Book> getBooks() {
            return books;
        }

        public void setBooks(Page<Book> books) {
            this.books = books;
        }

        public String getOriginalQuery() {
            return originalQuery;
        }

        public void setOriginalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        public String getNormalizedQuery() {
            return normalizedQuery;
        }

        public void setNormalizedQuery(String normalizedQuery) {
            this.normalizedQuery = normalizedQuery;
        }

        public String getDidYouMean() {
            return didYouMean;
        }

        public void setDidYouMean(String didYouMean) {
            this.didYouMean = didYouMean;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public String getInterpretation() {
            return interpretation;
        }

        public void setInterpretation(String interpretation) {
            this.interpretation = interpretation;
        }

        public SearchEngine getSearchEngine() {
            return searchEngine;
        }

        public void setSearchEngine(SearchEngine searchEngine) {
            this.searchEngine = searchEngine;
        }
    }
}
