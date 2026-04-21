package com.library.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.library.repository.BookDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private BookDocumentRepository bookDocumentRepository;

    @InjectMocks
    private ElasticsearchSearchService elasticsearchSearchService;

    @Test
    void buildSuggestionQuery_usesAnalyzedFieldsWithoutAuthorKeyword() {
        Query query = elasticsearchSearchService.buildSuggestionQuery("设计模式");

        assertEquals(TextQueryType.BoolPrefix, query.multiMatch().type());
        assertTrue(query.multiMatch().fields().contains("title^4"));
        assertTrue(query.multiMatch().fields().contains("author^3"));
        assertTrue(query.multiMatch().fields().contains("description^2"));
        assertFalse(query.multiMatch().fields().stream().anyMatch(field -> field.contains("author.keyword")));
    }

    @Test
    void buildSearchQuery_usesChineseFriendlyWeightedFields() {
        Query query = elasticsearchSearchService.buildSearchQuery("深入理解计算机系统");

        assertEquals(TextQueryType.BestFields, query.multiMatch().type());
        assertTrue(query.multiMatch().fields().contains("title^4"));
        assertTrue(query.multiMatch().fields().contains("author^3"));
        assertTrue(query.multiMatch().fields().contains("description^2"));
        assertTrue(query.multiMatch().fields().contains("isbn"));
    }
}
