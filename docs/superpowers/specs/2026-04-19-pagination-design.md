---
name: 接口分页功能设计
description: 为借阅历史、通知列表、搜索历史、收藏列表、阅读状态等接口添加分页功能
type: feature
date: 2026-04-19
---

# 接口分页功能设计文档

## 1. 背景与目标

### 1.1 问题描述
当前系统中多个接口返回完整列表，没有分页功能，包括：
- 借阅历史 `/api/borrow/history`
- 当前借阅 `/api/borrow/current`
- 待审核借阅 `/api/borrow/admin/pending`
- 通知列表 `/api/notifications`
- 搜索历史 `/api/search-history`
- 收藏列表 `/api/favorites`
- 阅读状态 `/api/reading-status`

活跃用户的数据会不断膨胀，导致：
- 单次请求返回数据量过大，影响性能
- 前端渲染压力大，用户体验差
- 数据库查询效率低下
- 网络传输开销大

### 1.2 设计目标
- 为上述接口添加分页功能，支持按需加载数据
- 使用 Spring Data 的 `Page<T>` 对象作为统一的分页响应格式
- 根据不同接口特点设置合理的默认分页大小
- 提供灵活的多字段排序支持
- 保持向后兼容性，不破坏现有 API 契约

## 2. 技术方案

### 2.1 实现策略
采用混合方案，根据每个接口的复杂度选择最优实现：
- **简单单表查询**：使用 Repository 层原生分页（Spring Data JPA）
- **复杂业务逻辑**：使用 Service 层手动分页

当前所有需要分页的接口都是简单单表查询，因此统一使用 Repository 层原生分页。

### 2.2 分页参数设计

所有接口统一使用以下查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 0 | 页码，从 0 开始 |
| `size` | int | 否 | 见下表 | 每页大小，最大值 100 |
| `sort` | String[] | 否 | 见下表 | 排序字段和方向，格式为 `field,direction` |

### 2.3 各接口默认配置

| 接口 | 默认 size | 默认 sort | 说明 |
|------|----------|-----------|------|
| 借阅历史 | 15 | `borrowTime,desc` | 按借阅时间倒序 |
| 当前借阅 | 10 | `borrowTime,desc` | 按借阅时间倒序 |
| 待审核借阅 | 20 | `applyTime,asc` | 按申请时间正序（先申请先处理） |
| 通知列表 | 20 | `createdAt,desc` | 按创建时间倒序 |
| 搜索历史 | 30 | `searchTime,desc` | 按搜索时间倒序 |
| 收藏列表 | 20 | `createdAt,desc` | 按收藏时间倒序 |
| 阅读状态 | 20 | `updatedAt,desc` | 按更新时间倒序 |

### 2.4 分页响应格式

使用 Spring Data 的 `Page<T>` 对象，包含以下字段：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],           // 当前页数据
    "pageable": {
      "pageNumber": 0,          // 当前页码
      "pageSize": 20,           // 每页大小
      "sort": {...},            // 排序信息
      "offset": 0,              // 偏移量
      "paged": true,
      "unpaged": false
    },
    "totalElements": 100,       // 总记录数
    "totalPages": 5,            // 总页数
    "last": false,              // 是否最后一页
    "first": true,              // 是否第一页
    "size": 20,                 // 每页大小
    "number": 0,                // 当前页码
    "numberOfElements": 20,     // 当前页实际记录数
    "empty": false              // 是否为空
  }
}
```

## 3. 详细设计

### 3.1 Repository 层改造

#### 3.1.1 BorrowRecordRepository

```java
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    // 现有方法保持不变...
    
    // 新增分页方法
    Page<BorrowRecord> findByUserIdOrderByBorrowTimeDesc(Long userId, Pageable pageable);
    
    Page<BorrowRecord> findByUserIdAndStatusInOrderByBorrowTimeDesc(
        Long userId, 
        List<BorrowStatus> statuses, 
        Pageable pageable
    );
    
    Page<BorrowRecord> findByStatusOrderByApplyTimeAsc(
        BorrowStatus status, 
        Pageable pageable
    );
}
```

#### 3.1.2 NotificationRecordRepository

```java
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
    // 现有方法保持不变...
    
    // 新增分页方法
    Page<NotificationRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
```

#### 3.1.3 SearchHistoryRecordRepository

```java
public interface SearchHistoryRecordRepository extends JpaRepository<SearchHistoryRecord, Long> {
    // 现有方法保持不变...
    
    // 新增分页方法
    Page<SearchHistoryRecord> findByUserIdOrderBySearchTimeDesc(Long userId, Pageable pageable);
}
```

#### 3.1.4 BookFavoriteRepository

```java
public interface BookFavoriteRepository extends JpaRepository<BookFavorite, Long> {
    // 现有方法保持不变...
    
    // 新增分页方法
    Page<BookFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
```

#### 3.1.5 ReadingStatusRepository

```java
public interface ReadingStatusRepository extends JpaRepository<ReadingStatus, Long> {
    // 现有方法保持不变...
    
    // 新增分页方法
    Page<ReadingStatus> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    
    Page<ReadingStatus> findByUserIdAndStatusOrderByUpdatedAtDesc(
        Long userId, 
        ReadingStatus status, 
        Pageable pageable
    );
}
```

### 3.2 Service 层改造

#### 3.2.1 BorrowService

```java
@Service
public class BorrowService {
    // 修改现有方法，添加分页支持
    public Page<BorrowResponse> getUserBorrowHistory(Long userId, Pageable pageable) {
        Page<BorrowRecord> records = borrowRecordRepository
            .findByUserIdOrderByBorrowTimeDesc(userId, pageable);
        return records.map(this::toBorrowResponse);
    }
    
    public Page<BorrowResponse> getUserCurrentBorrows(Long userId, Pageable pageable) {
        Page<BorrowRecord> records = borrowRecordRepository
            .findByUserIdAndStatusInOrderByBorrowTimeDesc(userId, ACTIVE_STATUSES, pageable);
        return records.map(this::toBorrowResponse);
    }
    
    public Page<BorrowResponse> getPendingBorrows(Pageable pageable) {
        Page<BorrowRecord> records = borrowRecordRepository
            .findByStatusOrderByApplyTimeAsc(BorrowStatus.PENDING, pageable);
        return records.map(this::toBorrowResponse);
    }
    
    // 保留原有的 List 返回方法，用于内部调用或向后兼容
    public List<BorrowResponse> getUserBorrowHistory(Long userId) {
        return getUserBorrowHistory(userId, Pageable.unpaged()).getContent();
    }
}
```

#### 3.2.2 NotificationService

```java
@Service
public class NotificationService {
    public Page<NotificationRecord> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    // 保留原有方法
    public List<NotificationRecord> getUserNotifications(Long userId) {
        return getUserNotifications(userId, Pageable.unpaged()).getContent();
    }
}
```

#### 3.2.3 SearchHistoryService

```java
@Service
public class SearchHistoryService {
    public Page<SearchHistoryRecord> getHistory(Long userId, Pageable pageable) {
        return searchHistoryRecordRepository.findByUserIdOrderBySearchTimeDesc(userId, pageable);
    }
    
    // 保留原有方法
    public List<SearchHistoryRecord> getHistory(Long userId) {
        return getHistory(userId, Pageable.unpaged()).getContent();
    }
}
```

#### 3.2.4 BookFavoriteService

```java
@Service
public class BookFavoriteService {
    public Page<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable) {
        Page<BookFavorite> favorites = bookFavoriteRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return favorites.map(this::toFavoriteResponse);
    }
    
    // 保留原有方法
    public List<FavoriteResponse> getUserFavorites(Long userId) {
        return getUserFavorites(userId, Pageable.unpaged()).getContent();
    }
}
```

#### 3.2.5 ReadingStatusService

```java
@Service
public class ReadingStatusService {
    public Page<ReadingStatusResponse> getUserReadingStatuses(
        Long userId, 
        ReadingStatus status, 
        Pageable pageable
    ) {
        Page<ReadingStatus> statuses;
        if (status == null) {
            statuses = readingStatusRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        } else {
            statuses = readingStatusRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, status, pageable);
        }
        return statuses.map(this::toReadingStatusResponse);
    }
    
    // 保留原有方法
    public List<ReadingStatusResponse> getUserReadingStatuses(Long userId, ReadingStatus status) {
        return getUserReadingStatuses(userId, status, Pageable.unpaged()).getContent();
    }
}
```

### 3.3 Controller 层改造

#### 3.3.1 通用分页参数处理工具类

创建一个工具类来统一处理分页参数：

```java
@Component
public class PageableHelper {
    
    /**
     * 创建 Pageable 对象
     * @param page 页码
     * @param size 每页大小
     * @param defaultSize 默认每页大小
     * @param sort 排序参数
     * @return Pageable 对象
     */
    public static Pageable createPageable(
        int page, 
        int size, 
        int defaultSize, 
        String[] sort
    ) {
        // 参数校验和规范化
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? Math.min(size, 100) : defaultSize;
        
        // 解析排序参数
        Sort sortObj = parseSort(sort);
        
        return PageRequest.of(normalizedPage, normalizedSize, sortObj);
    }
    
    /**
     * 解析排序参数
     * @param sort 排序参数数组，格式为 ["field,direction", ...]
     * @return Sort 对象
     */
    private static Sort parseSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            if (parts.length >= 1) {
                String field = parts[0].trim();
                Sort.Direction direction = parts.length > 1 && 
                    "asc".equalsIgnoreCase(parts[1].trim()) 
                    ? Sort.Direction.ASC 
                    : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, field));
            }
        }
        
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
```

#### 3.3.2 BorrowController

```java
@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {
    
    private final BorrowService borrowService;
    private final JwtUtil jwtUtil;
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BorrowResponse>>> getBorrowHistory(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "borrowTime,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 15, sort);
        Page<BorrowResponse> history = borrowService.getUserBorrowHistory(userId, pageable);
        return ApiResponse.ok(history);
    }
    
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Page<BorrowResponse>>> getCurrentBorrows(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "borrowTime,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 10, sort);
        Page<BorrowResponse> borrows = borrowService.getUserCurrentBorrows(userId, pageable);
        return ApiResponse.ok(borrows);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<Page<BorrowResponse>>> getPendingBorrows(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "applyTime,asc") String[] sort
    ) {
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        return ApiResponse.ok(borrowService.getPendingBorrows(pageable));
    }
    
    // 其他方法保持不变...
}
```

#### 3.3.3 NotificationController

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationRecord>>> getAllNotifications(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<NotificationRecord> notifications = notificationService.getUserNotifications(userId, pageable);
        return ApiResponse.ok(notifications);
    }
    
    // 其他方法保持不变...
}
```

#### 3.3.4 SearchHistoryController

```java
@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {
    
    private final SearchHistoryService searchHistoryService;
    private final JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SearchHistoryRecord>>> getHistory(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size,
        @RequestParam(defaultValue = "searchTime,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 30, sort);
        Page<SearchHistoryRecord> history = searchHistoryService.getHistory(userId, pageable);
        return ApiResponse.ok(history);
    }
    
    // 其他方法保持不变...
}
```

#### 3.3.5 BookFavoriteController

```java
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class BookFavoriteController {
    
    private final BookFavoriteService bookFavoriteService;
    private final JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> getUserFavorites(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<FavoriteResponse> favorites = bookFavoriteService.getUserFavorites(userId, pageable);
        return ApiResponse.ok(favorites);
    }
    
    // 其他方法保持不变...
}
```

#### 3.3.6 ReadingStatusController

```java
@RestController
@RequestMapping("/api/reading-status")
@RequiredArgsConstructor
public class ReadingStatusController {
    
    private final ReadingStatusService readingStatusService;
    private final JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReadingStatusResponse>>> getUserReadingStatuses(
        Authentication authentication,
        @RequestParam(required = false) ReadingStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "updatedAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<ReadingStatusResponse> statuses = readingStatusService
            .getUserReadingStatuses(userId, status, pageable);
        return ApiResponse.ok(statuses);
    }
    
    // 其他方法保持不变...
}
```

## 4. 向后兼容性

### 4.1 兼容策略
- 分页参数都是可选的，不传参数时使用默认值
- Service 层保留原有的 `List` 返回方法，供内部调用
- 前端可以通过不传分页参数来获取所有数据（使用 `Pageable.unpaged()`）

### 4.2 迁移建议
建议前端逐步迁移到分页接口：
1. 第一阶段：使用默认分页参数，验证功能正常
2. 第二阶段：根据实际需求调整分页大小和排序
3. 第三阶段：实现无限滚动或分页导航

## 5. 性能优化

### 5.1 数据库索引
确保以下字段有索引：
- `borrow_record.user_id`
- `borrow_record.borrow_time`
- `borrow_record.apply_time`
- `borrow_record.status`
- `notification_record.user_id`
- `notification_record.created_at`
- `search_history_record.user_id`
- `search_history_record.search_time`
- `book_favorite.user_id`
- `book_favorite.created_at`
- `reading_status.user_id`
- `reading_status.updated_at`

### 5.2 查询优化
- 使用 Spring Data JPA 的方法命名查询，自动生成优化的 SQL
- 避免 N+1 查询问题，必要时使用 `@EntityGraph` 或 JOIN FETCH
- 对于大数据量场景，考虑使用游标分页（Cursor-based Pagination）

## 6. 测试计划

### 6.1 单元测试
- 测试 Repository 层分页方法
- 测试 Service 层分页逻辑
- 测试 PageableHelper 工具类

### 6.2 集成测试
- 测试各个 Controller 接口的分页功能
- 测试边界条件（page=0, size=1, size=100, size>100）
- 测试排序功能（单字段、多字段、正序、倒序）
- 测试空结果集

### 6.3 性能测试
- 测试大数据量下的分页性能
- 对比分页前后的响应时间和内存占用

## 7. API 文档更新

需要更新以下接口的 API 文档：
- `GET /api/borrow/history`
- `GET /api/borrow/current`
- `GET /api/borrow/admin/pending`
- `GET /api/notifications`
- `GET /api/search-history`
- `GET /api/favorites`
- `GET /api/reading-status`

文档需要包含：
- 新增的分页参数说明
- 分页响应格式示例
- 排序参数使用示例

## 8. 实施步骤

1. 创建 PageableHelper 工具类
2. 修改 Repository 层，添加分页方法
3. 修改 Service 层，添加分页支持
4. 修改 Controller 层，添加分页参数
5. 编写单元测试
6. 编写集成测试
7. 更新 API 文档
8. 前端适配测试
9. 性能测试
10. 上线部署

## 9. 风险与注意事项

### 9.1 风险
- 前端可能依赖完整列表数据，需要协调前端同步修改
- 大数据量下的总数查询（COUNT）可能影响性能
- 排序字段可能不存在或类型不匹配，需要做好异常处理

### 9.2 注意事项
- 确保所有排序字段都有索引
- 对于超大数据量（百万级以上），考虑使用游标分页
- 监控分页接口的性能指标，及时优化
- 考虑添加缓存机制，减少数据库压力

## 10. 后续优化方向

- 实现游标分页（Cursor-based Pagination）支持实时数据流
- 添加分页缓存机制
- 支持更复杂的过滤和搜索条件
- 实现虚拟滚动（Virtual Scrolling）优化前端性能
