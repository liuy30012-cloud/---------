# 分页 API 文档

## 概述

本次更新为以下接口添加了分页支持，解决了活跃用户数据膨胀问题。

## 分页参数

所有分页接口支持以下查询参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 0 | 页码（从0开始） |
| `size` | int | 接口特定 | 每页记录数（最大100） |
| `sort` | string[] | 接口特定 | 排序规则，格式：`字段名,方向`（如 `createdAt,desc`） |

## 分页响应格式

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {...}
  },
  "totalElements": 100,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

## 已更新的接口

### 1. 借阅历史
**接口**: `GET /api/borrow/history`

**默认分页**: 15条/页，按借阅时间倒序

**示例**:
```
GET /api/borrow/history?page=0&size=20&sort=borrowTime,desc
```

### 2. 当前借阅
**接口**: `GET /api/borrow/current`

**默认分页**: 10条/页，按借阅时间倒序

**示例**:
```
GET /api/borrow/current?page=0&size=10
```

### 3. 待审批借阅（管理员）
**接口**: `GET /api/borrow/admin/pending`

**默认分页**: 20条/页，按申请时间正序

**示例**:
```
GET /api/borrow/admin/pending?page=0&size=20&sort=applyTime,asc
```

### 4. 通知列表
**接口**: `GET /api/notifications`

**默认分页**: 20条/页，按创建时间倒序

**示例**:
```
GET /api/notifications?page=0&size=20&sort=createdAt,desc
```

### 5. 搜索历史
**接口**: `GET /api/search-history`

**默认分页**: 30条/页，按时间戳倒序

**示例**:
```
GET /api/search-history?page=0&size=30&sort=timestamp,desc
```

### 6. 收藏列表
**接口**: `GET /api/favorites`

**默认分页**: 20条/页，按收藏时间倒序

**示例**:
```
GET /api/favorites?page=0&size=20&sort=createdAt,desc
```

### 7. 阅读状态列表
**接口**: `GET /api/reading-status`

**默认分页**: 20条/页，按更新时间倒序

**支持过滤**: 可通过 `status` 参数过滤（WANT_TO_READ, READING, READ）

**示例**:
```
GET /api/reading-status?status=READING&page=0&size=20&sort=updatedAt,desc
```

## 向后兼容性

- 所有原有的非分页方法仍然保留，确保现有客户端不受影响
- 新的分页方法作为重载方法添加
- 前端可以逐步迁移到分页接口

## 性能优化

- 使用 Spring Data JPA 的 `Page` 和 `Pageable` 实现高效分页
- 数据库层面使用 `LIMIT` 和 `OFFSET` 减少数据传输
- 最大页面大小限制为 100，防止单次查询过大

## 实现细节

### PageableHelper 工具类

位置: `com.library.util.PageableHelper`

功能:
- 标准化分页参数处理
- 自动规范化负数页码为 0
- 限制最大页面大小为 100
- 解析多字段排序规则

### Repository 层

为每个需要分页的查询添加了对应的 `Page<T> findXxx(Pageable pageable)` 方法。

### Service 层

添加了接受 `Pageable` 参数的重载方法，返回 `Page<T>` 对象。

### Controller 层

添加了 `@RequestParam` 注解的分页参数，使用 `PageableHelper` 创建 `Pageable` 对象。
