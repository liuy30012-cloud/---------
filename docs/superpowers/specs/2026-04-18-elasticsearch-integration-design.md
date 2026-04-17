# Elasticsearch 全文搜索引擎集成设计

## 一、背景和问题

### 当前问题

在大型图书馆场景（几万到几十万本书）下，系统存在严重的性能问题：

1. **StatisticsService.getInventoryStatistics()** (第122行)
   - 调用 `bookRepository.findAll()` 加载所有图书到内存
   - 使用 Java Stream 在内存中计算总册数和可用册数
   - 问题：全表扫描，内存占用大，响应慢

2. **SmartSearchService.collectSearchTerms()** (第213行)
   - 调用 `bookRepository.findAll()` 加载所有图书
   - 提取书名和作者用于模糊匹配
   - 问题：每次搜索建议都全表扫描，性能极差

### 性能影响

- 10万本书 × 平均2KB/条 = 200MB 内存占用
- 查询时间：3-5秒（全表扫描）
- 高并发下可能导致 OOM

## 二、解决方案：Elasticsearch 集成

### 方案选择

**选择方案三：Elasticsearch 全文搜索引擎**

**理由：**
- 搜索功能强大（拼写纠错、同义词、中文分词）
- 性能优秀，支持海量数据（百万级）
- 自带搜索建议（completion suggester）
- 聚合查询性能优异

**权衡：**
- 引入 ES，架构复杂度提升
- 需要数据同步机制
- 运维成本增加

## 三、架构设计

### 3.1 整体架构

```
┌─────────────────┐
│   前端应用      │
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────┐
│      Spring Boot 后端服务           │
│  ┌──────────────┐  ┌──────────────┐│
│  │ Statistics   │  │ SmartSearch  ││
│  │ Service      │  │ Service      ││
│  └──────┬───────┘  └──────┬───────┘│
│         │                  │        │
│         ↓                  ↓        │
│  ┌──────────────────────────────┐  │
│  │  ElasticsearchService        │  │
│  │  (降级 + 容错)               │  │
│  └──────┬───────────────┬───────┘  │
└─────────┼───────────────┼──────────┘
          │               │
          ↓               ↓
┌─────────────────┐  ┌──────────────┐
│   MySQL         │  │ Elasticsearch│
│   (主数据源)    │  │ (搜索索引)   │
└─────────────────┘  └──────────────┘
         ↑                  ↑
         └──────同步────────┘
           (JPA 事件监听)
```

### 3.2 数据流

1. **写入流程：**
   - 用户操作 → MySQL 写入
   - JPA 事件触发 → 同步到 ES
   - 准实时同步（延迟 < 1秒）

2. **查询流程：**
   - 搜索请求 → ES 查询
   - ES 故障 → 降级到 MySQL
   - 返回结果

## 四、ES 索引设计

### 4.1 book 索引 Mapping

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_pinyin": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "long"
      },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": {
            "type": "keyword"
          },
          "completion": {
            "type": "completion",
            "analyzer": "ik_max_word"
          }
        }
      },
      "author": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "isbn": {
        "type": "keyword"
      },
      "category": {
        "type": "keyword"
      },
      "languageCode": {
        "type": "keyword"
      },
      "totalCopies": {
        "type": "integer"
      },
      "availableCopies": {
        "type": "integer"
      },
      "borrowedCount": {
        "type": "integer"
      },
      "year": {
        "type": "keyword"
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "createdAt": {
        "type": "date"
      },
      "updatedAt": {
        "type": "date"
      }
    }
  }
}
```

### 4.2 索引设计说明

**分词策略：**
- `ik_max_word`：索引时细粒度分词，提高召回率
- `ik_smart`：搜索时粗粒度分词，提高准确率

**字段类型：**
- `text`：全文搜索字段（title, author, description）
- `keyword`：精确匹配字段（isbn, category）
- `completion`：自动完成字段（title.completion）
- `integer`：数值字段，用于聚合统计

**多字段映射：**
- `title` 同时支持全文搜索、精确匹配、自动完成

## 五、数据同步设计

### 5.1 同步策略

**选择：Spring Data Elasticsearch + JPA 事件监听**

**Why：**
- 代码侵入小，易于维护
- 实时性好（准实时同步）
- 不需要额外的同步组件（如 Logstash/Canal）

### 5.2 同步实现

**核心组件：**

1. **BookDocument 实体**
```java
@Document(indexName = "books")
public class BookDocument {
    @Id
    private Long id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    @CompletionField
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String author;
    
    @Field(type = FieldType.Keyword)
    private String isbn;
    
    @Field(type = FieldType.Integer)
    private Integer totalCopies;
    
    @Field(type = FieldType.Integer)
    private Integer availableCopies;
    
    // ... 其他字段
}
```

2. **JPA 事件监听器**
```java
@Component
public class BookSyncListener {
    
    @Autowired
    private ElasticsearchSyncService syncService;
    
    @PostPersist
    public void onBookCreated(Book book) {
        syncService.indexBook(book);
    }
    
    @PostUpdate
    public void onBookUpdated(Book book) {
        syncService.updateBook(book);
    }
    
    @PostRemove
    public void onBookDeleted(Book book) {
        syncService.deleteBook(book.getId());
    }
}
```

3. **同步服务**
```java
@Service
public class ElasticsearchSyncService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @Async
    public void indexBook(Book book) {
        BookDocument doc = convertToDocument(book);
        elasticsearchOperations.save(doc);
    }
    
    // 全量同步接口（用于初始化）
    public void syncAllBooks() {
        List<Book> books = bookRepository.findAll();
        List<BookDocument> docs = books.stream()
            .map(this::convertToDocument)
            .collect(Collectors.toList());
        elasticsearchOperations.save(docs);
    }
}
```

### 5.3 同步保障

**一致性保证：**
- 使用 `@Transactional` 确保 MySQL 和 ES 同步的原子性
- 失败重试机制（Spring Retry）
- 定时全量同步任务（每天凌晨）

**监控告警：**
- 同步失败计数器
- 数据一致性校验（定时对比 MySQL 和 ES 数据量）

## 六、服务改造设计

### 6.1 StatisticsService 改造

**改造前（全表扫描）：**
```java
public InventoryStatisticsDTO getInventoryStatistics() {
    List<Book> allBooks = bookRepository.findAll(); // 全表扫描
    
    long totalBooks = allBooks.stream()
        .mapToLong(Book::getTotalCopies)
        .sum();
    long availableBooks = allBooks.stream()
        .mapToLong(Book::getAvailableCopies)
        .sum();
    // ...
}
```

**改造后（ES 聚合查询）：**
```java
public InventoryStatisticsDTO getInventoryStatistics() {
    try {
        return elasticsearchStatisticsService.getInventoryStatistics();
    } catch (Exception e) {
        log.warn("ES 查询失败，降级到 MySQL", e);
        return mysqlStatisticsService.getInventoryStatistics();
    }
}

// ElasticsearchStatisticsService
public InventoryStatisticsDTO getInventoryStatistics() {
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withAggregations(
            AggregationBuilders.sum("totalCopies").field("totalCopies"),
            AggregationBuilders.sum("availableCopies").field("availableCopies")
        )
        .build();
    
    SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);
    
    Aggregations aggregations = hits.getAggregations();
    long totalBooks = (long) aggregations.get("totalCopies").getProperty("value");
    long availableBooks = (long) aggregations.get("availableCopies").getProperty("value");
    
    // ... 构造返回结果
}
```

**性能提升：**
- 查询时间：3-5秒 → 50-100ms
- 内存占用：200MB → < 1MB
- 支持并发：10 QPS → 1000+ QPS

### 6.2 SmartSearchService 改造

**改造前（全表扫描）：**
```java
private List<String> collectSearchTerms() {
    return bookRepository.findAll().stream() // 全表扫描
        .flatMap(book -> Stream.of(book.getTitle(), book.getAuthor()))
        .filter(StringUtils::hasText)
        .distinct()
        .collect(Collectors.toList());
}

public List<String> getSearchSuggestions(String prefix, int limit) {
    // 每次都调用 collectSearchTerms()，性能极差
    collectSearchTerms().stream()
        .filter(term -> normalizeQuery(term).contains(normalized))
        .forEach(/* ... */);
}
```

**改造后（ES Completion Suggester）：**
```java
public List<String> getSearchSuggestions(String prefix, int limit) {
    try {
        return elasticsearchSearchService.getSuggestions(prefix, limit);
    } catch (Exception e) {
        log.warn("ES 建议查询失败，降级到历史记录", e);
        return suggestionRepository.findByQueryStartingWith(prefix, limit);
    }
}

// ElasticsearchSearchService
public List<String> getSuggestions(String prefix, int limit) {
    CompletionSuggestionBuilder suggestionBuilder = 
        SuggestBuilders.completionSuggestion("title.completion")
            .prefix(prefix)
            .size(limit)
            .skipDuplicates(true);
    
    SuggestBuilder suggestBuilder = new SuggestBuilder()
        .addSuggestion("title-suggest", suggestionBuilder);
    
    SearchResponse response = client.search(
        new SearchRequest("books").source(
            new SearchSourceBuilder().suggest(suggestBuilder)
        ),
        RequestOptions.DEFAULT
    );
    
    return response.getSuggest()
        .getSuggestion("title-suggest")
        .getEntries().get(0)
        .getOptions().stream()
        .map(option -> option.getText().string())
        .collect(Collectors.toList());
}
```

**功能增强：**
- 支持拼写纠错（fuzzy matching）
- 支持同义词搜索
- 支持高亮显示
- 响应时间：< 50ms

### 6.3 智能搜索增强

**新增功能：**

1. **拼写纠错**
```java
// 使用 Fuzzy Query
QueryBuilder fuzzyQuery = QueryBuilders.fuzzyQuery("title", query)
    .fuzziness(Fuzziness.AUTO)
    .prefixLength(2);
```

2. **同义词搜索**
```json
// ES 配置同义词
"filter": {
  "synonym_filter": {
    "type": "synonym",
    "synonyms": [
      "计算机,电脑",
      "程序设计,编程"
    ]
  }
}
```

3. **搜索高亮**
```java
HighlightBuilder highlightBuilder = new HighlightBuilder()
    .field("title")
    .field("author")
    .preTags("<em>")
    .postTags("</em>");
```

## 七、降级和容错设计

### 7.1 降级策略

**原则：ES 作为搜索增强，不能影响核心功能**

**降级场景：**
1. ES 服务不可用
2. ES 查询超时（> 3秒）
3. ES 返回错误

**降级方案：**
- 统计查询 → 降级到 MySQL 聚合查询（添加 SQL 聚合方法）
- 搜索建议 → 降级到历史搜索记录
- 智能搜索 → 降级到 MySQL LIKE 查询

### 7.2 容错实现

**Circuit Breaker 模式：**
```java
@Service
public class ElasticsearchFallbackService {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Page<Book> search(String query, Pageable pageable) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("elasticsearch");
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                return elasticsearchService.search(query, pageable);
            } catch (Exception e) {
                log.warn("ES 查询失败，降级到 MySQL", e);
                throw e; // 触发熔断
            }
        });
    }
    
    // 降级方法
    private Page<Book> fallbackSearch(String query, Pageable pageable, Throwable t) {
        return bookRepository.searchCatalog(query, "", "", "", "", "", pageable);
    }
}
```

**配置：**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      elasticsearch:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
```

### 7.3 缓存兜底

**统计数据缓存：**
```java
@Cacheable(value = "inventory-stats", unless = "#result == null")
public InventoryStatisticsDTO getInventoryStatistics() {
    // ES 查询逻辑
}
```

**缓存策略：**
- TTL: 5分钟
- 缓存预热：应用启动时加载
- 缓存失效：图书增删改时主动失效

## 八、技术栈和依赖

### 8.1 Maven 依赖

```xml
<!-- Spring Data Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- Elasticsearch Java Client -->
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>8.11.0</version>
</dependency>

<!-- Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

### 8.2 配置文件

```yaml
spring:
  elasticsearch:
    uris: ${ES_URIS:localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD}
    connection-timeout: 3s
    socket-timeout: 5s
  
  data:
    elasticsearch:
      repositories:
        enabled: true

# 降级开关
library:
  search:
    elasticsearch:
      enabled: true
      fallback-enabled: true
```

### 8.3 环境要求

**Elasticsearch：**
- 版本：8.x
- 插件：IK 中文分词器
- 内存：建议 4GB+
- 磁盘：数据量 × 1.5 倍

**应用服务器：**
- JDK 17+
- Spring Boot 3.x

## 九、实施计划

### 9.1 实施步骤

**阶段一：环境准备（1天）**
1. 部署 Elasticsearch 8.x
2. 安装 IK 中文分词器插件
3. 创建 book 索引和 mapping
4. 验证 ES 服务可用性

**阶段二：数据同步（2天）**
1. 创建 BookDocument 实体
2. 实现 JPA 事件监听器
3. 实现 ElasticsearchSyncService
4. 全量同步初始数据
5. 验证数据一致性

**阶段三：服务改造（3天）**
1. 实现 ElasticsearchStatisticsService
2. 实现 ElasticsearchSearchService
3. 改造 StatisticsService（添加降级逻辑）
4. 改造 SmartSearchService（添加降级逻辑）
5. 单元测试和集成测试

**阶段四：容错和监控（1天）**
1. 配置 Circuit Breaker
2. 实现降级逻辑
3. 添加监控指标（Prometheus）
4. 配置告警规则

**阶段五：测试验证（2天）**
1. 功能测试（搜索、建议、统计）
2. 性能测试（压测 1000 QPS）
3. 降级测试（模拟 ES 故障）
4. 数据一致性测试

**阶段六：灰度发布（1天）**
1. 先在统计功能上线（低风险）
2. 观察 1-2 天，监控性能和错误率
3. 再切换搜索功能
4. 全量上线

**总计：10 个工作日**

### 9.2 风险和应对

**风险一：ES 服务不稳定**
- 应对：完善降级机制，确保核心功能可用
- 监控：实时监控 ES 健康状态

**风险二：数据同步延迟**
- 应对：异步同步 + 重试机制
- 监控：同步延迟告警

**风险三：性能不达预期**
- 应对：调优 ES 配置（分片数、副本数）
- 备选：引入缓存层

## 十、验收标准

### 10.1 功能验收

- [ ] 统计查询返回正确结果
- [ ] 搜索建议返回相关结果（< 50ms）
- [ ] 智能搜索支持中文分词
- [ ] 拼写纠错功能正常
- [ ] 搜索高亮显示正常

### 10.2 性能验收

- [ ] 统计查询响应时间 < 100ms
- [ ] 搜索建议响应时间 < 50ms
- [ ] 智能搜索响应时间 < 200ms
- [ ] 支持 1000 QPS 并发
- [ ] 内存占用 < 原方案的 10%

### 10.3 可用性验收

- [ ] ES 故障时自动降级
- [ ] 降级后核心功能可用
- [ ] 数据同步成功率 > 99.9%
- [ ] 数据一致性校验通过

## 十一、后续优化方向

1. **搜索排序优化**：引入 BM25 算法，提升搜索相关性
2. **个性化推荐**：基于用户搜索历史推荐
3. **搜索分析**：统计热门搜索词，优化索引
4. **多语言支持**：支持英文、日文等多语言分词
5. **向量搜索**：引入语义搜索（Elasticsearch 8.x 支持）

## 十二、总结

本设计通过引入 Elasticsearch 全文搜索引擎，彻底解决了全表扫描问题：

**性能提升：**
- 统计查询：3-5秒 → 50-100ms（50倍提升）
- 搜索建议：2-3秒 → < 50ms（60倍提升）
- 内存占用：200MB → < 1MB（200倍降低）

**功能增强：**
- 中文分词、拼写纠错、同义词搜索
- 搜索高亮、自动完成
- 支持海量数据（百万级）

**架构优化：**
- 降级和容错机制完善
- 数据同步自动化
- 监控告警体系健全

**Why：** 本方案在性能、功能、可扩展性上都达到了大型图书馆系统的要求，是最适合当前场景的解决方案。

**How to apply：** 按照实施计划分阶段推进，先环境准备，再数据同步，最后服务改造，确保平滑过渡。
