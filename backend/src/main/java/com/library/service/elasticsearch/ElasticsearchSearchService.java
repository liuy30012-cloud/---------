package com.library.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.library.document.BookDocument;
import com.library.repository.BookDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "library.search.elasticsearch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class ElasticsearchSearchService {

    private static final List<String> SEARCH_FIELDS = List.of(
        "title^4",
        "author^3",
        "description^2",
        "isbn"
    );

    private static final List<String> SUGGESTION_FIELDS = List.of(
        "title^4",
        "author^3",
        "description^2"
    );

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookDocumentRepository bookDocumentRepository;

    public List<String> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty() || prefix.length() < 2) {
            return List.of();
        }

        try {
            String normalizedPrefix = prefix.trim();
            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(buildSuggestionQuery(normalizedPrefix))
                .withMaxResults(limit)
                .build();

            SearchHits<BookDocument> searchHits = elasticsearchOperations.search(searchQuery, BookDocument.class);

            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            String loweredPrefix = normalizedPrefix.toLowerCase(Locale.ROOT);

            for (SearchHit<BookDocument> hit : searchHits.getSearchHits()) {
                BookDocument document = hit.getContent();
                addSuggestionIfRelevant(suggestions, document.getTitle(), loweredPrefix);
                addSuggestionIfRelevant(suggestions, document.getAuthor(), loweredPrefix);
                if (suggestions.size() >= limit) {
                    break;
                }
            }

            return suggestions.stream().limit(limit).toList();
        } catch (Exception e) {
            log.error("Failed to get Elasticsearch suggestions for prefix {}", prefix, e);
            throw new RuntimeException("Elasticsearch suggestion query failed", e);
        }
    }

    public Page<BookDocument> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        try {
            String normalizedKeyword = keyword.trim();
            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(buildSearchQuery(normalizedKeyword))
                .withPageable(pageable)
                .build();

            SearchHits<BookDocument> searchHits = elasticsearchOperations.search(searchQuery, BookDocument.class);
            List<BookDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

            return new PageImpl<>(content, pageable, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("Failed to search Elasticsearch for keyword {}", keyword, e);
            throw new RuntimeException("Elasticsearch search query failed", e);
        }
    }

    public boolean isAvailable() {
        try {
            bookDocumentRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch is not available: {}", e.getMessage());
            return false;
        }
    }

    Query buildSuggestionQuery(String prefix) {
        return Query.of(q -> q.multiMatch(MultiMatchQuery.of(m -> m
            .query(prefix)
            .fields(SUGGESTION_FIELDS)
            .type(TextQueryType.BoolPrefix)
        )));
    }

    Query buildSearchQuery(String keyword) {
        return Query.of(q -> q.multiMatch(MultiMatchQuery.of(m -> m
            .query(keyword)
            .fields(SEARCH_FIELDS)
            .type(TextQueryType.BestFields)
            .fuzziness("AUTO")
        )));
    }

    private void addSuggestionIfRelevant(LinkedHashSet<String> suggestions, String candidate, String loweredPrefix) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }

        String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
        if (normalizedCandidate.contains(loweredPrefix)) {
            suggestions.add(candidate.trim());
        }
    }
}
