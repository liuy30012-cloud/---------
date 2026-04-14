# i18n 全面国际化改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将所有硬编码中文文本替换为 vue-i18n 翻译键，实现完整的中英文切换功能。

**Architecture:** 将现有 `zh.json`/`en.json` 拆分为按模块的 TypeScript 文件（`locales/modules/*.ts`），每个模块导出 `{ zh, en }` 对象。由 `locales/index.ts` 合并后供 `i18n.ts` 使用。所有组件统一使用 `useI18n()` + `t('key')` 模式，消除三元表达式硬编码。语言偏好持久化到 localStorage。

**Tech Stack:** Vue 3 Composition API, vue-i18n (legacy: false), TypeScript

---

## File Structure

### 新建文件

| 文件 | 职责 |
|------|------|
| `src/locales/index.ts` | 合并所有模块，导出 `{ zh, en }` messages 对象 |
| `src/locales/modules/common.ts` | 共用文本：通用按钮、状态、时间、标签 |
| `src/locales/modules/nav.ts` | 导航栏文本（含用户菜单） |
| `src/locales/modules/hero.ts` | 首页英雄区域文本 |
| `src/locales/modules/filters.ts` | 筛选器文本 |
| `src/locales/modules/catalog.ts` | 目录/书籍网格文本 |
| `src/locales/modules/related.ts` | 相关卷宗文本 |
| `src/locales/modules/notifications.ts` | 通知面板文本 |
| `src/locales/modules/history.ts` | 搜索历史文本 |
| `src/locales/modules/footer.ts` | 页脚文本 |
| `src/locales/modules/notFound.ts` | 404 页面文本 |
| `src/locales/modules/login.ts` | 登录/注册文本 |
| `src/locales/modules/forgotPassword.ts` | 忘记密码文本 |
| `src/locales/modules/bookSearch.ts` | 馆藏检索页文本 |
| `src/locales/modules/bookDetail.ts` | 图书详情页文本 |
| `src/locales/modules/dashboard.ts` | 运营总览文本 |
| `src/locales/modules/myBorrows.ts` | 我的借阅文本 |
| `src/locales/modules/myReservations.ts` | 我的预约文本 |
| `src/locales/modules/myBookshelf.ts` | 我的书架文本 |
| `src/locales/modules/myAccount.ts` | 我的账号文本 |
| `src/locales/modules/damageReports.ts` | 损坏报告文本 |
| `src/locales/modules/purchaseSuggestions.ts` | 采购心愿池文本 |
| `src/locales/modules/inventoryAlerts.ts` | 库存预警文本 |
| `src/locales/modules/quickActions.ts` | 快捷操作面板文本 |
| `src/locales/modules/offlineIndicator.ts` | 离线指示器文本 |
| `src/locales/modules/cacheManagement.ts` | 缓存管理文本 |
| `src/locales/modules/captcha.ts` | 验证码弹窗文本 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `src/i18n.ts` | 导入新 index.ts，添加 localStorage 持久化 |
| `src/components/navigation/TopNav.vue` | 消除三元表达式，统一用 t() |
| `src/components/home/HeroSection.vue` | 消除三元表达式，统一用 t() |
| `src/components/home/RelatedVolumes.vue` | 消除三元表达式，统一用 t() |
| `src/views/ForgotPassword.vue` | 接入已有 forgotPassword 翻译键 |
| `src/views/BookSearch.vue` | 全部硬编码改为 t() |
| `src/views/BookDetail.vue` | 全部硬编码改为 t() |
| `src/views/Dashboard.vue` | 全部硬编码改为 t() |
| `src/views/MyBorrows.vue` | 全部硬编码改为 t() |
| `src/views/MyReservations.vue` | 全部硬编码改为 t() |
| `src/views/MyBookshelf.vue` | 全部硬编码改为 t() |
| `src/views/MyAccount.vue` | 全部硬编码改为 t() |
| `src/views/DamageReports.vue` | 全部硬编码改为 t() |
| `src/views/PurchaseSuggestions.vue` | 全部硬编码改为 t() |
| `src/views/InventoryAlerts.vue` | 全部硬编码改为 t() |
| `src/components/login/LoginAuthCard.vue` | 全部硬编码改为 t() |
| `src/components/damage/DamageReportModal.vue` | 全部硬编码改为 t() |
| `src/components/panels/QuickActionPanel.vue` | 全部硬编码改为 t() |
| `src/components/panels/QuickRenewModal.vue` | 全部硬编码改为 t() |
| `src/components/common/OfflineIndicator.vue` | 全部硬编码改为 t() |
| `src/components/common/CaptchaChallengeModal.vue` | 全部硬编码改为 t() |
| `src/components/settings/CacheManagement.vue` | 全部硬编码改为 t() |

### 删除文件

| 文件 | 原因 |
|------|------|
| `src/locales/zh.json` | 被 modules 目录替代 |
| `src/locales/en.json` | 被 modules 目录替代 |

---

## Task 1: 创建翻译模块文件 — 基础架构 + 首页已有模块

**Files:**
- Create: `src/locales/modules/common.ts`
- Create: `src/locales/modules/nav.ts`
- Create: `src/locales/modules/hero.ts`
- Create: `src/locales/modules/filters.ts`
- Create: `src/locales/modules/catalog.ts`
- Create: `src/locales/modules/related.ts`
- Create: `src/locales/modules/notifications.ts`
- Create: `src/locales/modules/history.ts`
- Create: `src/locales/modules/footer.ts`
- Create: `src/locales/modules/notFound.ts`
- Create: `src/locales/modules/forgotPassword.ts`
- Create: `src/locales/index.ts`
- Modify: `src/i18n.ts`

- [ ] **Step 1: 创建 common.ts** — 包含跨组件复用的文本

```typescript
// src/locales/modules/common.ts
export default {
  zh: {
    button: {
      confirm: '确认',
      cancel: '取消',
      save: '保存',
      delete: '删除',
      close: '关闭',
      submit: '提交',
      refresh: '刷新',
      loading: '加载中...',
      submitting: '提交中...',
      back: '返回',
      export: '导出',
      update: '更新',
      search: '搜索',
      reset: '重置',
    },
    status: {
      pending: '待审批',
      approved: '已批准',
      borrowed: '借阅中',
      returned: '已归还',
      overdue: '已逾期',
      rejected: '未通过',
      completed: '已完成',
      cancelled: '已取消',
      expired: '已过期',
      processing: '处理中',
      available: '可借',
      unavailable: '不可借',
    },
    label: {
      all: '全部',
      noData: '暂无数据',
      previousPage: '上一页',
      nextPage: '下一页',
      page: '第 {current} / {total} 页',
      results: '{count} 条结果',
      noMatch: '没有找到匹配结果',
      error: '操作失败',
      success: '操作成功',
      networkError: '网络异常，请检查网络连接后重试。',
    },
    time: {
      justNow: '刚刚',
      minutesAgo: '{n} 分钟前',
      hoursAgo: '{n} 小时前',
      daysAgo: '{n} 天前',
      yesterday: '昨天',
      unknown: '未知',
    },
    readingStatus: {
      wantToRead: '想读',
      reading: '在读',
      read: '已读',
      markStatus: '标记阅读状态',
      markReadingStatus: '标记状态',
    },
    circulation: {
      referenceOnly: '馆内阅览',
      manual: '人工审批',
      auto: '自动审批',
    },
    favorite: {
      add: '收藏',
      remove: '取消收藏',
      added: '已收藏',
      addFavorite: '添加收藏',
      removeFavorite: '取消收藏',
    },
  },
  en: {
    button: {
      confirm: 'Confirm',
      cancel: 'Cancel',
      save: 'Save',
      delete: 'Delete',
      close: 'Close',
      submit: 'Submit',
      refresh: 'Refresh',
      loading: 'Loading...',
      submitting: 'Submitting...',
      back: 'Back',
      export: 'Export',
      update: 'Update',
      search: 'Search',
      reset: 'Reset',
    },
    status: {
      pending: 'Pending',
      approved: 'Approved',
      borrowed: 'Borrowed',
      returned: 'Returned',
      overdue: 'Overdue',
      rejected: 'Rejected',
      completed: 'Completed',
      cancelled: 'Cancelled',
      expired: 'Expired',
      processing: 'Processing',
      available: 'Available',
      unavailable: 'Unavailable',
    },
    label: {
      all: 'All',
      noData: 'No data',
      previousPage: 'Previous',
      nextPage: 'Next',
      page: 'Page {current} / {total}',
      results: '{count} results',
      noMatch: 'No matching results found',
      error: 'Operation failed',
      success: 'Operation successful',
      networkError: 'Network error. Please check your connection and try again.',
    },
    time: {
      justNow: 'Just now',
      minutesAgo: '{n} min ago',
      hoursAgo: '{n} hr ago',
      daysAgo: '{n} days ago',
      yesterday: 'Yesterday',
      unknown: 'Unknown',
    },
    readingStatus: {
      wantToRead: 'Want to Read',
      reading: 'Reading',
      read: 'Read',
      markStatus: 'Mark reading status',
      markReadingStatus: 'Mark status',
    },
    circulation: {
      referenceOnly: 'Reference Only',
      manual: 'Manual Approval',
      auto: 'Auto Approval',
    },
    favorite: {
      add: 'Favorite',
      remove: 'Unfavorite',
      added: 'Favorited',
      addFavorite: 'Add to favorites',
      removeFavorite: 'Remove from favorites',
    },
  },
}
```

- [ ] **Step 2: 创建其余首页已有模块文件** — 将 `zh.json` / `en.json` 中现有键迁移到对应模块文件

每个模块文件格式统一为 `export default { zh: {...}, en: {...} }`。现有键从 `zh.json`/`en.json` 原样迁移，不做修改。涉及文件：`nav.ts`, `hero.ts`, `filters.ts`, `catalog.ts`, `related.ts`, `notifications.ts`, `history.ts`, `footer.ts`, `notFound.ts`, `forgotPassword.ts`。

- [ ] **Step 3: 创建 index.ts** — 合并所有模块

```typescript
// src/locales/index.ts
import common from './modules/common'
import nav from './modules/nav'
import hero from './modules/hero'
import filters from './modules/filters'
import catalog from './modules/catalog'
import related from './modules/related'
import notifications from './modules/notifications'
import history from './modules/history'
import footer from './modules/footer'
import notFound from './modules/notFound'
import forgotPassword from './modules/forgotPassword'
import login from './modules/login'
import bookSearch from './modules/bookSearch'
import bookDetail from './modules/bookDetail'
import dashboard from './modules/dashboard'
import myBorrows from './modules/myBorrows'
import myReservations from './modules/myReservations'
import myBookshelf from './modules/myBookshelf'
import myAccount from './modules/myAccount'
import damageReports from './modules/damageReports'
import purchaseSuggestions from './modules/purchaseSuggestions'
import inventoryAlerts from './modules/inventoryAlerts'
import quickActions from './modules/quickActions'
import offlineIndicator from './modules/offlineIndicator'
import cacheManagement from './modules/cacheManagement'
import captcha from './modules/captcha'

function merge(modules: Record<string, any>[]) {
  const result: Record<string, any> = {}
  for (const mod of modules) {
    for (const locale of ['zh', 'en'] as const) {
      if (!result[locale]) result[locale] = {}
      Object.assign(result[locale], mod[locale])
    }
  }
  return result as { zh: Record<string, any>; en: Record<string, any> }
}

const messages = merge([
  common, nav, hero, filters, catalog, related, notifications,
  history, footer, notFound, forgotPassword, login, bookSearch,
  bookDetail, dashboard, myBorrows, myReservations, myBookshelf,
  myAccount, damageReports, purchaseSuggestions, inventoryAlerts,
  quickActions, offlineIndicator, cacheManagement, captcha,
])

export default messages
```

- [ ] **Step 4: 修改 i18n.ts** — 使用新模块 + localStorage 持久化

```typescript
// src/i18n.ts
import { createI18n } from 'vue-i18n'
import messages from './locales'

const getBrowserLang = (): string => {
  const stored = localStorage.getItem('locale')
  if (stored === 'zh' || stored === 'en') return stored
  const nav = navigator as Navigator & { userLanguage?: string }
  const browserLang = navigator.language || nav.userLanguage
  if (browserLang && browserLang.toLowerCase().includes('zh')) {
    return 'zh'
  }
  return 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: getBrowserLang(),
  fallbackLocale: 'en',
  messages,
})

export default i18n
```

- [ ] **Step 5: Commit**

```bash
git add src/locales/ src/i18n.ts
git commit -m "refactor: 重构 i18n 翻译文件为模块化结构并添加 localStorage 持久化"
```

---

## Task 2: 创建新增翻译模块文件

**Files:**
- Create: `src/locales/modules/login.ts`
- Create: `src/locales/modules/bookSearch.ts`
- Create: `src/locales/modules/bookDetail.ts`
- Create: `src/locales/modules/dashboard.ts`
- Create: `src/locales/modules/myBorrows.ts`
- Create: `src/locales/modules/myReservations.ts`
- Create: `src/locales/modules/myBookshelf.ts`
- Create: `src/locales/modules/myAccount.ts`
- Create: `src/locales/modules/damageReports.ts`
- Create: `src/locales/modules/purchaseSuggestions.ts`
- Create: `src/locales/modules/inventoryAlerts.ts`
- Create: `src/locales/modules/quickActions.ts`
- Create: `src/locales/modules/offlineIndicator.ts`
- Create: `src/locales/modules/cacheManagement.ts`
- Create: `src/locales/modules/captcha.ts`

- [ ] **Step 1: 创建 login.ts**

从 `LoginAuthCard.vue` 中提取所有硬编码中文文本。包含：登录/注册标签、表单字段名、按钮文本、错误/成功提示、键盘提示等。

```typescript
// src/locales/modules/login.ts
export default {
  zh: {
    login: {
      brandName: '中国劳动关系学院图书馆',
      tabLogin: '登录',
      tabRegister: '注册',
      studentId: '学工号',
      username: '用户名',
      loginPassword: '登录密码',
      setPassword: '设置密码',
      confirmPassword: '确认密码',
      email: '邮箱（选填）',
      phone: '手机号（选填）',
      rememberMe: '记住本机',
      forgotPassword: '忘记密码',
      enterLibrary: '进入书库',
      createAccount: '创建账户',
      loggingIn: '登录中...',
      submitting: '提交中...',
      enterToLogin: '按 Enter 快速登录',
      portal: '馆务入口',
      showPassword: '显示密码',
      hidePassword: '隐藏密码',
      switchLang: '切换语言',
      copyright: '© 2026 中国劳动关系学院图书馆',
      schoolBadgeAlt: '中国劳动关系学院校徽',
      passwordStrength: {
        weak: '弱',
        fair: '一般',
        good: '良好',
        strong: '强',
      },
    },
  },
  en: {
    login: {
      brandName: 'China University of Labor Relations Library',
      tabLogin: 'Sign In',
      tabRegister: 'Register',
      studentId: 'Student/Staff ID',
      username: 'Username',
      loginPassword: 'Password',
      setPassword: 'Set Password',
      confirmPassword: 'Confirm Password',
      email: 'Email (optional)',
      phone: 'Phone (optional)',
      rememberMe: 'Remember me',
      forgotPassword: 'Forgot password',
      enterLibrary: 'Enter Library',
      createAccount: 'Create Account',
      loggingIn: 'Signing in...',
      submitting: 'Submitting...',
      enterToLogin: 'Press Enter to sign in',
      portal: 'Quick Access',
      showPassword: 'Show password',
      hidePassword: 'Hide password',
      switchLang: 'Switch language',
      copyright: '© 2026 China University of Labor Relations Library',
      schoolBadgeAlt: 'China University of Labor Relations Badge',
      passwordStrength: {
        weak: 'Weak',
        fair: 'Fair',
        good: 'Good',
        strong: 'Strong',
      },
    },
  },
}
```

- [ ] **Step 2: 创建 bookSearch.ts** — 从 `BookSearch.vue` 提取所有文本

- [ ] **Step 3: 创建 bookDetail.ts** — 从 `BookDetail.vue` 提取所有文本

- [ ] **Step 4: 创建 dashboard.ts** — 从 `Dashboard.vue` 提取所有文本

- [ ] **Step 5: 创建 myBorrows.ts** — 从 `MyBorrows.vue` 提取所有文本

- [ ] **Step 6: 创建 myReservations.ts** — 从 `MyReservations.vue` 提取所有文本

- [ ] **Step 7: 创建 myBookshelf.ts** — 从 `MyBookshelf.vue` 提取所有文本

- [ ] **Step 8: 创建 myAccount.ts** — 从 `MyAccount.vue` 提取所有文本

- [ ] **Step 9: 创建 damageReports.ts** — 从 `DamageReports.vue` 和 `DamageReportModal.vue` 提取所有文本

- [ ] **Step 10: 创建 purchaseSuggestions.ts** — 从 `PurchaseSuggestions.vue` 提取所有文本

- [ ] **Step 11: 创建 inventoryAlerts.ts** — 从 `InventoryAlerts.vue` 提取所有文本

- [ ] **Step 12: 创建 quickActions.ts** — 从 `QuickActionPanel.vue` 和 `QuickRenewModal.vue` 提取所有文本

- [ ] **Step 13: 创建 offlineIndicator.ts** — 从 `OfflineIndicator.vue` 提取所有文本

- [ ] **Step 14: 创建 cacheManagement.ts** — 从 `CacheManagement.vue` 提取所有文本

- [ ] **Step 15: 创建 captcha.ts** — 从 `CaptchaChallengeModal.vue` 提取所有文本

- [ ] **Step 16: Commit**

```bash
git add src/locales/modules/
git commit -m "feat: 添加所有页面的中英文翻译键"
```

---

## Task 3: 改造首页相关组件 — TopNav, HeroSection, RelatedVolumes

**Files:**
- Modify: `src/components/navigation/TopNav.vue`
- Modify: `src/components/home/HeroSection.vue`
- Modify: `src/components/home/RelatedVolumes.vue`

- [ ] **Step 1: 改造 TopNav.vue** — 消除所有 `locale === 'zh' ? ... : ...` 三元表达式

在 `<script setup>` 中添加 `const { t } = useI18n()` (已有 `locale`)。
在 `toggleLang` 中添加 `localStorage.setItem('locale', locale.value)`。
替换模板和脚本中的所有三元表达式为 `t()` 调用：

- `locale === 'zh' ? '馆藏展陈系统' : 'Archive Exhibition System'` → `t('nav.systemName')`
- `locale === 'zh' ? '打开通知面板' : 'Open notifications'` → `t('nav.openNotifications')`
- `locale === 'zh' ? '打开搜索历史' : 'Open search history'` → `t('nav.openSearchHistory')`
- `locale === 'zh' ? '登录/注册' : 'Sign In'` → `t('nav.signIn')`
- `locale === 'zh' ? '我的账号' : 'My Account'` → `t('nav.myAccount')`
- `locale === 'zh' ? '我的书架' : 'My Bookshelf'` → `t('nav.myBookshelf')`
- `locale === 'zh' ? '退出登录' : 'Sign Out'` → `t('nav.signOut')`
- `locale === 'zh' ? '切换语言' : 'Switch language'` → `t('nav.switchLang')`
- 等等...

同时更新 `navItems` 中的 `label` 改为 `t()` 调用：
```typescript
const navItems = computed(() => [
  { name: 'Home', to: '/', label: t('nav.home') },
  { name: 'BookSearch', to: '/books/search', label: t('nav.search') },
  { name: 'MyBorrows', to: '/my-borrows', label: t('nav.borrows') },
  // ...
])
```

- [ ] **Step 2: 改造 HeroSection.vue** — 消除所有 `locale === 'zh' ? ... : ...` 三元表达式

替换模板中所有硬编码的三元表达式为 `t()` 调用。`trendingTags` 的 computed 改为使用翻译键。

- [ ] **Step 3: 改造 RelatedVolumes.vue** — 消除所有 `locale === 'zh' ? ... : ...` 三元表达式

- [ ] **Step 4: Commit**

```bash
git add src/components/navigation/TopNav.vue src/components/home/HeroSection.vue src/components/home/RelatedVolumes.vue
git commit -m "feat: 改造首页组件统一使用 i18n t() 函数"
```

---

## Task 4: 改造登录相关组件 — LoginAuthCard, ForgotPassword

**Files:**
- Modify: `src/components/login/LoginAuthCard.vue`
- Modify: `src/views/ForgotPassword.vue`

- [ ] **Step 1: 改造 LoginAuthCard.vue**

添加 `import { useI18n } from 'vue-i18n'` 和 `const { t } = useI18n()`。
替换模板中所有硬编码中文文本为 `t('login.xxx')` 调用。

- [ ] **Step 2: 改造 ForgotPassword.vue**

添加 `import { useI18n } from 'vue-i18n'` 和 `const { t } = useI18n()`。
替换模板中所有硬编码中文为 `t('forgotPassword.xxx')` 调用（翻译键已存在于 `forgotPassword.ts`）。

- [ ] **Step 3: Commit**

```bash
git add src/components/login/LoginAuthCard.vue src/views/ForgotPassword.vue
git commit -m "feat: 改造登录和忘记密码组件使用 i18n"
```

---

## Task 5: 改造搜索与详情页 — BookSearch, BookDetail

**Files:**
- Modify: `src/views/BookSearch.vue`
- Modify: `src/views/BookDetail.vue`

- [ ] **Step 1: 改造 BookSearch.vue**

添加 `const { t } = useI18n()`。
替换模板中所有硬编码中文：
- `title="馆藏检索"` → `:title="t('bookSearch.title')"`
- 所有 label、placeholder、按钮文本
- `querySummary` 计算属性中的 `'全部馆藏'`
- `circulationLabel()` 函数中的硬编码
- 错误消息和 toast 消息

- [ ] **Step 2: 改造 BookDetail.vue**

添加 `const { t } = useI18n()`。
替换模板和脚本中所有硬编码中文文本。
`borrowButtonText` computed 改为使用 `t()` 调用。
`circulationLabel()` 和 `statusLabel()` 改为使用 `t()` 调用。
所有 toast 消息中的硬编码。

- [ ] **Step 3: Commit**

```bash
git add src/views/BookSearch.vue src/views/BookDetail.vue
git commit -m "feat: 改造图书搜索和详情页使用 i18n"
```

---

## Task 6: 改造管理页面 — Dashboard, InventoryAlerts

**Files:**
- Modify: `src/views/Dashboard.vue`
- Modify: `src/views/InventoryAlerts.vue`

- [ ] **Step 1: 改造 Dashboard.vue**

替换所有硬编码中文：统计标签（馆藏总量、当前可借...）、图表标题、按钮文本。

- [ ] **Step 2: 改造 InventoryAlerts.vue**

替换所有硬编码中文：摘要卡片标签、统计项标签、预警级别文本、空状态文本。

- [ ] **Step 3: Commit**

```bash
git add src/views/Dashboard.vue src/views/InventoryAlerts.vue
git commit -m "feat: 改造管理页面使用 i18n"
```

---

## Task 7: 改造个人页面 — MyBorrows, MyReservations, MyBookshelf, MyAccount

**Files:**
- Modify: `src/views/MyBorrows.vue`
- Modify: `src/views/MyReservations.vue`
- Modify: `src/views/MyBookshelf.vue`
- Modify: `src/views/MyAccount.vue`

- [ ] **Step 1: 改造 MyBorrows.vue**

替换所有硬编码：PageHeader 标题、tab 标签、状态映射 (`statusText()`)、下一步映射 (`nextActionText()`)、详情网格标签、按钮文本、dialog 文本、toast 消息、空状态文本。

- [ ] **Step 2: 改造 MyReservations.vue**

替换所有硬编码：标题、状态映射 (`statusText()`)、详情标签、按钮文本、dialog 文本、过期警告、toast 消息、空状态。

- [ ] **Step 3: 改造 MyBookshelf.vue**

替换所有硬编码：标题、tab 标签 (`tabs` computed)、状态映射 (`statusLabel()`)、按钮文本、dialog 文本、空状态消息、toast 消息。

- [ ] **Step 4: 改造 MyAccount.vue**

替换所有硬编码：标题、个人资料标签（邮箱/手机/角色）、借阅画像标签、密码修改表单、提醒偏好、数据导出区域、toast 消息。

- [ ] **Step 5: Commit**

```bash
git add src/views/MyBorrows.vue src/views/MyReservations.vue src/views/MyBookshelf.vue src/views/MyAccount.vue
git commit -m "feat: 改造个人中心页面使用 i18n"
```

---

## Task 8: 改造报告与采购页面 — DamageReports, PurchaseSuggestions

**Files:**
- Modify: `src/views/DamageReports.vue`
- Modify: `src/views/PurchaseSuggestions.vue`

- [ ] **Step 1: 改造 DamageReports.vue**

替换所有硬编码：统计卡片标签、筛选标签、状态映射、损坏类型映射、详情弹窗标签、按钮文本、分页文本、空状态。

- [ ] **Step 2: 改造 PurchaseSuggestions.vue**

替换所有硬编码：标题、摘要标签、流程说明、表单标签、请求列表文本、投票按钮、管理员控制区、算法建议区、分页文本。

- [ ] **Step 3: Commit**

```bash
git add src/views/DamageReports.vue src/views/PurchaseSuggestions.vue
git commit -m "feat: 改造损坏报告和采购建议页面使用 i18n"
```

---

## Task 9: 改造子组件 — DamageReportModal, QuickActionPanel, QuickRenewModal, OfflineIndicator, CaptchaChallengeModal, CacheManagement

**Files:**
- Modify: `src/components/damage/DamageReportModal.vue`
- Modify: `src/components/panels/QuickActionPanel.vue`
- Modify: `src/components/panels/QuickRenewModal.vue`
- Modify: `src/components/common/OfflineIndicator.vue`
- Modify: `src/components/common/CaptchaChallengeModal.vue`
- Modify: `src/components/settings/CacheManagement.vue`

- [ ] **Step 1: 改造 DamageReportModal.vue**

替换：标题、表单标签、损坏类型选项、按钮文本、错误消息。

- [ ] **Step 2: 改造 QuickActionPanel.vue**

替换：按钮文本、面板标题、分组标题、操作项文本、页脚提示。

- [ ] **Step 3: 改造 QuickRenewModal.vue**

替换：标题、加载/空状态文本、按钮文本、页脚提示、toast 消息。

- [ ] **Step 4: 改造 OfflineIndicator.vue**

替换：离线模式文本、提示文本、通知标题/消息/按钮。

- [ ] **Step 5: 改造 CaptchaChallengeModal.vue**

替换：标题、说明文本、按钮文本、加载/错误文本。

- [ ] **Step 6: 改造 CacheManagement.vue**

替换：标题、统计标签、按钮文本、说明列表、时间文本、toast 消息。

- [ ] **Step 7: Commit**

```bash
git add src/components/damage/DamageReportModal.vue src/components/panels/QuickActionPanel.vue src/components/panels/QuickRenewModal.vue src/components/common/OfflineIndicator.vue src/components/common/CaptchaChallengeModal.vue src/components/settings/CacheManagement.vue
git commit -m "feat: 改造所有子组件使用 i18n"
```

---

## Task 10: 清理旧文件 + 验证

**Files:**
- Delete: `src/locales/zh.json`
- Delete: `src/locales/en.json`

- [ ] **Step 1: 删除旧的 JSON 翻译文件**

确认所有翻译键已迁移到模块文件后，删除 `zh.json` 和 `en.json`。

- [ ] **Step 2: 运行前端构建验证**

```bash
cd frontend && npm run build
```

确保编译无错误。

- [ ] **Step 3: 人工测试**

在浏览器中：
1. 确认默认中文显示正常
2. 切换到英文，确认所有页面文本均变为英文
3. 刷新页面，确认语言偏好保持
4. 切换回中文，确认正常

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: 删除旧的 JSON 翻译文件，完成 i18n 全面改造"
```
