# 图书损坏报告功能设计规格

## Context

图书馆书籍定位系统目前缺少图书损坏反馈机制。读者发现破损书籍后无法上报问题，管理员也无法系统性地追踪和处理损坏书籍。本功能旨在添加完整的"报告问题"流程：读者提交损坏报告（含照片和分类），管理员处理报告并自动更新书籍状态，同时通过通知系统推送进度。

---

## 需求总结

| 项目 | 决定 |
|------|------|
| 谁可以报告 | 所有已登录用户（STUDENT / TEACHER / ADMIN） |
| 管理员操作 | 标记状态（处理中/已修复/已驳回）、自动更新书籍状态、删除报告、统计仪表盘 |
| 照片存储 | 服务器本地文件系统 `uploads/damage-photos/` |
| 报告内容 | 破损照片（必填，最多3张）、损坏类型分类、文字描述 |
| 进度通知 | 提交后通知管理员，处理完成后通知报告人 |
| 入口 | 书籍详情页内"报告问题"按钮 + 独立"损坏报告中心"页面 |
| 架构方案 | 方案A：独立损坏报告模块（完全解耦） |

---

## 数据模型

### DamageReport 实体

新建表 `damage_reports`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK, 自增) | 主键 |
| `bookId` | Long (FK → books) | 关联书籍 |
| `bookTitle` | String(200) | 书名冗余（避免频繁联表） |
| `reporterId` | Long (FK → users) | 报告人 |
| `reporterName` | String(50) | 报告人姓名冗余 |
| `damageTypes` | String(200) | 损坏类型，逗号分隔（COVER_TORN, PAGE_MISSING, WATER_DAMAGE, GRAFFITI, BINDING_BROKEN, OTHER） |
| `description` | TEXT | 文字描述 |
| `photoUrls` | TEXT | 照片URL列表，逗号分隔（最多3张） |
| `status` | String(20) | 状态：PENDING / IN_PROGRESS / RESOLVED / REJECTED |
| `adminNotes` | TEXT | 管理员处理备注 |
| `resolvedBy` | Long (FK → users) | 处理管理员ID |
| `resolvedByName` | String(50) | 处理管理员姓名冗余 |
| `resolvedAt` | LocalDateTime | 处理时间 |
| `createdAt` | LocalDateTime | 创建时间（JPA审计） |
| `updatedAt` | LocalDateTime | 更新时间（JPA审计） |

### 损坏类型枚举

```
COVER_TORN      - 封面破损
PAGE_MISSING    - 页面缺失
WATER_DAMAGE    - 水渍
GRAFFITI        - 涂写
BINDING_BROKEN  - 装订脱落
OTHER           - 其他
```

### 报告状态枚举

```
PENDING      - 待处理
IN_PROGRESS  - 处理中
RESOLVED     - 已修复
REJECTED     - 已驳回
```

---

## 后端 API 设计

### DamageReportController — `/api/damage-reports`

| 方法 | 路由 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/damage-reports` | 已登录用户 | 提交损坏报告（multipart/form-data，含照片文件） |
| GET | `/api/damage-reports` | 已登录用户 | 获取我的报告列表（分页） |
| GET | `/api/damage-reports/{id}` | 已登录用户 | 获取报告详情（仅报告人或管理员） |
| GET | `/api/damage-reports/admin/all` | 管理员 | 获取全部报告（分页，支持状态过滤） |
| GET | `/api/damage-reports/admin/statistics` | 管理员 | 报告统计数据 |
| PATCH | `/api/damage-reports/admin/{id}/status` | 管理员 | 更新报告状态（自动更新书籍状态） |
| DELETE | `/api/damage-reports/admin/{id}` | 管理员 | 删除报告 |

### 照片上传

- 端点：`POST /api/damage-reports` 使用 `multipart/form-data`
- 存储路径：`uploads/damage-photos/{reportId}_{timestamp}.{ext}`
- 文件限制：单张最大 5MB，仅接受 jpg/png/webp，最多3张
- 返回值中 `photoUrls` 存储相对路径，前端拼接 baseURL

### 书籍状态自动更新规则

管理员处理报告时，`DamageReportService` 根据状态变更联动更新 `Book` 实体：

| 报告状态 → | 书籍状态变更 |
|-----------|------------|
| PENDING → IN_PROGRESS | `book.status = "DAMAGED"` |
| IN_PROGRESS → RESOLVED | `book.status = "AVAILABLE"`（恢复原状态） |
| PENDING → REJECTED | 不变更书籍状态 |

---

## 后端文件结构

### 新建文件

```
backend/src/main/java/com/library/
├── model/DamageReport.java                    # JPA 实体
├── repository/DamageReportRepository.java     # 数据仓库
├── dto/DamageReportRequest.java               # 提交请求 DTO
├── dto/DamageReportResponse.java              # 响应 DTO
├── dto/DamageReportStatistics.java            # 统计 DTO
├── service/DamageReportService.java           # 业务逻辑
├── controller/DamageReportController.java     # REST 控制器

uploads/damage-photos/                          # 照片存储目录
```

### 修改文件

```
backend/src/main/resources/application.yml      # 添加文件上传配置
backend/src/main/java/com/library/config/SecurityConfig.java  # 放行照片静态资源
backend/src/main/java/com/library/service/NotificationService.java  # 添加新通知类型
```

---

## 前端设计

### 配色方案（Bamboo Monastery 主题）

| 元素 | 色值 |
|------|------|
| 页面背景 | `linear-gradient(180deg, rgba(236,241,232,0.96) → rgba(226,234,224,0.98))` |
| 卡片背景 | `linear-gradient(180deg, rgba(249,246,239,0.9) → rgba(239,234,223,0.84))` |
| 卡片边框 | `rgba(110,124,104,0.14)` |
| 主文字 | `#1b2821` (home-ink) |
| 次要文字 | `rgba(57,68,58,0.78)` |
| 标签文字 | `rgba(118,91,56,0.88)` |
| 主按钮 | `linear-gradient(135deg, #d7b37a, #ba8850, #efd0a6)` |
| 提交/强调按钮 | `linear-gradient(135deg, #335c67, #537072)` |
| "报告问题"按钮背景 | `rgba(184,92,56,0.08)`，边框 `rgba(184,92,56,0.2)`，文字 `#8b482f` |
| 待处理药丸 | 背景 `rgba(139,111,71,0.14)`，文字 `#7c633e` |
| 处理中药丸 | 背景 `rgba(60,110,113,0.14)`，文字 `#315f63` |
| 已修复药丸 | 背景 `rgba(85,124,85,0.14)`，文字 `#456846` |
| 已驳回药丸 | 背景 `rgba(124,79,79,0.14)`，文字 `#7c4f4f` |
| 类型标签选中 | 背景 `rgba(133,160,131,0.18)`，边框 `rgba(133,160,131,0.32)` |
| 类型标签默认 | 背景 `rgba(255,255,255,0.6)`，边框 `rgba(110,122,102,0.16)` |
| 输入框背景 | `linear-gradient(180deg, rgba(255,252,247,0.92) → rgba(246,241,231,0.88))` |

### 入口1：BookDetail.vue — "报告问题"按钮

在书籍详情页操作按钮区（借阅/预约按钮下方）添加"报告问题"按钮。点击后打开 `DamageReportModal` 弹窗，自动关联当前书籍。

### 入口2：DamageReportModal.vue — 报告表单弹窗

独立弹窗组件，包含：
- 书籍信息展示（标题、作者）
- 损坏类型多选标签（6种类型，点击切换选中状态）
- 照片上传区（最多3张，支持预览和删除，拖拽或点击上传）
- 文字描述输入框（可选，最多500字）
- 取消/提交按钮

### 入口3：DamageReports.vue — 损坏报告中心页面

独立页面，路由 `/damage-reports`，需要登录：
- **普通用户视图**：我的报告列表 + 统计概览
- **管理员视图**：全部报告 + 统计仪表盘 + 状态筛选 + 处理操作
  - 统计卡片：待处理数、处理中数、已修复数、已驳回数
  - 报告列表卡片：左侧色条标识状态 + 书籍封面 + 书名 + 损坏类型 + 状态药丸 + 时间
  - 管理员操作：点击卡片展开详情，可修改状态、添加备注、查看照片

### 前端文件结构

#### 新建文件

```
frontend/src/
├── api/damageReportApi.ts              # API 接口层
├── composables/useDamageReports.ts     # 报告列表逻辑（分页、筛选、统计）
├── components/damage/
│   ├── DamageReportModal.vue           # 报告弹窗表单
│   ├── DamageReportCard.vue            # 报告卡片组件
│   └── DamageReportStats.vue           # 统计卡片组件
├── views/DamageReports.vue             # 报告中心页面
```

#### 修改文件

```
frontend/src/router/index.ts            # 添加 /damage-reports 路由
frontend/src/views/BookDetail.vue       # 添加"报告问题"按钮 + 弹窗引用
frontend/src/components/navigation/TopNav.vue  # 导航栏添加入口（可选）
```

---

## 通知系统联动

在现有 `NotificationService` 中添加新通知类型：

| 类型 | 触发时机 | 接收人 |
|------|---------|--------|
| `DAMAGE_REPORT_SUBMITTED` | 用户提交报告 | 管理员（所有 ADMIN 角色） |
| `DAMAGE_REPORT_IN_PROGRESS` | 管理员开始处理 | 报告人 |
| `DAMAGE_REPORT_RESOLVED` | 管理员标记已修复 | 报告人 |
| `DAMAGE_REPORT_REJECTED` | 管理员驳回报告 | 报告人 |

通知内容格式（双语）：

- `DAMAGE_REPORT_SUBMITTED`：
  - 标题Zh：`新损坏报告`
  - 描述Zh：`《{bookTitle}》被报告存在损坏问题`
  - 图标：`report_problem`
  - 跳转：`/damage-reports`

- `DAMAGE_REPORT_RESOLVED`：
  - 标题Zh：`损坏报告已处理`
  - 描述Zh：`您报告的《{bookTitle}》损坏问题已修复`
  - 图标：`check_circle`
  - 跳转：`/damage-reports`

---

## 验证方式

1. **后端测试**：JUnit 测试 DamageReportService 的 CRUD 和状态流转逻辑
2. **照片上传**：手动测试上传 jpg/png/webp，验证大小限制和数量限制
3. **前端手动验证**：
   - 在书籍详情页点击"报告问题"，提交含照片的报告
   - 在报告中心页面查看报告列表和统计
   - 以管理员身份登录，处理报告，验证书籍状态自动更新
   - 验证通知是否正确推送到管理员和报告人
4. **权限验证**：未登录用户无法提交报告，普通用户只能看到自己的报告，管理员可以看到全部报告
