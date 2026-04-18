package com.library.service.elasticsearch;

import com.library.repository.BookDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;

import java.util.Map;

/**
 * Elasticsearch 统计服务
 * 使用 ES 聚合查询来统计图书数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "library.search.elasticsearch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ElasticsearchStatisticsService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookDocumentRepository bookDocumentRepository;

    /**
     * 获取总册数统计
     */
    public Long getTotalCopiesSum() {
        try {
            NativeQuery query = NativeQuery.builder()
                .withAggregation("total_copies_sum", Aggregation.of(a -> a
                    .sum(SumAggregation.of(s -> s.field("totalCopies")))
                ))
                .withMaxResults(0)
                .build();

            SearchHits<?> searchHits = elasticsearchOperations.search(query, Object.class);
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

            if (aggregations != null && !aggregations.aggregations().isEmpty()) {
                var aggList = aggregations.aggregations();

                // 按名称查找聚合结果，而不是按索引
                for (var agg : aggList) {
                    if ("total_copies_sum".equals(agg.aggregation().getName())) {
                        return (long) agg.aggregation().getAggregate().sum().value();
                    }
                }
            }

            log.warn("Elasticsearch aggregation returned empty or incomplete results");
            return 0L;
        } catch (Exception e) {
            log.error("Failed to get total copies sum from Elasticsearch", e);
            throw new RuntimeException("Elasticsearch query failed", e);
        }
    }

    /**
     * 获取可用册数统计
     */
    public Long getAvailableCopiesSum() {
        try {
            NativeQuery query = NativeQuery.builder()
                .withAggregation("available_copies_sum", Aggregation.of(a -> a
                    .sum(SumAggregation.of(s -> s.field("availableCopies")))
                ))
                .withMaxResults(0)
                .build();

            SearchHits<?> searchHits = elasticsearchOperations.search(query, Object.class);
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

            if (aggregations != null && !aggregations.aggregations().isEmpty()) {
                var aggList = aggregations.aggregations();

                // 按名称查找聚合结果，而不是按索引
                for (var agg : aggList) {
                    if ("available_copies_sum".equals(agg.aggregation().getName())) {
                        return (long) agg.aggregation().getAggregate().sum().value();
                    }
                }
            }

            log.warn("Elasticsearch aggregation returned empty or incomplete results");
            return 0L;
        } catch (Exception e) {
            log.error("Failed to get available copies sum from Elasticsearch", e);
            throw new RuntimeException("Elasticsearch query failed", e);
        }
    }

    /**
     * 批量获取统计数据
     */
    public Map<String, Long> getInventoryStatistics() {
        try {
            NativeQuery query = NativeQuery.builder()
                .withAggregation("total_copies_sum", Aggregation.of(a -> a
                    .sum(SumAggregation.of(s -> s.field("totalCopies")))
                ))
                .withAggregation("available_copies_sum", Aggregation.of(a -> a
                    .sum(SumAggregation.of(s -> s.field("availableCopies")))
                ))
                .withMaxResults(0)
                .build();

            SearchHits<?> searchHits = elasticsearchOperations.search(query, Object.class);
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

            if (aggregations != null && !aggregations.aggregations().isEmpty()) {
                var aggList = aggregations.aggregations();

                // 按名称查找聚合结果，而不是按索引
                Long totalCopies = null;
                Long availableCopies = null;

                for (var agg : aggList) {
                    String aggName = agg.aggregation().getName();
                    long value = (long) agg.aggregation().getAggregate().sum().value();

                    if ("total_copies_sum".equals(aggName)) {
                        totalCopies = value;
                    } else if ("available_copies_sum".equals(aggName)) {
                        availableCopies = value;
                    }
                }

                if (totalCopies != null && availableCopies != null) {
                    return Map.of(
                        "totalCopies", totalCopies,
                        "availableCopies", availableCopies
                    );
                }
            }

            log.warn("Elasticsearch aggregation returned empty or incomplete results");
            return Map.of("totalCopies", 0L, "availableCopies", 0L);
        } catch (Exception e) {
            log.error("Failed to get inventory statistics from Elasticsearch", e);
            throw new RuntimeException("Elasticsearch query failed", e);
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
