# i18n 全面国际化改造设计文档

**日期**: 2026-04-14
**状态**: 已批准

## 问题

vue-i18n 已部署但翻译覆盖率极低：约 96 个翻译键仅覆盖首页区域，12 个页面级组件中约 11 个完全硬编码中文。切换到英文后 90% 的页面内容仍然是中文，导致中英混杂的用户体验。

同时存在翻译方式不统一（混用 `$t()` 和三元表达式）、状态映射散落各组件、语言偏好未持久化等问题。

## 方案选择

| 方案 | 描述 | 结论 |
|------|------|------|
| A: 全面改造 | 一次性改造所有组件，统一使用翻译键 | **已选定** |
| B: 移除切换 | 暂时移除语言切换按钮 | 不解决根本问题 |
| C: 渐进式 | 分期完成高频页面 | 引入中间状态复杂度 |

## 设计

### 1. 翻译文件结构重构

将现有 `zh.json` / `en.json` 拆分为按模块的 TypeScript 文件：

```
frontend/src/locales/
  index.ts              # 合并所有模块，导出 messages 对象
  modules/
    nav.ts              # 导航栏
    hero.ts             # 首页英雄区
    filters.ts          # 筛选器
    catalog.ts          # 目录/书籍网格
    related.ts          # 相关卷宗
    notifications.ts    # 通知面板
    history.ts          # 搜索历史
    footer.ts           # 页脚
    notFound.ts         # 404 页面
    forgotPassword.ts   # 忘记密码
    login.ts            # 登录/注册
    bookSearch.ts       # 馆藏检索页
    bookDetail.ts       # 图书详情页
    dashboard.ts        # 运营总览
    myBorrows.ts        # 我的借阅
    myReservations.ts   # 我的预约
    myBookshelf.ts      # 我的书架
    myAccount.ts        # 我的账号
    damageReports.ts    # 损坏报告
    purchaseSuggestions.ts  # 采购心愿池
    inventoryAlerts.ts  # 库存预警
    common.ts           # 共用文本（按钮、状态、时间等）
```

每个模块导出 `{ zh: {...}, en: {...} }`，由 `index.ts` 合并为 vue-i18n 所需格式。

### 2. 组件改造规则

- 统一使用 `useI18n()` 组合式 API：`const { t } = useI18n()`
- 模板中使用 `{{ t('key') }}`
- 消除所有 `locale === 'zh' ? ... : ...` 三元表达式
- 翻译键命名：顶级=模块名，二级=语义分组（status/button/label/message），三级=具体项

### 3. 共用文本抽取到 common 模块

以下重复出现的文本统一到 `common.ts`：

- **通用按钮**: 确认、取消、保存、删除、关闭、提交、刷新、加载中...
- **通用状态**: 待审批、处理中、已完成、已拒绝、已过期...
- **通用标签**: 全部、无数据、搜索、上一页、下一页...
- **时间相关**: 天前、小时前、刚刚...

### 4. 语言偏好持久化

- 初始化时从 `localStorage.getItem('locale')` 读取
- 切换时同步写入 `localStorage`
- 首次访问按浏览器语言自动检测（不变）

### 5. 状态映射统一

散落在各组件中的状态文本映射抽取为翻译键：
- `common.status.pending` → "待审批" / "Pending"
- 各组件通过 `t('common.status.' + status.toLowerCase())` 引用

### 6. 不改动的部分

- 后端 API 不变，纯前端改造
- 视觉设计、布局、样式不变
- 路由结构不变

## 需要改造的组件清单

### 页面级组件 (Views)

| 页面 | 组件 | 当前状态 |
|------|------|----------|
| 馆藏检索 | BookSearch.vue | 全部硬编码 |
| 图书详情 | BookDetail.vue | 全部硬编码 |
| 运营总览 | Dashboard.vue | 全部硬编码 |
| 我的借阅 | MyBorrows.vue | 全部硬编码 |
| 我的预约 | MyReservations.vue | 全部硬编码 |
| 我的书架 | MyBookshelf.vue | 全部硬编码 |
| 我的账号 | MyAccount.vue | 全部硬编码 |
| 损坏报告 | DamageReports.vue | 全部硬编码 |
| 采购心愿池 | PurchaseSuggestions.vue | 全部硬编码 |
| 库存预警 | InventoryAlerts.vue | 全部硬编码 |
| 忘记密码 | ForgotPassword.vue | 有翻译键但未接入 |

### 需改造的子组件

| 组件 | 说明 |
|------|------|
| TopNav.vue | 消除混合模式，统一用 $t() |
| HeroSection.vue | 消除三元表达式 |
| RelatedVolumes.vue | 消除三元表达式 |
| LoginAuthCard.vue | 全部硬编码 |
| DamageReportModal.vue | 全部硬编码 |
| QuickActionPanel.vue | 全部硬编码 |
| QuickRenewModal.vue | 全部硬编码 |
| OfflineIndicator.vue | 全部硬编码 |
| CacheManagement.vue | 全部硬编码 |
| CaptchaChallengeModal.vue | 全部硬编码 |

### 已完成的组件（无需改造）

NotFound.vue、BookGrid.vue、SiteFooter.vue、SidebarFilters.vue、SuperButton.vue、SearchHistoryPanel.vue、NotificationPanel.vue

## 技术决策

- **翻译来源**: 自动生成英文翻译，后续人工微调
- **文件组织**: 按模块拆分 TypeScript 文件（用户选择）
- **语言偏好**: 持久化到 localStorage
