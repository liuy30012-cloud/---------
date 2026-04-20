---
name: 交互反馈层优化设计
description: 图书馆书籍定位系统交互反馈层重构式升级 - 统一反馈管理、骨架屏系统、错误处理中心、离线管理
type: feature
date: 2026-04-20
---

# 交互反馈层优化设计文档

## 1. 项目概述

### 1.1 背景

当前图书馆书籍定位系统已具备基础的交互反馈功能（FeedbackToast、OfflineIndicator），但存在以下问题：

- Toast 功能单一，不支持操作按钮、堆叠显示
- 缺少统一的加载状态管理和骨架屏
- 错误处理分散，缺乏统一的错误处理机制
- 离线功能基础，缺少数据缓存和离线操作队列

### 1.2 目标

采用**重构式升级方案**，建立统一的交互反馈架构：

- 创建 FeedbackManager 统一管理所有用户反馈
- 建立专用骨架屏系统，覆盖主要页面场景
- 创建 ErrorCenter 集中处理所有错误
- 建立 OfflineManager 统一管理离线功能

### 1.3 技术栈

- Vue 3 + TypeScript
- IndexedDB (离线缓存)
- Service Worker (PWA 支持)
- Axios (HTTP 拦截器)

## 2. 整体架构

### 2.1 核心服务层

四个核心服务构成交互反馈系统的基础：

1. **FeedbackManager** (`services/FeedbackManager.ts`)
   - 统一管理 Toast、通知、加载状态
   - 队列管理、优先级控制、自动清理
   - 单例模式，全局唯一实例

2. **ErrorCenter** (`services/ErrorCenter.ts`)
   - 集中处理所有错误类型
   - 错误分类、错误码映射、友好提示
   - 错误日志收集和上报
   - 自动重试机制（指数退避算法）

3. **OfflineManager** (`services/OfflineManager.ts`)
   - 网络状态监控（在线/离线/慢速）
   - IndexedDB 缓存策略管理
   - 离线操作队列
   - 数据同步协调

4. **SkeletonFactory** (`services/SkeletonFactory.ts`)
   - 骨架屏组件注册和管理
   - 根据页面类型自动选择骨架屏
   - 统一加载状态管理

### 2.2 目录结构

```text
frontend/src/
├── services/
│   ├── FeedbackManager.ts      # 反馈管理器
│   ├── ErrorCenter.ts          # 错误中心
│   ├── OfflineManager.ts       # 离线管理器
│   └── SkeletonFactory.ts      # 骨架屏工厂
├── components/
│   ├── feedback/
│   │   ├── FeedbackToast.vue   # 增强版 Toast（重构）
│   │   ├── ToastContainer.vue  # Toast 容器（堆叠管理）
│   │   └── LoadingOverlay.vue  # 全局加载遮罩
│   ├── skeleton/
│   │   ├── BookCardSkeleton.vue
│   │   ├── BookListSkeleton.vue
│   │   ├── BookDetailSkeleton.vue
│   │   ├── UserProfileSkeleton.vue
│   │   ├── DashboardSkeleton.vue
│   │   ├── TableSkeleton.vue
│   │   ├── FormSkeleton.vue
│   │   └── SearchResultSkeleton.vue
│   ├── error/
│   │   ├── ErrorBoundary.vue   # 错误边界组件
│   │   ├── ErrorPage.vue       # 统一错误页面
│   │   └── ErrorRetry.vue      # 错误重试组件
│   └── offline/
│       ├── OfflineIndicator.vue # 增强版离线指示器
│       └── SyncStatus.vue       # 同步状态显示
├── composables/
│   ├── useFeedback.ts          # 反馈 Hook
│   ├── useError.ts             # 错误处理 Hook
│   ├── useOffline.ts           # 离线管理 Hook
│   └── useSkeleton.ts          # 骨架屏 Hook
└── workers/
    └── service-worker.ts       # Service Worker
```

## 3. FeedbackManager 反馈管理系统

### 3.1 核心功能

**Toast 增强功能**：

- 类型扩展：success、error、info、warning、loading
- 堆叠管理：支持同时显示多个 Toast（最多 5 个）
- 位置配置：top-left、top-center、top-right、bottom-left、bottom-center、bottom-right
- 操作按钮：支持自定义操作（如"撤销"、"查看详情"）
- 进度条：可选的倒计时进度条
- 可关闭：手动关闭按钮
- 自动消失：可配置持续时间（默认 3000ms）
- 自定义图标：支持 Material Icons 或自定义 SVG
- 动画效果：滑入、淡入、弹跳等多种动画

### 3.2 API 设计

```typescript
// 基础用法
feedbackManager.success('操作成功')
feedbackManager.error('操作失败')
feedbackManager.warning('请注意')
feedbackManager.info('提示信息')
feedbackManager.loading('加载中...')

// 高级用法
feedbackManager.show({
  type: 'success',
  message: '书籍已添加到书架',
  duration: 5000,
  closable: true,
  position: 'top-right',
  showProgress: true,
  actions: [
    { label: '查看', onClick: () => router.push('/bookshelf') },
    { label: '撤销', onClick: () => undoAction() }
  ],
  icon: 'bookmark_added'
})
```

### 3.3 队列管理

- 优先级队列（error > warning > success > info）
- 相同消息去重（防止重复显示）
- 自动清理过期 Toast
- 最大显示数量限制（5 个）

### 3.4 组件设计

**FeedbackToast.vue**：单个 Toast 组件

- Props: type, message, duration, closable, showProgress, actions, icon, position
- 支持中国风设计风格（水墨色调、毛笔字体）
- 响应式设计（移动端自适应）

**ToastContainer.vue**：Toast 容器组件

- 管理多个 Toast 的堆叠显示
- 自动计算位置和间距
- 处理进入/离开动画

## 4. 骨架屏系统

### 4.1 专用骨架屏组件

创建 8 个专用骨架屏组件，覆盖主要页面场景：

1. **BookCardSkeleton.vue** - 书籍卡片骨架屏
   - 用于：首页书籍网格、搜索结果
   - 包含：封面图、标题、作者、标签、按钮区域
   - 支持：卡片/列表两种布局模式

2. **BookListSkeleton.vue** - 书籍列表骨架屏
   - 用于：我的借阅、预约列表
   - 包含：多行列表项，每项有图标、文本、状态标签
   - 支持：配置显示行数（默认 5 行）

3. **BookDetailSkeleton.vue** - 书籍详情骨架屏
   - 用于：书籍详情页
   - 包含：大封面、标题区、元数据区、描述区、操作按钮区
   - 布局：左右分栏布局

4. **UserProfileSkeleton.vue** - 用户资料骨架屏
   - 用于：个人中心页面
   - 包含：头像、用户名、统计卡片、表单字段

5. **DashboardSkeleton.vue** - 仪表盘骨架屏
   - 用于：管理员仪表盘
   - 包含：统计卡片网格、图表占位、表格占位

6. **TableSkeleton.vue** - 表格骨架屏
   - 用于：用户管理、图书管理等表格页面
   - 包含：表头、多行数据行、分页器
   - 支持：配置列数和行数

7. **FormSkeleton.vue** - 表单骨架屏
   - 用于：各种表单页面
   - 包含：标签、输入框、按钮组
   - 支持：配置字段数量

8. **SearchResultSkeleton.vue** - 搜索结果骨架屏
   - 用于：搜索页面
   - 包含：搜索框、筛选器、结果列表

### 4.2 骨架屏特性

- 波浪动画：流畅的光泽扫过效果
- 响应式：自动适配移动端和桌面端
- 主题适配：匹配项目中国风设计风格（水墨色调）
- 可配置：支持自定义行数、列数、动画速度

### 4.3 SkeletonFactory 使用方式

```typescript
// 在组件中使用
const { showSkeleton, hideSkeleton } = useSkeleton()

// 自动显示对应的骨架屏
showSkeleton('book-list')  // 显示 BookListSkeleton
showSkeleton('book-detail') // 显示 BookDetailSkeleton

// 数据加载完成后隐藏
hideSkeleton()
```

## 5. ErrorCenter 错误处理系统

### 5.1 错误分类体系

**错误类型**：

- **NetworkError** - 网络错误（无网络、超时、DNS 失败）
- **APIError** - API 错误（4xx、5xx 状态码）
- **ValidationError** - 验证错误（表单验证失败）
- **AuthError** - 认证错误（未登录、token 过期、权限不足）
- **BusinessError** - 业务错误（库存不足、已被预约等）
- **ComponentError** - 组件错误（Vue 组件运行时错误）
- **UnknownError** - 未知错误

### 5.2 错误码映射

```typescript
const errorMessages = {
  // 网络错误
  'NETWORK_ERROR': '网络连接失败，请检查网络设置',
  'TIMEOUT': '请求超时，请稍后重试',
  
  // 认证错误
  401: '登录已过期，请重新登录',
  403: '没有权限访问此资源',
  
  // 业务错误
  'BOOK_NOT_AVAILABLE': '该书籍暂时不可借阅',
  'RESERVATION_FULL': '预约人数已满',
  
  // 默认错误
  'DEFAULT': '操作失败，请稍后重试'
}
```

### 5.3 核心功能

**错误拦截**：

- HTTP 拦截器集成（axios interceptor）
- Vue 全局错误处理器（app.config.errorHandler）
- Promise 未捕获错误（window.onunhandledrejection）
- 组件错误边界（ErrorBoundary）

**自动重试机制**：

- 指数退避算法（1s → 2s → 4s → 8s）
- 可配置最大重试次数（默认 3 次）
- 仅对幂等请求自动重试（GET、PUT、DELETE）
- 用户可手动触发重试

**错误日志**：

- 本地日志记录（localStorage，最多保存 100 条）
- 错误上报（可选，发送到后端日志服务）
- 包含上下文信息（用户 ID、页面路径、时间戳、错误堆栈）

**用户界面**：

- ErrorBoundary 组件：捕获子组件错误，显示降级 UI
- ErrorPage 组件：统一错误页面（404、500、网络错误）
- ErrorRetry 组件：内联错误提示 + 重试按钮
- Toast 通知：轻量级错误提示

### 5.4 API 设计

```typescript
// 基础用法
errorCenter.handle(error)  // 自动分类并处理错误

// 手动处理特定错误
errorCenter.handleNetworkError(error)
errorCenter.handleAPIError(error, { showToast: true })
errorCenter.handleAuthError(error)  // 自动跳转登录页

// 重试机制
await errorCenter.retry(() => fetchBookList(), {
  maxRetries: 3,
  backoff: 'exponential'
})

// 错误日志
errorCenter.log(error, { context: { page: 'BookDetail', bookId: 123 } })
errorCenter.getLogs()  // 获取所有日志
errorCenter.clearLogs()  // 清空日志
```

## 6. OfflineManager 离线管理系统

### 6.1 网络状态监控

**网络状态分类**：

- **online** - 在线（正常网络）
- **offline** - 离线（无网络）
- **slow** - 慢速网络（检测到高延迟）
- **reconnecting** - 重连中

**网络质量检测**：

- 定期 ping 后端健康检查接口（每 30 秒）
- 测量响应时间判断网络速度
- 慢速网络阈值：> 2000ms

### 6.2 IndexedDB 缓存策略

**缓存数据类型**：

- 书籍列表：首页热门书籍、搜索结果（缓存 1 小时）
- 书籍详情：详情页数据（缓存 30 分钟）
- 用户借阅记录：我的借阅、预约（缓存 10 分钟）
- 用户信息：个人资料（缓存 1 小时）
- 静态资源：图书封面图片（永久缓存，LRU 淘汰）

**缓存策略**：

- **Cache First**：优先使用缓存，后台更新（书籍列表、详情）
- **Network First**：优先网络请求，失败时使用缓存（用户数据）
- **Stale While Revalidate**：返回缓存同时后台更新（搜索结果）

**缓存管理**：

- 自动过期清理
- 手动清除缓存功能
- 缓存大小限制（最大 50MB）
- LRU 淘汰策略

### 6.3 离线操作队列

**可离线操作**：

- 添加书签/收藏（排队执行）
- 提交采购建议（排队执行）
- 提交损坏报告（排队执行）

**队列机制**：

- 离线时将操作存入 IndexedDB 队列
- 联网后自动按顺序执行
- 执行失败自动重试（最多 3 次）
- 用户可查看队列状态和手动清除

**冲突处理**：

- 检测数据冲突（如书籍已被借出）
- 提示用户选择保留本地或服务器数据

### 6.4 Service Worker 集成

**资源缓存**：

- 应用外壳（HTML、CSS、JS）
- 静态资源（图标、字体）
- API 响应（根据策略）

**离线页面**：

- 离线时显示友好的离线页面
- 显示已缓存的内容列表
- 提供重新连接按钮

**后台同步**：

- 使用 Background Sync API
- 网络恢复时自动同步数据

### 6.5 API 设计

```typescript
// 网络状态
const { status, isOnline, isSlow } = offlineManager.getNetworkStatus()

// 缓存管理
await offlineManager.cache.set('book-list', data, { ttl: 3600 })
const data = await offlineManager.cache.get('book-list')
await offlineManager.cache.clear()

// 离线操作队列
offlineManager.queue.add({
  type: 'add-favorite',
  data: { bookId: 123 },
  retry: 3
})
const pending = offlineManager.queue.getPending()
await offlineManager.queue.sync()  // 手动同步

// 数据同步事件
offlineManager.on('sync-start', () => console.log('开始同步'))
offlineManager.on('sync-complete', () => console.log('同步完成'))
offlineManager.on('sync-error', (error) => console.error('同步失败', error))
```

### 6.6 UI 组件增强

**OfflineIndicator 增强**：

- 显示当前网络状态（在线/离线/慢速）
- 显示待同步操作数量
- 点击查看同步详情

**SyncStatus 组件**：

- 显示同步进度
- 显示队列中的操作列表
- 手动触发同步按钮
- 清除队列按钮

## 7. 数据流与交互

### 7.1 组件间通信

```text
用户操作
  ↓
Vue 组件 → useFeedback/useError/useOffline
  ↓
FeedbackManager/ErrorCenter/OfflineManager
  ↓
UI 更新（Toast/骨架屏/错误页面）
```

### 7.2 错误处理流程

```text
错误发生
  ↓
ErrorCenter 拦截
  ↓
错误分类 → 错误码映射
  ↓
判断是否需要重试
  ↓ 是
自动重试（指数退避）
  ↓ 否
显示错误提示（Toast/ErrorPage）
  ↓
记录错误日志
```

### 7.3 离线同步流程

```text
用户操作（离线状态）
  ↓
OfflineManager 检测离线
  ↓
操作加入队列（IndexedDB）
  ↓
显示"已加入同步队列"提示
  ↓
网络恢复
  ↓
自动触发同步
  ↓
按顺序执行队列操作
  ↓
成功 → 从队列移除
失败 → 重试或提示用户
```

## 8. 技术实现细节

### 8.1 FeedbackManager 实现

**核心类结构**：

```typescript
class FeedbackManager {
  private toasts: Toast[] = []
  private maxToasts = 5
  
  show(options: ToastOptions): string
  success(message: string): string
  error(message: string): string
  warning(message: string): string
  info(message: string): string
  loading(message: string): string
  hide(id: string): void
  clear(): void
}
```

**Why**: 单例模式确保全局唯一实例，避免多个管理器冲突

**How to apply**: 在 main.ts 中初始化并注入到 Vue 应用

### 8.2 ErrorCenter 实现

**核心类结构**：

```typescript
class ErrorCenter {
  private errorHandlers: Map<ErrorType, ErrorHandler>
  private retryConfig: RetryConfig
  
  handle(error: Error): void
  retry<T>(fn: () => Promise<T>, options?: RetryOptions): Promise<T>
  log(error: Error, context?: any): void
  getLogs(): ErrorLog[]
}
```

**Why**: 集中管理错误处理逻辑，避免重复代码

**How to apply**: 在 axios 拦截器和 Vue errorHandler 中集成

### 8.3 OfflineManager 实现

**核心类结构**：

```typescript
class OfflineManager {
  private db: IDBDatabase
  private networkStatus: NetworkStatus
  private syncQueue: SyncQueue
  
  getNetworkStatus(): NetworkStatus
  cache: CacheManager
  queue: QueueManager
  on(event: string, handler: Function): void
}
```

**Why**: 统一管理离线功能，提供一致的 API

**How to apply**: 在 App.vue 中初始化，监听网络状态变化

### 8.4 Service Worker 注册

```typescript
// main.ts
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js')
}
```

**Why**: 实现 PWA 功能，支持离线访问

**How to apply**: 在生产环境启用，开发环境可选

## 9. 迁移策略

### 9.1 现有组件迁移

**FeedbackToast.vue**：

- 保留现有组件作为兼容层
- 内部调用新的 FeedbackManager
- 逐步迁移使用方式

**OfflineIndicator.vue**：

- 增强现有组件功能
- 集成 OfflineManager
- 保持向后兼容

**useToast.ts**：

- 重构为 useFeedback.ts
- 提供更丰富的 API
- 保留旧 API 作为别名

### 9.2 分阶段实施

**阶段 1：核心服务层**（1-2 天）

- 实现 FeedbackManager
- 实现 ErrorCenter
- 实现 OfflineManager
- 实现 SkeletonFactory

**阶段 2：UI 组件**（2-3 天）

- 重构 FeedbackToast 和 ToastContainer
- 创建 8 个骨架屏组件
- 创建 ErrorBoundary、ErrorPage、ErrorRetry
- 增强 OfflineIndicator，创建 SyncStatus

**阶段 3：集成与测试**（1-2 天）

- 集成到现有页面
- HTTP 拦截器集成
- Vue 错误处理集成
- Service Worker 配置
- 端到端测试

**阶段 4：优化与文档**（1 天）

- 性能优化
- 编写使用文档
- 代码审查
- 部署上线

## 10. 测试策略

### 10.1 单元测试

- FeedbackManager 核心方法测试
- ErrorCenter 错误分类和重试逻辑测试
- OfflineManager 缓存和队列测试
- 各骨架屏组件渲染测试

### 10.2 集成测试

- Toast 堆叠显示测试
- 错误拦截和处理流程测试
- 离线操作队列同步测试
- Service Worker 缓存策略测试

### 10.3 端到端测试

- 用户操作反馈完整流程
- 网络断开/恢复场景测试
- 错误重试和恢复测试
- 多页面骨架屏加载测试

## 11. 性能考虑

### 11.1 优化点

- Toast 使用虚拟滚动（超过 5 个时）
- 骨架屏使用 CSS 动画（避免 JS 动画）
- IndexedDB 操作使用 Web Worker（避免阻塞主线程）
- Service Worker 缓存策略优化（避免过度缓存）

### 11.2 性能指标

- Toast 显示延迟 < 50ms
- 骨架屏渲染时间 < 100ms
- IndexedDB 读写延迟 < 200ms
- Service Worker 缓存命中率 > 80%

## 12. 安全考虑

### 12.1 数据安全

- IndexedDB 存储敏感数据加密
- 错误日志脱敏（移除敏感信息）
- Service Worker 仅缓存公开资源

### 12.2 XSS 防护

- Toast 消息内容转义
- 错误提示内容过滤
- 用户输入验证

## 13. 兼容性

### 13.1 浏览器支持

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### 13.2 降级策略

- IndexedDB 不支持时使用 localStorage
- Service Worker 不支持时使用传统缓存
- 骨架屏不支持时显示 loading 文本

## 14. 总结

本设计采用重构式升级方案，建立统一的交互反馈架构，包括：

1. **FeedbackManager**：统一管理所有用户反馈，支持丰富的 Toast 功能
2. **骨架屏系统**：8 个专用骨架屏组件，覆盖主要页面场景
3. **ErrorCenter**：集中处理所有错误，提供自动重试和友好提示
4. **OfflineManager**：完整的离线支持，包括缓存、队列、同步

通过这套系统，将显著提升用户体验，提供流畅、友好、可靠的交互反馈。
