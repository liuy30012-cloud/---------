package com.library.service.elasticsearch;

import com.library.document.BookDocument;
import com.library.repository.BookDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 搜索服务
 * 提供搜索建议和全文搜索功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookDocumentRepository bookDocumentRepository;

    /**
     * 获取搜索建议（基于前缀匹配）
     *
     * @param prefix 搜索前缀
     * @param limit 返回数量限制
     * @return 建议列表
     */
    public List<String> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty() || prefix.length() < 2) {
            return List.of();
        }

        try {
            String normalizedPrefix = prefix.trim().toLowerCase();

            // 使用 prefix query 在 title 和 author 字段上进行前缀匹配
            Query titlePrefixQuery = Query.of(q -> q
                .prefix(PrefixQuery.of(p -> p
                    .field("title")
                    .value(normalizedPrefix)
                ))
            );

            Query authorPrefixQuery = Query.of(q -> q
                .prefix(PrefixQuery.of(p -> p
                    .field("author.keyword")
                    .value(normalizedPrefix)
                ))
            );

            Query boolQuery = Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                    .should(titlePrefixQuery)
                    .should(authorPrefixQuery)
                    .minimumShouldMatch("1")
                ))
            );

            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withMaxResults(limit)
                .build();

            SearchHits<BookDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                BookDocument.class
            );

            // 提取书名和作者作为建议
            return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .flatMap(doc -> {
                    List<String> suggestions = new java.util.ArrayList<>();
                    if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(normalizedPrefix)) {
                        suggestions.add(doc.getTitle());
                    }
                    if (doc.getAuthor() != null && doc.getAuthor().toLowerCase().contains(normalizedPrefix)) {
                        suggestions.add(doc.getAuthor());
                    }
                    return suggestions.stream();
                })
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get suggestions from Elasticsearch for prefix: {}", prefix, e);
            throw new RuntimeException("Elasticsearch suggestion query failed", e);
        }
    }

    /**
     * 全文搜索
     *
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果分页
     */
    public Page<BookDocument> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        try {
            String normalizedKeyword = keyword.trim();

            // 使用 multi_match 在多个字段上进行全文搜索
            Query multiMatchQuery = Query.of(q -> q
                .multiMatch(MultiMatchQuery.of(m -> m
                    .query(normalizedKeyword)
                    .fields("title^3", "author^2", "description", "isbn")
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                    .fuzziness("AUTO")
                ))
            );

            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .withPageable(pageable)
                .build();

            SearchHits<BookDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                BookDocument.class
            );

            List<BookDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

            return new PageImpl<>(
                content,
                pageable,
                searchHits.getTotalHits()
            );

        } catch (Exception e) {
            log.error("Failed to search from Elasticsearch for keyword: ", keyword, e);
            throw new RuntimeException("Elasticsearch search query failed", e);
        }
    }

    /**
     * 检查 Elasticsearch 是否可用
     */
    public boolean isAvailable() {
        try {
            bookDocumentRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch is not available: {}", e.getMessage());
            return false;
        }
    }
}
