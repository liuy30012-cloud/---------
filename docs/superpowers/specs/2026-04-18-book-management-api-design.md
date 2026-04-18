---
title: 图书管理 API 设计规范
date: 2026-04-18
status: approved
---

# 图书管理 API 设计规范

## 1. 概述

### 1.1 背景
当前系统的 BookController 只提供了只读接口（@GetMapping），虽然 BookService 已实现了 createBook、updateBook、deleteBook 方法，但管理员无法通过 API 新增、修改或删除图书。

### 1.2 目标
为管理员提供完整的图书管理功能，包括：
- 单个图书的创建、更新、删除
- 批量删除图书
- 批量导入图书（支持 CSV 和 Excel）
- 下载导入模板

### 1.3 设计原则
- 仅管理员（ADMIN）可访问这些接口
- 复用现有 BookService 的核心方法
- 保证数据一致性和完整性
- 提供详细的错误信息和批量操作结果

## 2. API 端点设计

### 2.1 单个操作

#### 2.1.1 创建图书
```
POST /api/books
权限: ADMIN
请求体: CreateBookRequest
响应: ApiResponse<Book>
```

#### 2.1.2 更新图书
```
PUT /api/books/{id}
权限: ADMIN
路径参数: id (Long) - 图书 ID
请求体: UpdateBookRequest
响应: ApiResponse<Book>
```

#### 2.1.3 删除图书
```
DELETE /api/books/{id}
权限: ADMIN
路径参数: id (Long) - 图书 ID
响应: ApiResponse<Void>
```

### 2.2 批量操作

#### 2.2.1 批量删除
```
DELETE /api/books/batch
权限: ADMIN
请求体: BatchDeleteRequest { bookIds: List<Long> }
响应: ApiResponse<BatchDeleteResponse>
```

#### 2.2.2 批量导入
```
POST /api/books/import
权限: ADMIN
请求: MultipartFile (CSV 或 Excel)
响应: ApiResponse<ImportResponse>
```

#### 2.2.3 下载导入模板
```
GET /api/books/import/template
权限: ADMIN
响应: Excel 文件下载
```

## 3. 数据模型

### 3.1 CreateBookRequest
```java
{
  // 必填字段
  "title": "string",           // 标题，最大 200 字符
  "author": "string",          // 作者，最大 100 字符
  "isbn": "string",            // ISBN，最大 50 字符
  "location": "string",        // 位置，最大 100 字符
  
  // 可选字段
  "coverUrl": "string",        // 封面 URL，最大 500 字符
  "status": "string",          // 状态，最大 50 字符
  "year": "string",            // 出版年份，最大 20 字符
  "description": "string",     // 描述，TEXT 类型
  "languageCode": "string",    // 语言代码，最大 10 字符
  "availability": "string",    // 可用性，最大 50 字符
  "category": "string",        // 分类，最大 100 字符
  "circulationPolicy": "AUTO|MANUAL|REFERENCE_ONLY",  // 流通策略，默认 AUTO
  "totalCopies": 1             // 总副本数，默认 1
}
```

**说明：**
- availableCopies 自动设置为与 totalCopies 相同
- borrowedCount 初始化为 0
- createdAt 和 updatedAt 由系统自动设置

### 3.2 UpdateBookRequest
```java
{
  // 可更新的所有字段（与 CreateBookRequest 相同）
  "title": "string",
  "author": "string",
  "isbn": "string",
  "location": "string",
  "coverUrl": "string",
  "status": "string",
  "year": "string",
  "description": "string",
  "languageCode": "string",
  "availability": "string",
  "category": "string",
  "circulationPolicy": "AUTO|MANUAL|REFERENCE_ONLY",
  "totalCopies": 1
}
```

**特殊处理：**
- 如果修改 totalCopies，自动调整 availableCopies = totalCopies - borrowedCount（保持借出数量不变）
- 不允许直接修改 borrowedCount（由借阅系统自动维护）
- 不允许直接修改 availableCopies（由系统自动计算）

### 3.3 BatchDeleteResponse
```java
{
  "successCount": 5,           // 成功删除数量
  "failedCount": 2,            // 失败数量
  "failures": [                // 失败详情
    {
      "bookId": 123,
      "reason": "该图书仍有未归还记录，无法删除。"
    },
    {
      "bookId": 456,
      "reason": "该图书有 3 条评论记录，无法删除。"
    }
  ]
}
```

### 3.4 ImportResponse
```java
{
  "successCount": 48,          // 成功导入数量
  "failedCount": 2,            // 失败数量
  "failures": [                // 失败详情
    {
      "row": 5,
      "reason": "ISBN 字段为空"
    },
    {
      "row": 12,
      "reason": "ISBN '978-1234567890' 已存在"
    }
  ]
}
```

## 4. 业务逻辑

### 4.1 数据验证

#### 4.1.1 创建图书验证
1. 必填字段检查：title、author、isbn、location 不能为空
2. 字段长度检查：按照数据库字段定义
3. ISBN 唯一性检查：数据库中不能存在相同 ISBN
4. totalCopies 必须 >= 1
5. circulationPolicy 必须是有效枚举值

#### 4.1.2 更新图书验证
1. 图书必须存在
2. 必填字段检查（同创建）
3. ISBN 唯一性检查（排除当前图书）
4. totalCopies 调整规则：
   - 新 totalCopies >= borrowedCount（不能小于已借出数量）
   - 自动调整：availableCopies = 新 totalCopies - borrowedCount

### 4.2 删除图书安全检查

删除图书前必须检查以下关联数据：

1. **借阅记录检查**
   - borrowedCount > 0：有未归还记录，拒绝删除
   - 错误信息："该图书仍有未归还记录，无法删除。"

2. **预约记录检查**
   - 查询 ReservationRecord 表，status 为 WAITING 或 AVAILABLE
   - 如果存在活跃预约，拒绝删除
   - 错误信息："该图书有 X 条活跃预约记录，无法删除。"

3. **评论记录检查**
   - 查询 BookReview 表
   - 如果存在评论，拒绝删除
   - 错误信息："该图书有 X 条评论记录，无法删除。"

4. **收藏记录检查**
   - 查询 Favorite 表
   - 如果存在收藏，拒绝删除
   - 错误信息："该图书被 X 位用户收藏，无法删除。"

5. **阅读状态记录检查**
   - 查询 ReadingStatus 表
   - 如果存在阅读状态，拒绝删除
   - 错误信息："该图书有 X 条阅读状态记录，无法删除。"

6. **库存一致性检查**
   - availableCopies 必须等于 totalCopies
   - 错误信息："图书库存数据异常，无法删除。"

**Why:** 保证数据完整性，避免删除后产生孤立记录或数据不一致。

**How to apply:** 在 BookService.deleteBook 方法中，调用新增的 checkRelatedData 方法进行全面检查。

### 4.3 批量删除逻辑

1. 接收图书 ID 列表
2. 逐个检查删除条件
3. 收集成功和失败结果
4. 返回详细的批量操作结果

**事务处理：**
- 每个删除操作独立提交
- 部分失败不影响已成功的删除
- 使用 @Transactional(propagation = Propagation.REQUIRES_NEW)

### 4.4 批量导入逻辑

#### 4.4.1 文件格式支持
- CSV：使用 OpenCSV 解析
- Excel (.xlsx)：使用 Apache POI 解析

#### 4.4.2 导入流程
1. **文件解析**
   - 检查文件格式（CSV 或 Excel）
   - 解析表头，验证必需列是否存在
   - 逐行解析数据

2. **数据验证**
   - 必填字段检查（title、author、isbn、location）
   - 字段长度检查
   - ISBN 格式验证（可选）
   - ISBN 唯一性检查（数据库 + 当前批次内）

3. **批量插入**
   - 使用 BookService.createBook 逐个创建
   - 收集成功和失败记录
   - 返回详细结果

**事务处理：**
- 每个插入操作独立提交
- 部分失败不影响已成功的插入
- 使用 @Transactional(propagation = Propagation.REQUIRES_NEW)

#### 4.4.3 导入模板格式
Excel 模板包含以下列：
- title* (必填)
- author* (必填)
- isbn* (必填)
- location* (必填)
- coverUrl
- status
- year
- description
- languageCode
- availability
- category
- circulationPolicy (AUTO/MANUAL/REFERENCE_ONLY)
- totalCopies (默认 1)

模板包含示例数据行，帮助用户理解格式。

## 5. 技术实现

### 5.1 Controller 层

在 BookController 中添加以下方法：

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ApiResponse<Book>> createBook(@Valid @RequestBody CreateBookRequest request)

@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<Book>> updateBook(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest request)

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id)

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/batch")
public ResponseEntity<ApiResponse<BatchDeleteResponse>> batchDeleteBooks(@RequestBody BatchDeleteRequest request)

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/import")
public ResponseEntity<ApiResponse<ImportResponse>> importBooks(@RequestParam("file") MultipartFile file)

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/import/template")
public ResponseEntity<Resource> downloadTemplate()
```

### 5.2 Service 层增强

在 BookService 中添加以下方法：

```java
// 验证图书数据
public void validateBook(Book book) throws IllegalArgumentException

// 检查关联数据（用于删除前检查）
public void checkRelatedData(Long bookId) throws IllegalArgumentException

// 更新时调整库存
public void adjustCopiesOnUpdate(Book existingBook, Book updatedBook)

// 批量删除
public BatchDeleteResult batchDeleteBooks(List<Long> bookIds)

// 批量创建
public BatchImportResult batchCreateBooks(List<Book> books)
```

**依赖注入：**
- ReservationRecordRepository
- BookReviewRepository
- FavoriteRepository
- ReadingStatusRepository

### 5.3 工具类

创建 BookFileParser 工具类：

```java
public class BookFileParser {
    // 解析 Excel 文件
    public static List<Book> parseExcel(MultipartFile file) throws IOException
    
    // 解析 CSV 文件
    public static List<Book> parseCsv(MultipartFile file) throws IOException
    
    // 生成导入模板
    public static byte[] generateTemplate() throws IOException
}
```

**依赖库：**
- Apache POI (Excel)
- OpenCSV (CSV)

### 5.4 Elasticsearch 同步

**Why:** 项目已集成 Elasticsearch，图书数据变更需要同步到搜索引擎。

**How to apply:** 依赖现有的 BookSyncListener（@EntityListeners），无需在 Controller 或 Service 层手动触发。BookSyncListener 会自动监听 Book 实体的创建、更新、删除事件。

## 6. 错误处理

### 6.1 HTTP 状态码

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 创建成功 | 200 OK | 返回创建的 Book 对象 |
| 更新成功 | 200 OK | 返回更新后的 Book 对象 |
| 删除成功 | 200 OK | 返回空响应 |
| 数据验证失败 | 400 Bad Request | 详细说明哪个字段不符合要求 |
| 权限不足 | 403 Forbidden | 非管理员访问 |
| 图书不存在 | 404 Not Found | 更新或删除时图书不存在 |
| 删除冲突 | 409 Conflict | 有关联数据，无法删除 |
| ISBN 重复 | 409 Conflict | 创建或更新时 ISBN 已存在 |
| 文件格式错误 | 400 Bad Request | 导入文件格式不支持 |

### 6.2 错误响应格式

使用现有的 ApiResponse 格式：

```java
{
  "success": false,
  "message": "错误描述",
  "data": null
}
```

### 6.3 批量操作错误处理

批量操作（删除、导入）采用"部分成功"策略：
- 成功的操作正常提交
- 失败的操作记录原因
- 返回详细的成功/失败统计

## 7. 安全性

### 7.1 权限控制
- 所有管理接口使用 `@PreAuthorize("hasRole('ADMIN')")`
- 依赖 Spring Security 的角色验证机制

### 7.2 数据验证
- 使用 `@Valid` 注解触发 Bean Validation
- 在 Service 层进行业务逻辑验证
- 防止 SQL 注入（使用 JPA 参数化查询）

### 7.3 文件上传安全
- 限制文件大小（建议 10MB）
- 验证文件类型（仅允许 CSV 和 Excel）
- 防止路径遍历攻击

## 8. 性能考虑

### 8.1 批量操作优化
- 批量删除：逐个检查，避免一次性加载大量数据
- 批量导入：分批插入（建议每批 100 条）
- 使用事务隔离级别 SERIALIZABLE 保证数据一致性

### 8.2 缓存失效
- 图书数据变更后，BookSearchCacheService 的缓存会自动失效（依赖 @CacheEvict）
- 无需手动清理缓存

## 9. 测试计划

### 9.1 单元测试
- BookService 新增方法的单元测试
- BookFileParser 工具类测试
- 数据验证逻辑测试

### 9.2 集成测试
- API 端点测试（使用 MockMvc）
- 权限控制测试
- 批量操作测试
- 文件导入测试

### 9.3 边界测试
- 空数据测试
- 大批量数据测试（1000+ 条）
- 并发操作测试

## 10. 实施步骤

1. 创建 DTO 类（CreateBookRequest、UpdateBookRequest、BatchDeleteRequest 等）
2. 创建响应类（BatchDeleteResponse、ImportResponse）
3. 创建 BookFileParser 工具类
4. 增强 BookService（添加验证、批量操作方法）
5. 在 BookController 中添加管理接口
6. 编写单元测试
7. 编写集成测试
8. 手动测试和验证
9. 更新 API 文档

## 11. 依赖项

需要添加以下 Maven 依赖：

```xml
<!-- Apache POI for Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- OpenCSV for CSV -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.7.1</version>
</dependency>
```

## 12. 未来扩展

可能的扩展功能（不在本次实施范围内）：
- 图书导出功能（导出为 CSV/Excel）
- 批量修改功能
- 图书审计日志
- 图书标签管理
- 图书系列管理
