# BorrowService 重构报告

> **重构日期**: 2026-04-06  
> **重构范围**: BorrowService 及相关业务逻辑  
> **目标**: 提高代码可维护性、可测试性和职责分离

---

## 📊 重构成果

### 代码行数对比

| 文件 | 重构前 | 重构后 | 变化 |
|------|--------|--------|------|
| BorrowService.java | 390行 | 316行 | **-74行 (-19%)** |

### 新增辅助类

| 文件 | 行数 | 职责 |
|------|------|------|
| BorrowValidator.java | 116行 | 业务规则验证（借阅限制、续借条件、逾期计算） |
| BorrowConverter.java | 38行 | 实体与DTO转换 |
| BorrowNotificationHelper.java | 79行 | 统一通知发送管理 |

**总计**: 316 + 116 + 38 + 79 = **549行** (比重构前多159行，但职责更清晰)

---

## 🎯 重构目标

### 1. 职责分离 (Single Responsibility Principle)

**重构前问题**:
- BorrowService 承担了过多职责：
  - 业务流程编排
  - 业务规则验证
  - 数据转换
  - 通知发送
  - 逾期计算

**重构后改进**:
- **BorrowService**: 专注于业务流程编排和事务管理
- **BorrowValidator**: 负责所有业务规则验证
- **BorrowConverter**: 负责实体与DTO转换
- **BorrowNotificationHelper**: 负责统一通知发送

### 2. 消除重复代码

**重构前问题**:
- 用户权限验证代码重复出现在多个方法中
- 通知发送的空值检查重复
- 逾期计算逻辑混杂在业务代码中

**重构后改进**:
- 权限验证统一到 `validator.validateUserPermission()`
- 通知发送统一到 `notificationHelper.sendXxxNotification()`
- 逾期计算封装到 `validator.calculateOverdue()`

### 3. 提高可测试性

**重构前问题**:
- 业务规则验证逻辑嵌入在服务方法中，难以单独测试
- 通知发送逻辑与业务逻辑耦合
- 常量分散在代码中

**重构后改进**:
- 验证器可以独立进行单元测试
- 通知助手可以独立测试
- 业务常量集中管理

---

## 🔧 重构细节

### BorrowService 重构

**移除内容**:
- ❌ 业务常量定义 (MAX_BORROW_COUNT, FINE_PER_DAY 等)
- ❌ 重复的权限验证代码
- ❌ 重复的通知发送代码
- ❌ 逾期计算逻辑
- ❌ DTO转换方法 `convertToResponse()`

**保留内容**:
- ✅ 业务流程编排
- ✅ 事务管理
- ✅ 数据库操作
- ✅ 日志记录

**新增依赖**:
```java
private final BorrowValidator validator;
private final BorrowConverter converter;
private final BorrowNotificationHelper notificationHelper;
```

### BorrowValidator 设计

**核心功能**:
1. **借阅数量验证**: `validateCanBorrow(long currentBorrowCount)`
2. **续借条件验证**: `validateCanRenew(BorrowRecord, long waitingReservations)`
3. **用户权限验证**: `validateUserPermission(Long recordUserId, Long currentUserId, String operation)`
4. **逾期计算**: `calculateOverdue(LocalDateTime dueDate)` → 返回 `OverdueInfo`

**业务常量**:
```java
public static final int MAX_BORROW_COUNT = 5;
public static final int DEFAULT_BORROW_DAYS = 30;
public static final int RENEW_DAYS = 15;
public static final int MAX_RENEW_COUNT = 1;
public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");
```

**内部类**:
- `OverdueInfo`: 封装逾期天数和罚金信息

### BorrowConverter 设计

**核心功能**:
- `toResponse(BorrowRecord record)`: 将实体转换为响应DTO

**优势**:
- 单一职责：只负责数据转换
- 易于测试：纯函数，无副作用
- 易于扩展：可以添加更多转换方法

### BorrowNotificationHelper 设计

**核心功能**:
1. `sendApplicationNotification(User, Book)`: 发送借阅申请通知
2. `sendApprovalNotification(User, BorrowRecord)`: 发送审批通知
3. `sendReturnNotification(User, BorrowRecord)`: 发送归还通知
4. `sendRenewNotification(User, BorrowRecord)`: 发送续借通知
5. `sendOverdueNotification(User, BorrowRecord)`: 发送逾期通知

**优势**:
- 统一的空值检查：避免 NPE
- 统一的日志记录：便于追踪
- 易于扩展：可以添加更多通知类型

---

## 📈 代码质量改进

### 重复代码消除

**重构前** (权限验证重复3次):
```java
// returnBook 方法
if (!record.getUserId().equals(userId)) {
    throw new IllegalArgumentException("无权归还他人的借阅记录");
}

// renewBorrow 方法
if (!record.getUserId().equals(userId)) {
    throw new IllegalArgumentException("无权续借他人的借阅记录");
}

// confirmPickup 方法
if (!record.getUserId().equals(userId)) {
    throw new IllegalArgumentException("无权操作他人的借阅记录");
}
```

**重构后** (统一调用):
```java
validator.validateUserPermission(record.getUserId(), userId, "归还");
validator.validateUserPermission(record.getUserId(), userId, "续借");
validator.validateUserPermission(record.getUserId(), userId, "操作");
```

### 通知发送简化

**重构前**:
```java
User user = userService.getUserById(record.getUserId());
if (user != null) {
    notificationService.sendReturnNotification(user, record);
} else {
    log.error("用户 {} 不存在，无法发送归还通知", record.getUserId());
}
```

**重构后**:
```java
User user = userService.getUserById(record.getUserId());
notificationHelper.sendReturnNotification(user, record);
```

### 逾期计算封装

**重构前**:
```java
LocalDateTime now = LocalDateTime.now();
if (now.isAfter(record.getDueDate())) {
    long overdueDays = ChronoUnit.DAYS.between(record.getDueDate().toLocalDate(), now.toLocalDate());
    if (overdueDays <= 0) {
        overdueDays = 1;
    }
    record.setOverdueDays((int) overdueDays);
    record.setFineAmount(FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays)));
}
```

**重构后**:
```java
BorrowValidator.OverdueInfo overdueInfo = validator.calculateOverdue(record.getDueDate());
if (overdueInfo.isOverdue()) {
    record.setOverdueDays(overdueInfo.getDays());
    record.setFineAmount(overdueInfo.getFineAmount());
}
```

---

## 🧪 测试建议

### 单元测试

**BorrowValidator 测试**:
```java
@Test
void testValidateCanBorrow_Success() {
    BorrowValidator validator = new BorrowValidator();
    // 应该通过
    validator.validateCanBorrow(4);
}

@Test
void testValidateCanBorrow_Fail() {
    BorrowValidator validator = new BorrowValidator();
    // 应该抛出异常
    assertThrows(IllegalArgumentException.class, () -> {
        validator.validateCanBorrow(5);
    });
}

@Test
void testCalculateOverdue() {
    BorrowValidator validator = new BorrowValidator();
    LocalDateTime dueDate = LocalDateTime.now().minusDays(3);
    
    OverdueInfo info = validator.calculateOverdue(dueDate);
    
    assertTrue(info.isOverdue());
    assertEquals(3, info.getDays());
    assertEquals(new BigDecimal("1.50"), info.getFineAmount());
}
```

**BorrowConverter 测试**:
```java
@Test
void testToResponse() {
    BorrowConverter converter = new BorrowConverter();
    BorrowRecord record = createTestRecord();
    
    BorrowResponse response = converter.toResponse(record);
    
    assertEquals(record.getId(), response.getId());
    assertEquals(record.getBookTitle(), response.getBookTitle());
    // ... 更多断言
}
```

**BorrowNotificationHelper 测试**:
```java
@Test
void testSendReturnNotification_WithUser() {
    NotificationService mockService = mock(NotificationService.class);
    BorrowNotificationHelper helper = new BorrowNotificationHelper(mockService);
    
    User user = createTestUser();
    BorrowRecord record = createTestRecord();
    
    helper.sendReturnNotification(user, record);
    
    verify(mockService).sendReturnNotification(user, record);
}

@Test
void testSendReturnNotification_WithoutUser() {
    NotificationService mockService = mock(NotificationService.class);
    BorrowNotificationHelper helper = new BorrowNotificationHelper(mockService);
    
    helper.sendReturnNotification(null, createTestRecord());
    
    // 不应该调用 notificationService
    verify(mockService, never()).sendReturnNotification(any(), any());
}
```

---

## 🔄 迁移指南

### 对现有代码的影响

**无需修改**:
- ✅ 所有 Controller 调用
- ✅ 所有测试代码（如果使用 Mock）
- ✅ 配置文件

**需要注意**:
- ⚠️ 新增的辅助类会自动注册为 Spring Bean
- ⚠️ 业务常量从 BorrowService 移至 BorrowValidator

### 部署步骤

1. **编译项目**:
   ```bash
   cd backend
   mvn clean compile
   ```

2. **运行测试**:
   ```bash
   mvn test
   ```

3. **打包部署**:
   ```bash
   mvn clean package
   ```

---

## 📝 后续优化建议

### 短期优化 (1-2周)

1. **添加单元测试**
   - BorrowValidator 完整测试覆盖
   - BorrowConverter 测试
   - BorrowNotificationHelper 测试

2. **添加集成测试**
   - 完整借阅流程测试
   - 并发场景测试
   - 边界条件测试

### 中期优化 (1-2月)

1. **进一步拆分**
   - 考虑将审批逻辑独立为 BorrowApprovalService
   - 考虑将逾期检查独立为 OverdueCheckService

2. **事件驱动**
   - 使用 Spring Events 解耦通知发送
   - 使用消息队列处理异步通知

### 长期优化 (3-6月)

1. **状态机模式**
   - 使用状态机管理借阅状态转换
   - 更清晰的状态流转规则

2. **策略模式**
   - 不同类型用户的借阅策略
   - 不同类型书籍的借阅规则

---

## ✅ 重构检查清单

- [x] 创建 BorrowValidator 验证器
- [x] 创建 BorrowConverter 转换器
- [x] 创建 BorrowNotificationHelper 通知助手
- [x] 重构 BorrowService 使用新辅助类
- [x] 消除重复代码
- [x] 统一业务常量管理
- [x] 代码行数减少 19%
- [ ] 添加单元测试
- [ ] 添加集成测试
- [ ] 性能基准测试
- [ ] 文档更新

---

## 📚 相关文档

- [REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md) - RateLimitFilter 重构总结
- [ARCHITECTURE_ANALYSIS.md](../docs/ARCHITECTURE_ANALYSIS.md) - 系统架构分析
- [DEVELOPMENT_GUIDE.md](../docs/DEVELOPMENT_GUIDE.md) - 开发指南

---

**重构完成！** 🎉

BorrowService 代码质量显著提升，职责更加清晰，可维护性和可测试性大幅改善。
