# 接口分页功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为借阅历史、通知列表、搜索历史、收藏列表、阅读状态等 7 个接口添加分页功能

**Architecture:** 使用 Spring Data JPA 的原生分页支持，在 Repository 层添加 Pageable 参数的查询方法，Service 层调用分页方法并转换为 DTO，Controller 层接收分页参数并返回 Page 对象

**Tech Stack:** Spring Boot, Spring Data JPA, Java 17

---

## 文件结构

### 新增文件
- `backend/src/main/java/com/library/util/PageableHelper.java` - 分页参数处理工具类

### 修改文件
- `backend/src/main/java/com/library/repository/BorrowRecordRepository.java` - 添加分页查询方法
- `backend/src/main/java/com/library/repository/NotificationRecordRepository.java` - 添加分页查询方法
- `backend/src/main/java/com/library/repository/SearchHistoryRecordRepository.java` - 添加分页查询方法
- `backend/src/main/java/com/library/repository/BookFavoriteRepository.java` - 添加分页查询方法
- `backend/src/main/java/com/library/repository/ReadingStatusRecordRepository.java` - 添加分页查询方法
- `backend/src/main/java/com/library/service/BorrowService.java` - 添加分页方法
- `backend/src/main/java/com/library/service/NotificationService.java` - 添加分页方法
- `backend/src/main/java/com/library/service/SearchHistoryService.java` - 添加分页方法
- `backend/src/main/java/com/library/service/BookFavoriteService.java` - 添加分页方法
- `backend/src/main/java/com/library/service/ReadingStatusService.java` - 添加分页方法
- `backend/src/main/java/com/library/controller/BorrowController.java` - 修改接口添加分页参数
- `backend/src/main/java/com/library/controller/NotificationController.java` - 修改接口添加分页参数
- `backend/src/main/java/com/library/controller/SearchHistoryController.java` - 修改接口添加分页参数
- `backend/src/main/java/com/library/controller/BookFavoriteController.java` - 修改接口添加分页参数
- `backend/src/main/java/com/library/controller/ReadingStatusController.java` - 修改接口添加分页参数

### 测试文件
- `backend/src/test/java/com/library/util/PageableHelperTest.java` - PageableHelper 单元测试
- `backend/src/test/java/com/library/controller/BorrowControllerPaginationTest.java` - 借阅接口分页集成测试
- `backend/src/test/java/com/library/controller/NotificationControllerPaginationTest.java` - 通知接口分页集成测试

---

## Task 1: 创建 PageableHelper 工具类

**Files:**
- Create: `backend/src/main/java/com/library/util/PageableHelper.java`
- Test: `backend/src/test/java/com/library/util/PageableHelperTest.java`

- [ ] **Step 1: 编写 PageableHelper 测试 - 测试基本分页参数创建**

```java
package com.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class PageableHelperTest {

    @Test
    void createPageable_withValidParams_shouldCreateCorrectPageable() {
        Pageable pageable = PageableHelper.createPageable(0, 20, 10, new String[]{"createdAt,desc"});
        
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getSort());
    }

    @Test
    void createPageable_withNegativePage_shouldNormalizeToZero() {
        Pageable pageable = PageableHelper.createPageable(-1, 20, 10, new String[]{});
        
        assertEquals(0, pageable.getPageNumber());
    }

    @Test
    void createPageable_withZeroSize_shouldUseDefaultSize() {
        Pageable pageable = PageableHelper.createPageable(0, 0, 15, new String[]{});
        
        assertEquals(15, pageable.getPageSize());
    }

    @Test
    void createPageable_withSizeOver100_shouldCapAt100() {
        Pageable pageable = PageableHelper.createPageable(0, 200, 10, new String[]{});
        
        assertEquals(100, pageable.getPageSize());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -Dtest=PageableHelperTest`
Expected: FAIL - PageableHelper class not found

- [ ] **Step 3: 实现 PageableHelper 工具类**

```java
package com.library.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageableHelper {

    private PageableHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pageable createPageable(int page, int size, int defaultSize, String[] sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? Math.min(size, 100) : defaultSize;
        Sort sortObj = parseSort(sort);
        return PageRequest.of(normalizedPage, normalizedSize, sortObj);
    }

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

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -Dtest=PageableHelperTest`
Expected: PASS - All tests pass

- [ ] **Step 5: 提交 PageableHelper 工具类**

```bash
git add backend/src/main/java/com/library/util/PageableHelper.java backend/src/test/java/com/library/util/PageableHelperTest.java
git commit -m "feat: 添加 PageableHelper 分页参数处理工具类"
```

---

## Task 2: 修改 BorrowRecordRepository 添加分页方法

**Files:**
- Modify: `backend/src/main/java/com/library/repository/BorrowRecordRepository.java`

- [ ] **Step 1: 在 BorrowRecordRepository 添加分页查询方法**

```java
// 在文件末尾的 countByStatus 方法后添加以下方法

Page<BorrowRecord> findByUserIdOrderByBorrowTimeDesc(Long userId, Pageable pageable);

Page<BorrowRecord> findByUserIdAndStatusInOrderByBorrowTimeDesc(
    Long userId, 
    List<BorrowStatus> statuses, 
    Pageable pageable
);

Page<BorrowRecord> findByStatusOrderByApplyTimeAsc(BorrowStatus status, Pageable pageable);
```

- [ ] **Step 2: 添加必要的 import**

在文件顶部添加：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 3: 编译验证**

Run: `./mvnw compile`
Expected: SUCCESS - 编译通过

- [ ] **Step 4: 提交 Repository 修改**

```bash
git add backend/src/main/java/com/library/repository/BorrowRecordRepository.java
git commit -m "feat: BorrowRecordRepository 添加分页查询方法"
```

---

## Task 3: 修改其他 Repository 添加分页方法

**Files:**
- Modify: `backend/src/main/java/com/library/repository/NotificationRecordRepository.java`
- Modify: `backend/src/main/java/com/library/repository/SearchHistoryRecordRepository.java`
- Modify: `backend/src/main/java/com/library/repository/BookFavoriteRepository.java`
- Modify: `backend/src/main/java/com/library/repository/ReadingStatusRecordRepository.java`

- [ ] **Step 1: 修改 NotificationRecordRepository**

在文件末尾添加：
```java
Page<NotificationRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
```

添加 import：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 2: 修改 SearchHistoryRecordRepository**

在文件末尾添加：
```java
Page<SearchHistoryRecord> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
```

添加 import：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 3: 修改 BookFavoriteRepository**

在文件末尾添加：
```java
Page<BookFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
```

添加 import：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 4: 修改 ReadingStatusRecordRepository**

在文件末尾添加：
```java
Page<ReadingStatusRecord> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

Page<ReadingStatusRecord> findByUserIdAndStatusOrderByUpdatedAtDesc(
    Long userId, 
    ReadingStatus status, 
    Pageable pageable
);
```

添加 import：
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 5: 编译验证所有 Repository**

Run: `./mvnw compile`
Expected: SUCCESS - 所有 Repository 编译通过

- [ ] **Step 6: 提交所有 Repository 修改**

```bash
git add backend/src/main/java/com/library/repository/NotificationRecordRepository.java \
        backend/src/main/java/com/library/repository/SearchHistoryRecordRepository.java \
        backend/src/main/java/com/library/repository/BookFavoriteRepository.java \
        backend/src/main/java/com/library/repository/ReadingStatusRecordRepository.java
git commit -m "feat: 为通知、搜索历史、收藏、阅读状态 Repository 添加分页方法"
```

---

## Task 4: 修改 BorrowService 添加分页支持

**Files:**
- Modify: `backend/src/main/java/com/library/service/BorrowService.java`

- [ ] **Step 1: 在 BorrowService 添加分页方法**

在 getUserBorrowHistory 方法后添加重载方法：

```java
public Page<BorrowResponse> getUserBorrowHistory(Long userId, Pageable pageable) {
    Page<BorrowRecord> records = borrowRecordRepository
        .findByUserIdOrderByBorrowTimeDesc(userId, pageable);
    return records.map(this::toBorrowResponse);
}

public Page<BorrowResponse> getUserCurrentBorrows(Long userId, Pageable pageable) {
    Page<BorrowRecord> records = borrowRecordRepository
        .findByUserIdAndStatusInOrderByBorrowTimeDesc(userId, ACTIVE_BORROW_STATUSES, pageable);
    return records.map(this::toBorrowResponse);
}

public Page<BorrowResponse> getPendingBorrows(Pageable pageable) {
    Page<BorrowRecord> records = borrowRecordRepository
        .findByStatusOrderByApplyTimeAsc(BorrowStatus.PENDING, pageable);
    return records.map(this::toBorrowResponse);
}
```

- [ ] **Step 2: 添加必要的 import**

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 3: 编译验证**

Run: `./mvnw compile`
Expected: SUCCESS

- [ ] **Step 4: 提交 BorrowService 修改**

```bash
git add backend/src/main/java/com/library/service/BorrowService.java
git commit -m "feat: BorrowService 添加分页支持"
```

---

## Task 5: 修改其他 Service 添加分页支持

**Files:**
- Modify: `backend/src/main/java/com/library/service/NotificationService.java`
- Modify: `backend/src/main/java/com/library/service/SearchHistoryService.java`
- Modify: `backend/src/main/java/com/library/service/BookFavoriteService.java`
- Modify: `backend/src/main/java/com/library/service/ReadingStatusService.java`

- [ ] **Step 1: 修改 NotificationService**

添加分页方法：

```java
public Page<NotificationRecord> getUserNotifications(Long userId, Pageable pageable) {
    return notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
}
```

添加 import：

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 2: 修改 SearchHistoryService**

添加分页方法：

```java
public Page<SearchHistoryRecord> getHistory(Long userId, Pageable pageable) {
    return searchHistoryRecordRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
}
```

添加 import：

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 3: 修改 BookFavoriteService**

添加分页方法：

```java
public Page<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable) {
    Page<BookFavorite> favorites = bookFavoriteRepository
        .findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return favorites.map(this::toFavoriteResponse);
}
```

添加 import：

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 4: 修改 ReadingStatusService**

添加分页方法：

```java
public Page<ReadingStatusResponse> getUserReadingStatuses(
    Long userId, 
    ReadingStatus status, 
    Pageable pageable
) {
    Page<ReadingStatusRecord> statuses;
    if (status == null) {
        statuses = readingStatusRecordRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
    } else {
        statuses = readingStatusRecordRepository
            .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, status, pageable);
    }
    return statuses.map(this::toReadingStatusResponse);
}
```

添加 import：

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 5: 编译验证所有 Service**

Run: `./mvnw compile`
Expected: SUCCESS

- [ ] **Step 6: 提交所有 Service 修改**

```bash
git add backend/src/main/java/com/library/service/NotificationService.java \
        backend/src/main/java/com/library/service/SearchHistoryService.java \
        backend/src/main/java/com/library/service/BookFavoriteService.java \
        backend/src/main/java/com/library/service/ReadingStatusService.java
git commit -m "feat: 为通知、搜索历史、收藏、阅读状态 Service 添加分页支持"
```

---

## Task 6: 修改 BorrowController 添加分页参数

**Files:**
- Modify: `backend/src/main/java/com/library/controller/BorrowController.java`

- [ ] **Step 1: 修改 getBorrowHistory 方法添加分页参数**

将现有方法替换为：

```java
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
```

- [ ] **Step 2: 修改 getCurrentBorrows 方法添加分页参数**

```java
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
```

- [ ] **Step 3: 修改 getPendingBorrows 方法添加分页参数**

```java
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
```

- [ ] **Step 4: 添加必要的 import**

```java
import com.library.util.PageableHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 5: 编译验证**

Run: `./mvnw compile`
Expected: SUCCESS

- [ ] **Step 6: 提交 BorrowController 修改**

```bash
git add backend/src/main/java/com/library/controller/BorrowController.java
git commit -m "feat: BorrowController 添加分页参数支持"
```

---

## Task 7: 修改其他 Controller 添加分页参数

**Files:**
- Modify: `backend/src/main/java/com/library/controller/NotificationController.java`
- Modify: `backend/src/main/java/com/library/controller/SearchHistoryController.java`
- Modify: `backend/src/main/java/com/library/controller/BookFavoriteController.java`
- Modify: `backend/src/main/java/com/library/controller/ReadingStatusController.java`

- [ ] **Step 1: 修改 NotificationController.getAllNotifications**

```java
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
```

添加 import：

```java
import com.library.util.PageableHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 2: 修改 SearchHistoryController.getHistory**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<SearchHistoryRecord>>> getHistory(
    Authentication authentication,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "30") int size,
    @RequestParam(defaultValue = "timestamp,desc") String[] sort
) {
    Long userId = getUserIdFromAuth(authentication);
    Pageable pageable = PageableHelper.createPageable(page, size, 30, sort);
    Page<SearchHistoryRecord> history = searchHistoryService.getHistory(userId, pageable);
    return ApiResponse.ok(history);
}
```

添加 import：

```java
import com.library.util.PageableHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 3: 修改 BookFavoriteController.getUserFavorites**

```java
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
```

添加 import：

```java
import com.library.util.PageableHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 4: 修改 ReadingStatusController.getUserReadingStatuses**

```java
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
```

添加 import：

```java
import com.library.util.PageableHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 5: 编译验证所有 Controller**

Run: `./mvnw compile`
Expected: SUCCESS

- [ ] **Step 6: 提交所有 Controller 修改**

```bash
git add backend/src/main/java/com/library/controller/NotificationController.java \
        backend/src/main/java/com/library/controller/SearchHistoryController.java \
        backend/src/main/java/com/library/controller/BookFavoriteController.java \
        backend/src/main/java/com/library/controller/ReadingStatusController.java
git commit -m "feat: 为通知、搜索历史、收藏、阅读状态 Controller 添加分页参数"
```

---

## Task 8: 编写集成测试

**Files:**
- Create: `backend/src/test/java/com/library/controller/BorrowControllerPaginationTest.java`

- [ ] **Step 1: 创建 BorrowController 分页集成测试**

```java
package com.library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BorrowControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getBorrowHistory_withPagination_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/borrow/history")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pageable").exists())
            .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getBorrowHistory_withCustomSort_shouldReturnSortedResults() throws Exception {
        mockMvc.perform(get("/api/borrow/history")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "borrowTime,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `./mvnw test -Dtest=BorrowControllerPaginationTest`
Expected: PASS

- [ ] **Step 3: 提交集成测试**

```bash
git add backend/src/test/java/com/library/controller/BorrowControllerPaginationTest.java
git commit -m "test: 添加 BorrowController 分页集成测试"
```

---

## Task 9: 运行完整测试和验证

**Files:**
- N/A

- [ ] **Step 1: 运行所有单元测试**

Run: `./mvnw test`
Expected: All tests pass

- [ ] **Step 2: 运行应用程序验证启动**

Run: `./mvnw spring-boot:run`
Expected: Application starts successfully

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: 完成接口分页功能实现"
```

---

## 实施完成检查清单

- [ ] PageableHelper 工具类已创建并测试通过
- [ ] 所有 Repository 已添加分页方法
- [ ] 所有 Service 已添加分页支持
- [ ] 所有 Controller 已修改为支持分页参数
- [ ] 单元测试已编写并通过
- [ ] 集成测试已编写并通过
- [ ] 应用程序可以正常启动
- [ ] 所有代码已提交到 git

---

