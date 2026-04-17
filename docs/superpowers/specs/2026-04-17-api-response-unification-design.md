# API 响应格式统一设计文档

## 1. 背景与目标

### 1.1 当前问题

系统中存在三种不同的 API 响应格式，导致代码不一致和前端开发体验不佳：

1. **ApiResponse<T> 包装器**（较新的控制器）
   - 使用位置：BookReviewController、AdminUserManagementController
   - 特点：类型安全，支持分页，有静态工厂方法

2. **Map.of("success", true, ...)** （较旧的控制器）
   - 使用位置：BorrowController、AuthController、ReservationController、NotificationController 部分接口
   - 特点：灵活但缺乏类型安全

3. **直接返回裸对象**
   - 使用位置：NotificationController.getAllNotifications()、SearchHistoryController 部分接口
   - 特点：最简单但缺少统一的成功/错误标识

### 1.2 目标

- 统一所有 API 接口使用 `ApiResponse<T>` 包装器格式
- 提升代码一致性和可维护性
- 改善前端开发体验，统一响应处理逻辑
- 保持现有 ApiResponse 设计不变

## 2. 设计方案

### 2.1 统一格式

所有 API 接口统一使用 `ApiResponse<T>` 包装器：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;      // 成功标识
    private T data;               // 数据载荷
    private String message;       // 消息文案
    private Integer total;        // 分页：总记录数
    private Integer page;         // 分页：当前页码
    private Integer size;         // 分页：每页大小
    private Integer totalPages;   // 分页：总页数
}
```

### 2.2 迁移范围

需要迁移的控制器（共 6 个，约 26 个接口）：

| 控制器 | 接口数量 | 当前格式 | 优先级 |
|--------|---------|---------|--------|
| BorrowController | 8 | Map.of | 高 |
| AuthController | 7 | Map.of | 高 |
| ReservationController | 5 | Map.of | 高 |
| NotificationController | 3 | Map.of + 裸对象 | 中 |
| SearchHistoryController | 3 | Map.of + 裸对象 | 中 |
| CaptchaController | 待确认 | Map.of | 低 |

已使用 ApiResponse 的控制器（保持不变）：
- BookReviewController（9个接口）
- AdminUserManagementController（4个接口）

### 2.3 响应格式映射规则

| 旧格式 | 新格式 | 示例 |
|--------|--------|------|
| `Map.of("success", true, "data", xxx)` | `ApiResponse.ok(xxx)` | 成功响应，仅数据 |
| `Map.of("success", true, "message", "xxx", "data", yyy)` | `ApiResponse.ok(yyy, "xxx")` | 成功响应，数据+消息 |
| `return list;` | `ApiResponse.ok(list)` | 裸对象转包装 |
| `Map.of("success", false, "message", "xxx")` | `ApiResponse.error("xxx")` | 错误响应 |
| `Map.of("success", true, "user", userInfo)` | `ApiResponse.ok(userInfo)` | 特殊字段名统一为 data |

### 2.4 迁移策略

**方案选择：手动逐个修改控制器**

理由：
- 项目规模适中（约 26 个接口需要迁移）
- 可以精确控制每个接口的响应格式
- 可以针对性优化消息文案
- 避免全局拦截器可能带来的双重包装问题

**迁移步骤：**

1. **后端迁移**
   - 保持 ApiResponse 类不变
   - 逐个修改控制器方法，将返回类型改为 `ResponseEntity<ApiResponse<T>>`
   - 使用 ApiResponse 的静态工厂方法替换 Map.of 和裸对象
   - 统一消息文案，确保中英文一致性
   - 运行单元测试确保功能正常

2. **前端适配**
   - 所有接口响应统一从 `response.data` 中提取数据
   - 成功判断统一使用 `response.success`
   - 错误消息统一从 `response.message` 获取
   - 更新 TypeScript 类型定义

3. **测试验证**
   - 后端单元测试
   - 前端集成测试
   - 手动回归测试关键功能

### 2.5 向后兼容性

**策略：一次性全部迁移**

理由：
- 项目规模不大，一次性迁移可行
- 避免长期维护两套格式的复杂性
- 前后端改动在同一个提交中，减少中间状态的混乱

## 3. 实现细节

### 3.1 后端改动示例

**改动前（BorrowController）：**
```java
@PostMapping("/apply")
public ResponseEntity<?> applyBorrow(@Valid @RequestBody BorrowRequest request, Authentication authentication) {
    Long userId = getUserIdFromAuth(authentication);
    BorrowResponse response = borrowService.applyBorrow(userId, request);
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", response.getStatusHint(),
        "data", response
    ));
}
```

**改动后：**
```java
@PostMapping("/apply")
public ResponseEntity<ApiResponse<BorrowResponse>> applyBorrow(@Valid @RequestBody BorrowRequest request, Authentication authentication) {
    Long userId = getUserIdFromAuth(authentication);
    BorrowResponse response = borrowService.applyBorrow(userId, request);
    return ApiResponse.ok(response, response.getStatusHint());
}
```

**改动前（NotificationController）：**
```java
@GetMapping
public List<NotificationRecord> getAllNotifications(Authentication authentication) {
    Long userId = getUserIdFromAuth(authentication);
    return notificationService.getUserNotifications(userId);
}
```

**改动后：**
```java
@GetMapping
public ResponseEntity<ApiResponse<List<NotificationRecord>>> getAllNotifications(Authentication authentication) {
    Long userId = getUserIdFromAuth(authentication);
    List<NotificationRecord> notifications = notificationService.getUserNotifications(userId);
    return ApiResponse.ok(notifications);
}
```

**改动前（AuthController 错误处理）：**
```java
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
    try {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "刷新令牌不能为空。"
            ));
        }
        // ...
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "message", e.getMessage()
        ));
    }
}
```

**改动后：**
```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");
    if (refreshToken == null || refreshToken.isEmpty()) {
        return ApiResponse.error("刷新令牌不能为空。");
    }
    
    try {
        AuthResponse response = userService.refreshToken(refreshToken);
        return ApiResponse.ok(response);
    } catch (IllegalArgumentException e) {
        return ApiResponse.error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}
```

### 3.2 前端适配示例

**改动前：**
```typescript
// BorrowController 响应
const response = await api.post('/api/borrow/apply', request);
if (response.success) {
  const borrowData = response.data;
  // ...
}

// NotificationController 响应（裸对象）
const notifications = await api.get('/api/notifications');
// notifications 直接是数组
```

**改动后：**
```typescript
// 统一处理
const response = await api.post('/api/borrow/apply', request);
if (response.success) {
  const borrowData = response.data;
  // ...
}

// 统一处理
const response = await api.get('/api/notifications');
if (response.success) {
  const notifications = response.data;
  // ...
}
```

### 3.3 特殊情况处理

**AuthController.getCurrentUser() 的特殊字段名：**
```java
// 改动前
return ResponseEntity.ok(Map.of(
    "success", true,
    "user", userInfo  // 特殊字段名
));

// 改动后
return ApiResponse.ok(userInfo);  // 统一使用 data 字段
```

前端需要相应调整：
```typescript
// 改动前
const user = response.user;

// 改动后
const user = response.data;
```

## 4. 测试计划

### 4.1 后端测试

1. **单元测试**
   - 为每个修改的控制器方法编写/更新单元测试
   - 验证响应格式符合 ApiResponse 结构
   - 验证成功和错误场景

2. **集成测试**
   - 运行现有的集成测试套件
   - 确保所有测试通过

### 4.2 前端测试

1. **接口适配测试**
   - 更新前端 API 调用代码
   - 验证数据提取逻辑正确

2. **功能回归测试**
   - 借阅流程
   - 预约流程
   - 通知功能
   - 搜索历史
   - 用户认证

## 5. 风险与缓解

### 5.1 风险

1. **前后端不同步**
   - 风险：后端改完但前端未及时适配，导致功能异常
   - 缓解：在同一个 PR 中完成前后端改动

2. **遗漏接口**
   - 风险：某些接口未被识别和迁移
   - 缓解：使用 grep 全面搜索 Map.of 和裸对象返回

3. **测试覆盖不足**
   - 风险：改动后引入 bug
   - 缓解：运行完整的测试套件，手动测试关键流程

### 5.2 回滚计划

如果迁移后发现严重问题：
- 使用 git revert 回滚整个提交
- 修复问题后重新提交

## 6. 实施时间线

| 阶段 | 任务 | 预计时间 |
|------|------|---------|
| 1 | 后端迁移（6个控制器） | 2-3 小时 |
| 2 | 前端适配 | 1-2 小时 |
| 3 | 测试验证 | 1 小时 |
| 4 | 代码审查和提交 | 0.5 小时 |

**总计：4.5-6.5 小时**

## 7. 成功标准

- [ ] 所有控制器统一使用 ApiResponse<T> 格式
- [ ] 前端代码统一从 response.data 提取数据
- [ ] 所有单元测试通过
- [ ] 关键功能手动测试通过
- [ ] 代码审查通过

## 8. 附录

### 8.1 需要迁移的接口清单

**BorrowController (8个接口):**
1. POST /api/borrow/apply
2. POST /api/borrow/{recordId}/return
3. POST /api/borrow/{recordId}/renew
4. POST /api/borrow/{recordId}/pickup
5. GET /api/borrow/history
6. GET /api/borrow/current
7. GET /api/borrow/admin/pending
8. POST /api/borrow/admin/{recordId}/decision

**AuthController (7个接口):**
1. POST /api/auth/register
2. POST /api/auth/login
3. POST /api/auth/refresh
4. GET /api/auth/me
5. POST /api/auth/change-password
6. POST /api/auth/account-status
7. POST /api/auth/logout

**ReservationController (5个接口):**
1. POST /api/reservation
2. DELETE /api/reservation/{reservationId}
3. GET /api/reservation
4. POST /api/reservation/{reservationId}/pickup
5. POST /api/reservation/{reservationId}/extend

**NotificationController (3个接口):**
1. GET /api/notifications
2. PUT /api/notifications/{id}/read
3. PUT /api/notifications/read-all

**SearchHistoryController (3个接口):**
1. GET /api/search-history
2. POST /api/search-history
3. DELETE /api/search-history

**CaptchaController (待确认):**
- 需要检查具体接口

### 8.2 ApiResponse 静态方法参考

```java
// 成功响应
ApiResponse.ok(data)                              // 仅数据
ApiResponse.ok(data, message)                     // 数据 + 消息
ApiResponse.okWithPagination(data, total, page, size, totalPages)  // 分页数据

// 错误响应
ApiResponse.error(message)                        // 默认 400
ApiResponse.error(HttpStatus.XXX, message)        // 自定义状态码
ApiResponse.notFound(message)                     // 404
ApiResponse.unauthorized(message)                 // 401

// 直接构造
ApiResponse.success(data)                         // 不包装 ResponseEntity
ApiResponse.success(data, message)
```
