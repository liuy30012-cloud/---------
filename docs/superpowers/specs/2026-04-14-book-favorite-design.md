# 图书收藏与阅读状态功能设计文档

**日期**: 2026-04-14
**状态**: 待实现

---

## 背景与目标

读者在浏览图书详情时，经常希望能"记住"某本书以便后续借阅，但当前系统缺少收藏功能，每次都需要重新搜索。本功能旨在为读者提供类似豆瓣读书的"收藏+阅读状态"体验，核心价值：

1. 快速收藏感兴趣的图书
2. 标记阅读状态（想读/在读/已读）
3. 在"我的书架"页面统一管理收藏的书籍

---

## 技术方案：两表分离

采用收藏表与阅读状态表独立的设计，收藏和阅读状态解耦——可以收藏但不标记状态，也可以标记阅读状态但未收藏。

---

## 一、数据模型

### 1.1 `book_favorites`（收藏表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT | NOT NULL, FK → users | 用户 ID |
| `book_id` | BIGINT | NOT NULL, FK → books | 书籍 ID |
| `created_at` | DATETIME | NOT NULL | 收藏时间 |

- **唯一约束**: `uk_user_book (user_id, book_id)` — 一个用户对一本书只能收藏一次
- **索引**: `idx_user_id (user_id)`, `idx_book_id (book_id)`

### 1.2 `reading_status`（阅读状态表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT | NOT NULL, FK → users | 用户 ID |
| `book_id` | BIGINT | NOT NULL, FK → books | 书籍 ID |
| `status` | ENUM | NOT NULL | WANT_TO_READ / READING / READ |
| `notes` | TEXT | NULL | 个人备注 |
| `started_at` | DATETIME | NULL | 开始阅读时间 |
| `finished_at` | DATETIME | NULL | 完成阅读时间 |
| `created_at` | DATETIME | NOT NULL | 创建时间 |
| `updated_at` | DATETIME | NOT NULL | 更新时间 |

- **唯一约束**: `uk_user_book_status (user_id, book_id)` — 一个用户对一本书只有一条状态记录
- **索引**: `idx_user_status (user_id, status)`, `idx_book_id (book_id)`

---

## 二、API 设计

### 2.1 收藏 API — `/api/favorites`

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/favorites` | 添加收藏 | 必须 |
| DELETE | `/api/favorites/{bookId}` | 取消收藏 | 必须 |
| GET | `/api/favorites` | 获取我的收藏列表（分页） | 必须 |
| GET | `/api/favorites/check?bookId={id}` | 检查某书是否已收藏 | 必须 |

**请求/响应示例：**

```
POST /api/favorites
Body: { "bookId": 123 }
Response: { "id": 1, "bookId": 123, "createdAt": "2026-04-14T10:00:00" }

GET /api/favorites?page=0&size=12
Response: {
  "content": [
    { "id": 1, "bookId": 123, "bookTitle": "...", "author": "...", "coverUrl": "...", "readingStatus": "WANT_TO_READ" }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0
}

GET /api/favorites/check?bookId=123
Response: { "isFavorited": true }
```

### 2.2 阅读状态 API — `/api/reading-status`

| 方法 | 端点 | 说明 | 认证 |
|------|------|------|------|
| PUT | `/api/reading-status` | 设置/更新阅读状态 | 必须 |
| DELETE | `/api/reading-status/{bookId}` | 删除阅读状态 | 必须 |
| GET | `/api/reading-status` | 获取阅读状态列表（可按状态筛选，分页） | 必须 |
| GET | `/api/reading-status/{bookId}` | 获取单本书的阅读状态 | 必须 |

**请求/响应示例：**

```
PUT /api/reading-status
Body: { "bookId": 123, "status": "READING", "notes": "第三章很有启发" }
Response: { "id": 1, "bookId": 123, "status": "READING", "notes": "...", "startedAt": "2026-04-14T10:00:00" }

GET /api/reading-status?status=WANT_TO_READ&page=0&size=12
Response: {
  "content": [
    { "id": 1, "bookId": 123, "status": "WANT_TO_READ", "bookTitle": "...", "author": "...", "coverUrl": "..." }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "number": 0
}
```

---

## 三、前端交互设计

### 3.1 图书详情页（BookDetail.vue）

在封面面板的操作按钮组区域（`action-stack`），在"报告问题"按钮下方添加：

- **收藏按钮**: 心形图标 (`favorite` / `favorite_border`) + "收藏"文字
  - 未收藏状态：空心心形 + "收藏"
  - 已收藏状态：实心心形 + "已收藏"（红色）
  - 点击切换收藏/取消收藏
- **阅读状态选择器**（仅已收藏时显示）：下拉选择 想读/在读/已读
- **备注输入**（可选，展开式文本框）
- 未登录用户点击时跳转登录页

### 3.2 搜索结果页（BookSearch.vue）

在搜索结果的书籍卡片上添加快速收藏图标：

- 卡片右上角叠加一个心形小图标
- 点击快速切换收藏状态，无需跳转
- 使用 `favorite` / `favorite_border` 图标
- 搜索结果加载时批量检查收藏状态

### 3.3 新建"我的书架"页面（MyBookshelf.vue）

**路由**: `/my-bookshelf`，需要登录（`requiresAuth: true`）
**布局**: `page` 布局 + `default` 壳组件

**页面结构：**

1. **PageHeader**: 标题"我的书架"，eyebrow "MY BOOKSHELF"
2. **状态筛选标签栏**: 全部 | 想读 | 在读 | 已读（带数量角标）
3. **书籍卡片网格**: 每张卡片包含封面、标题、作者、当前阅读状态标签、备注预览
4. **每张卡片操作**:
   - 修改阅读状态下拉
   - 添加/编辑备注
   - 取消收藏（带确认对话框）
5. **空状态**: 无收藏时显示引导文字和搜索入口
6. **分页**: 底部分页控件

### 3.4 导航栏更新（TopNav.vue）

- 在 `navItems` 中新增"我的书架"项（`/my-bookshelf`），位于"我的预约"之后
- 加入 `authOnlyPages` 集合，仅登录用户可见
- 在用户下拉菜单中增加"我的书架"入口

---

## 四、后端实现要点

### 4.1 JPA 实体

遵循现有 `BookReview` 的模式，使用 `@Data` + `@Entity` + `@EntityListeners(AuditingEntityListener.class)` + Lombok。

新增文件：
- `backend/src/main/java/com/library/model/BookFavorite.java`
- `backend/src/main/java/com/library/model/ReadingStatus.java`（枚举）
- `backend/src/main/java/com/library/model/ReadingStatusRecord.java`

### 4.2 Repository

新增文件：
- `BookFavoriteRepository` — 含 `findByUserId(Long userId, Pageable)`, `existsByUserIdAndBookId()`, `deleteByUserIdAndBookId()`, `countByBookId()`
- `ReadingStatusRecordRepository` — 含 `findByUserIdAndStatus()`, `findByUserIdAndBookId()`, `deleteByUserIdAndBookId()`

### 4.3 Service

新增文件：
- `BookFavoriteService` — 收藏/取消/查询/检查
- `ReadingStatusService` — 状态设置/更新/删除/查询

### 4.4 Controller

新增文件：
- `BookFavoriteController` — 映射 `/api/favorites`
- `ReadingStatusController` — 映射 `/api/reading-status`

### 4.5 安全配置

在 `SecurityConfig.java` 中将 `/api/favorites/**` 和 `/api/reading-status/**` 加入需认证的端点列表（默认已是，无需特殊 permitAll）。

---

## 五、前端实现要点

### 5.1 新增 API 文件

`frontend/src/api/favoriteApi.ts` — 封装收藏和阅读状态的所有 API 调用

### 5.2 新增页面

`frontend/src/views/MyBookshelf.vue` — 我的书架页面

### 5.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `frontend/src/router/index.ts` | 新增 `/my-bookshelf` 路由 |
| `frontend/src/views/BookDetail.vue` | 添加收藏按钮、阅读状态选择器、备注输入 |
| `frontend/src/views/BookSearch.vue` | 在书籍卡片上添加快速收藏心形图标 |
| `frontend/src/components/navigation/TopNav.vue` | 导航栏和用户菜单增加"我的书架"入口 |

---

## 六、验证方案

### 6.1 后端验证

1. 启动后端，确认 `book_favorites` 和 `reading_status` 两张表自动创建（JPA ddl-auto: update）
2. 使用测试账号登录，通过 curl/Postman 测试所有 API 端点
3. 验证：重复收藏同一本书返回冲突、取消不存在的收藏返回 404、阅读状态枚举值校验

### 6.2 前端验证

1. 详情页：未登录点击收藏跳转登录页；登录后可收藏/取消；收藏后可设置阅读状态
2. 搜索页：卡片上心形图标可快速切换收藏状态
3. 我的书架：收藏的书正确显示；按状态筛选正常工作；修改状态/备注实时生效；取消收藏后从列表消失
4. 导航栏：登录后可见"我的书架"入口；未登录不可见

### 6.3 端到端流程

1. 搜索图书 → 在搜索结果页快速收藏
2. 进入详情页 → 确认已收藏 → 设置阅读状态为"想读"
3. 进入"我的书架" → 看到该书 → 将状态改为"在读" → 添加备注
4. 取消收藏 → 确认该书从书架消失
