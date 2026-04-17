# Elasticsearch 集成 Bug 修复报告

**日期：** 2026-04-18  
**修复提交：** b72e8c0

---

## 问题概述

在完成 Elasticsearch 集成后进行代码审查时，发现了 3 个严重问题和多个潜在问题。所有严重问题已立即修复。

---

## 修复的严重问题

### 1. NullPointerException 风险 - StatisticsService.java

**问题描述：**
- 在第 138、144、150、156 行使用 `Map.get()` 直接赋值给 `long` 类型
- 如果 Map 中的 key 不存在，会返回 `null`，导致自动拆箱时抛出 `NullPointerException`

**修复方案：**
```java
// 修复前
totalBooks = stats.get("totalCopies");
availableBooks = stats.get("availableCopies");

// 修复后
totalBooks = stats.getOrDefault("totalCopies", 0L);
availableBooks = stats.getOrDefault("availableCopies", 0L);
```

**影响范围：** 4 处修改

---

### 2. IndexOutOfBoundsException 风险 - ElasticsearchStatisticsService.java

**问题描述：**
- 在 `getInventoryStatistics()`、`getTotalCopiesSum()`、`getAvailableCopiesSum()` 方法中
- 直接使用 `aggregations.aggregations().get(0)` 和 `get(1)` 访问聚合结果
- 没有检查列表是否为空或大小是否足够，可能导致 `IndexOutOfBoundsException`

**修复方案：**
```java
// 修复前
if (aggregations != null) {
    var aggList = aggregations.aggregations();
    long totalCopies = (long) aggList.get(0).aggregation().getAggregate().sum().value();
    long availableCopies = (long) aggList.get(1).aggregation().getAggregate().sum().value();
    return Map.of("totalCopies", totalCopies, "availableCopies", availableCopies);
}

// 修复后
if (aggregations != null && !aggregations.aggregations().isEmpty()) {
    var aggList = aggregations.aggregations();
    if (aggList.size() >= 2) {
        long totalCopies = (long) aggList.get(0).aggregation().getAggregate().sum().value();
        long availableCopies = (long) aggList.get(1).aggregation().getAggregate().sum().value();
        return Map.of("totalCopies", totalCopies, "availableCopies", availableCopies);
    }
}
log.warn("Elasticsearch aggregation returned empty or incomplete results");
return Map.of("totalCopies", 0L, "availableCopies", 0L);
```

**影响范围：** 3 个方法修复

---

### 3. 日志格式错误 - ElasticsearchSearchService.java

**问题描述：**
- 在第 153 行，日志记录缺少占位符 `{}`
- 导致 `keyword` 参数和异常 `e` 不会被正确记录到日志中

**修复方案：**
```java
// 修复前
log.error("Failed to search from Elasticsearch for keyword: ", keyword, e);

// 修复后
log.error("Failed to search from Elasticsearch for keyword: {}", keyword, e);
```

**影响范围：** 1 处修改

---

## 潜在问题（已识别，暂未修复）

### 1. 异常处理策略

**问题：** `ElasticsearchStatisticsService` 抛出 `RuntimeException` 可能导致降级逻辑失效

**建议：** 考虑返回默认值而不是抛出异常，或在调用方先检查 `isAvailable()`

**优先级：** 中

---

### 2. 降级逻辑重复执行

**问题：** `StatisticsService` 中先检查 `isAvailable()`，异常时又降级，可能导致重复查询

**建议：** 简化降级逻辑，避免不必要的重复查询

**优先级：** 低

---

### 3. SQL 查询返回 null

**问题：** `BookRepository.sumTotalCopies()` 在表为空时返回 `null`

**现状：** `MysqlStatisticsService` 已有 null 检查，但依赖调用方处理

**优先级：** 低

---

## 测试验证

### 单元测试
- ✅ `ElasticsearchSyncServiceTest` - 通过
- ✅ `StatisticsServiceIntegrationTest` - 通过
- ✅ `FallbackTest` - 通过

### 手动测试建议
1. 测试 Elasticsearch 不可用时的降级功能
2. 测试空数据库的统计查询
3. 测试搜索建议功能

---

## 修复总结

**修复文件：** 3 个
- `StatisticsService.java` - 4 处修改
- `ElasticsearchStatisticsService.java` - 3 个方法修复
- `ElasticsearchSearchService.java` - 1 处修改

**代码变更：**
- 新增：34 行
- 删除：23 行
- 净增：11 行

**提交信息：**
```
commit b72e8c0
fix: 修复 Elasticsearch 集成中的 NPE 和日志格式问题
```

---

## 结论

✅ **所有严重问题已修复**

修复后的代码：
- 消除了 `NullPointerException` 风险
- 消除了 `IndexOutOfBoundsException` 风险
- 修复了日志记录问题
- 提高了系统健壮性和可维护性

潜在问题已记录，可在后续迭代中优化。当前代码可以安全部署使用。
