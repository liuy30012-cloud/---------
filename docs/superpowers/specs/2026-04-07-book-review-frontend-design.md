# 书籍评价前端UI设计文档

**日期**: 2026-04-07  
**项目**: 图书馆书籍定位系统  
**功能**: 书籍评价前端界面

## 1. 概述

本设计文档描述了书籍评价系统的前端UI实现方案。后端API已完全实现，本次工作重点是创建Vue组件来展示和管理书籍评价，包括评分统计、评价列表、评价表单等功能。

## 2. 设计目标

- 将现有BookDetail.vue中的简单评价UI替换为功能完整的组件化系统
- 连接到已实现的后端REST API
- 提供评分统计可视化、评价排序、分页、编辑/删除等功能
- 采用传统中国风格的视觉设计，与登录界面的水墨风格保持一致

## 3. 架构设计

### 3.1 组件结构

创建4个新的Vue组件，位于 `frontend/src/components/reviews/`：

**BookReviewStats.vue**
- 功能：显示评分统计信息
- 职责：展示平均评分和总评价数、显示评分分布（5星到1星的数量和百分比）、使用水墨风格的进度条可视化
- Props: `bookId: number`
- 数据源：调用 `GET /api/reviews/book/{bookId}/statistics`

**BookReviewForm.vue**
- 功能：创建或编辑评价
- 职责：交互式5星评分选择器、评价内容输入框（最大1000字符）、表单验证和提交、支持创建和编辑两种模式
- Props: `bookId: number`, `existingReview?: BookReviewResponse`, `mode?: 'create' | 'edit'`
- Emits: `review-submitted`, `cancelled`

**BookReviewList.vue**
- 功能：展示评价列表
- 职责：分页显示评价、排序选择（最新、最早、评分最高、评分最低）、空状态提示、加载状态处理
- Props: `bookId: number`, `currentUserId: number`
- 内部使用：BookReviewItem组件
- Emits: `edit-review(review)`, `delete-review(reviewId)`

**BookReviewItem.vue**
- 功能：单条评价卡片
- 职责：显示用户名、评分星级、日期、评价内容、为用户自己的评价显示编辑/删除按钮、传统卡片样式
- Props: `review: BookReviewResponse`, `isOwnReview: boolean`, `canEdit: boolean`
- Emits: `edit-clicked`, `delete-clicked`

### 3.2 API集成

更新 `frontend/src/api/bookApi.ts`，添加以下方法：

```typescript
// 评价CRUD操作
createReview(bookId: number, request: {rating: number, content: string})
  → POST /api/reviews
  
updateReview(reviewId: number, request: {rating: number, content: string})
  → PUT /api/reviews/{reviewId}
  
deleteReview(reviewId: number)
  → DELETE /api/reviews/{reviewId}

// 评价查询
getBookReviews(bookId: number, page: number, size: number, sortBy: string)
  → GET /api/reviews/book/{bookId}?page={page}&size={size}&sortBy={sortBy}
  
getMyReviews(page: number, size: number)
  → GET /api/reviews/my-reviews?page={page}&size={size}
  
getRatingStatistics(bookId: number)
  → GET /api/reviews/book/{bookId}/statistics
```

**类型定义：**

```typescript
interface BookReviewResponse {
  id: number
  bookId: number
  userId: number
  username: string
  rating: number
  content: string
  createdAt: string
  updatedAt: string
}

interface BookRatingStatistics {
  bookId: number
  averageRating: number
  totalReviews: number
  fiveStarCount: number
  fourStarCount: number
  threeStarCount: number
  twoStarCount: number
  oneStarCount: number
}
```

## 4. 数据流设计

### 4.1 评价提交流程

1. 用户在BookReviewForm中填写评分和内容
2. 点击提交按钮
3. 表单验证：评分必选（1-5星）、内容非空且不超过1000字符
4. 调用 `bookApi.createReview(bookId, {rating, content})`
5. 成功后：触发 `review-submitted` 事件、父组件刷新评价列表和统计数据、显示成功提示、清空表单
6. 失败处理：显示错误信息、如果是"已评价过"错误，引导用户编辑现有评价

### 4.2 评价编辑流程

1. 用户点击自己评价上的"编辑"按钮
2. BookReviewItem触发 `edit-clicked` 事件，传递评价数据
3. BookDetail.vue接收事件，设置 `editingReview` 状态
4. BookReviewForm以编辑模式显示，预填充现有数据
5. 用户修改后提交
6. 调用 `bookApi.updateReview(reviewId, {rating, content})`
7. 成功后：触发 `review-submitted` 事件、刷新列表、关闭编辑模式、显示成功提示

### 4.3 评价删除流程

1. 用户点击自己评价上的"删除"按钮
2. 显示确认对话框："确定要删除这条评价吗？"
3. 用户确认后，调用 `bookApi.deleteReview(reviewId)`
4. 成功后：从列表中移除该评价、更新统计数据、显示成功提示

### 4.4 分页与排序

- BookReviewList维护本地状态：`currentPage: number` (默认0)、`pageSize: number` (默认10)、`sortBy: string` (默认'createdAt')
- 当页码或排序改变时，重新调用API获取数据
- 后端返回分页对象，包含：`content: BookReviewResponse[]`、`totalElements: number`、`totalPages: number`、`number: number` (当前页)

## 5. 视觉设计

### 5.1 设计风格

采用传统中国风格，与登录界面的水墨风格保持一致。

**色彩方案：**
- 主色：墨黑 `#2c3e50` (文字)
- 强调色：朱砂红 `#c0392b` (评分、操作按钮)
- 背景：宣纸白 `#faf9f6` (卡片背景)
- 边框：淡墨 `#e8e6e3` (分隔线、边框)
- 星级：琥珀金 `#f39c12` (填充星星)
- 星级空：浅灰 `#d5d5d5` (未填充星星)

**字体：**
- 标题：优雅衬线字体或中文风格字体
- 正文：清晰无衬线字体，保证可读性
- 评价内容：稍大字号，舒适阅读

### 5.2 组件样式

**评分统计 (BookReviewStats)**
- 顶部显示大号平均评分和总评价数
- 下方显示5个水平进度条（5星到1星）
- 进度条样式：类似水墨笔触的渐变效果、从浅到深的渐变（基于百分比）、加载时平滑动画、高度8px、圆角4px

**评价表单 (BookReviewForm)**
- 星级选择器：5个大号星星（40px）可点击、悬停效果轻微放大、选中状态琥珀金填充
- 文本框：边框样式类似传统信纸、右下角显示字符计数 (xxx/1000)、最小高度120px
- 提交按钮：水墨风格带涟漪效果、提交中显示加载动画

**评价列表 (BookReviewList)**
- 顶部排序下拉框：选项包括最新、最早、评分最高、评分最低
- 底部分页控件：上一页/下一页按钮、页码指示器、禁用状态样式

**评价卡片 (BookReviewItem)**
- 卡片样式：微妙阴影（如纸张从表面抬起）、圆角8px、内边距16px、悬停效果阴影加深轻微上移
- 布局：顶部显示用户名、星级、日期（一行）、中间显示评价内容、底部显示编辑/删除按钮（仅本人评价可见）
- 编辑/删除按钮：最小化设计、悬停时显示、图标+文字

### 5.3 响应式设计

- 桌面端（>768px）：评分统计和表单并排显示、评价列表每行1个卡片宽度100%
- 移动端（≤768px）：所有元素垂直堆叠、星级选择器稍小（32px）、卡片内边距减小（12px）

## 6. 错误处理

### 6.1 错误场景

**重复评价检测**
- 后端返回："You have already reviewed this book"
- 前端处理：显示提示"您已经评价过这本书"、提供"编辑现有评价"按钮、点击后加载用户的现有评价到编辑表单

**未登录用户**
- 评价表单显示："请先登录后再评价"
- 提供"前往登录"按钮
- 点击后跳转到登录页

**网络错误**
- API调用失败时显示友好错误信息
- 提供"重试"按钮
- 错误信息示例："网络连接失败，请检查网络后重试"

**权限错误**
- 尝试编辑/删除他人评价时：后端返回401/403错误、前端显示"无权限操作"、编辑/删除按钮在非本人评价时隐藏（前端预防）

**验证错误**
- 评分未选择：提示"请选择评分"
- 内容为空：提示"请输入评价内容"
- 内容超长：实时显示字符计数，超过1000时禁用提交按钮

### 6.2 加载状态

- 评价列表加载时：显示骨架屏（3-5个占位卡片）
- 统计数据加载时：显示占位符（灰色条）
- 提交评价时：按钮显示加载动画、禁用按钮防止重复点击、禁用表单输入

## 7. 性能优化

### 7.1 懒加载与分页

- 评价列表默认加载10条
- 用户点击"加载更多"或翻页时加载下一页
- 统计数据单独请求，不阻塞评价列表加载

### 7.2 缓存策略

- 评分统计数据缓存5分钟
- 用户提交新评价时清除缓存
- 已加载的评价页缓存在组件状态中
- 切换排序时重新请求，切换页码时优先使用缓存

### 7.3 防抖与节流

- 评价内容输入：防抖300ms后更新字符计数
- 提交按钮：防止重复点击（提交中禁用）
- 排序切换：立即响应，无需防抖

### 7.4 组件优化

- BookReviewItem使用 `v-memo` 优化渲染
- 评价列表使用 `v-for` 的 `key` 绑定到 `review.id`
- 星级组件使用CSS而非图片，减少资源加载
- 条件渲染：编辑/删除按钮仅在 `isOwnReview` 时渲染

### 7.5 数据预取

- 进入书籍详情页时，同时请求：评分统计、最新10条评价（默认排序）
- 用户滚动到评价区域时才加载完整列表（可选优化）

## 8. BookDetail.vue集成

### 8.1 替换现有代码

将BookDetail.vue第135-188行的简单评价UI替换为新组件。

### 8.2 状态管理

在BookDetail.vue的 `<script setup>` 中添加评价相关状态和事件处理函数。

### 8.3 移除旧代码

删除 `newReview` ref、`submitReview` 函数、旧的评价表单和列表HTML（第138-187行）。

## 9. 测试计划

### 9.1 单元测试

- BookReviewForm：验证评分选择、验证内容输入和字符限制、验证表单提交、验证编辑模式预填充
- BookReviewItem：验证编辑/删除按钮显示逻辑、验证事件触发
- BookReviewList：验证排序切换、验证分页逻辑

### 9.2 集成测试

- 完整评价流程：创建 → 显示 → 编辑 → 删除
- 权限验证：非本人评价不显示编辑/删除按钮
- 错误处理：网络错误、重复评价、验证错误

### 9.3 手动测试

- 视觉检查：确认传统中国风格一致性
- 响应式测试：桌面端和移动端布局
- 性能测试：大量评价时的加载速度
- 用户体验：表单交互流畅性

## 10. 实施步骤

1. 创建 `frontend/src/components/reviews/` 目录
2. 实现 BookReviewItem.vue（最小依赖）
3. 实现 BookReviewForm.vue
4. 实现 BookReviewStats.vue
5. 实现 BookReviewList.vue
6. 更新 bookApi.ts，添加评价相关API方法
7. 更新 BookDetail.vue，集成新组件
8. 添加CSS样式（传统中国风格）
9. 测试所有功能
10. 优化性能和用户体验

## 11. 未来扩展

可能的功能扩展（不在本次实施范围内）：
- 评价点赞/有用功能
- 评价举报/审核功能
- 图片上传（书籍照片）
- 评价回复功能
- 评价分享到社交媒体
- 评价导出功能

---

**文档版本**: 1.0  
**最后更新**: 2026-04-07
