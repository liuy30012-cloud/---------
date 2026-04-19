# 渐进式按钮状态系统设计

## 背景

项目前端（Vue 3 + TypeScript）没有使用第三方 UI 库，所有按钮样式以 CSS class 方式散落在 `app.css` 中（约 4100 行），缺乏统一的状态管理。按钮没有悬停过渡、加载指示、成功/失败反馈、禁用提示等标准交互模式。

## 目标

1. 引入 Element Plus，基于 `el-button` 封装通用 `LibraryButton` 组件
2. 通过 CSS 变量定制 Element Plus 主题，融入项目现有竹院风暖色调设计
3. 实现完整的按钮状态动画系统：悬停、点击、加载中、成功、失败、禁用
4. 全面替换项目中所有散落的按钮样式，统一按钮体验

## 不变部分

- `SuperButton`（首页装饰性按钮）完全不动
- `.nav-link`、`.nav-arrow` 等非按钮语义元素不变

## 组件设计

### LibraryButton.vue

位置：`frontend/src/components/common/LibraryButton.vue`

基于 Element Plus `el-button` 封装，通过 props 控制状态。

### Props

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | `'primary' \| 'secondary' \| 'danger' \| 'ghost'` | `'primary'` | 按钮类型 |
| `size` | `'small' \| 'default' \| 'large'` | `'default'` | 尺寸 |
| `loading` | `boolean` | `false` | 加载状态，显示旋转指示器 |
| `loadingText` | `string` | `''` | 加载时替换的文本（不传则保留原文本） |
| `disabled` | `boolean` | `false` | 禁用状态 |
| `disabledReason` | `string` | `''` | 禁用时 hover 显示的 tooltip 提示 |
| `icon` | `Component` | `undefined` | 左侧图标 |
| `successText` | `string` | `'操作成功'` | 操作成功时的临时文本 |
| `errorText` | `string` | `'操作失败'` | 操作失败时的临时文本 |

### 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| `click` | `MouseEvent` | 按钮点击（加载/禁用时自动阻止） |

### 状态机

```
默认 → [点击] → 加载中(旋转动画) → 成功(✓ + 绿色闪烁 2s) → 默认
                                      → 失败(✗ + 红色抖动 2s) → 默认
默认 → [禁用] → 灰色 + 禁止图标 + tooltip提示
默认 → [悬停] → 轻微上浮 + 阴影增强
```

状态转换通过 composable `useButtonState` 管理。

### composable: useButtonState

位置：`frontend/src/composables/useButtonState.ts`

提供响应式状态管理和自动恢复逻辑：

- `execute(action: () => Promise<void>)` — 包装异步操作，自动管理 加载→成功/失败→恢复 的完整周期
- `state` — 当前状态：`'idle' | 'loading' | 'success' | 'error'`
- `reset()` — 手动重置为 idle

## 主题定制

通过覆盖 Element Plus CSS 变量，将默认蓝色系映射到竹院风暖色调。

### 映射关系

| Element Plus 变量 | 映射值 |
|---|---|
| `--el-color-primary` | `#c7a067`（项目 `--primary`） |
| `--el-color-primary-light-3` | 基于 `#c7a067` 计算 |
| `--el-color-primary-light-5` | 基于 `#c7a067` 计算 |
| `--el-color-primary-light-7` | 基于 `#c7a067` 计算 |
| `--el-color-primary-light-8` | 基于 `#c7a067` 计算 |
| `--el-color-primary-light-9` | 基于 `#c7a067` 计算 |
| `--el-color-primary-dark-2` | `#8f6738`（项目 `--primary-dim`） |
| `--el-color-danger` | `#e74c3c` |
| `--el-color-success` | `#2ecc71` |
| `--el-color-warning` | `#f39c12` |
| `--el-border-radius-base` | 项目 `--radius-md` 值 |
| `--el-font-family` | 项目 `--font-body` |

主题覆盖写在全局样式入口，同时确保 `[data-theme="midnight-archive"]` 深色模式下变量正确切换。

## 按钮状态动画规范

| 状态 | 视觉效果 | 过渡时长 |
|---|---|---|
| **悬停** | 上浮 2px + 阴影增强 + 亮度提升 10% | `--motion-duration-fast` (0.24s) |
| **点击/激活** | 缩放 0.97 + 阴影收缩 | 0.1s |
| **加载中** | 文本淡出 + 旋转指示器淡入，按钮宽度不变 | 0.3s |
| **成功** | 背景闪绿色 + ✓ 图标弹入 + 成功文本 | 2s 后恢复 |
| **失败** | 背景闪红色 + ✗ 图标 + 水平抖动 2 次 | 2s 后恢复 |
| **禁用** | 灰色背景 + 半透明 + 禁止图标（左上角小图标） | 静态 |
| **禁用悬停** | tooltip 显示 `disabledReason` 文字 | — |

成功/失败状态除按钮自身视觉变化外，同时触发 `FeedbackToast` 组件显示 toast 通知。

## 按钮类型映射

| LibraryButton type | 视觉 | 用途 |
|---|---|---|
| `primary` | 竹院暖色渐变背景 `--primary → --primary-dim`，白色文字 | 主操作（提交、保存、确认） |
| `secondary` | 白色/透明背景，暖色边框，暖色文字 | 次要操作（查询、筛选、重置） |
| `danger` | 红色系背景，白色文字 | 危险操作（删除、报告损坏） |
| `ghost` | 无背景无边框，文字色，hover 显示底色 | 返回、取消、视图切换 |

## 迁移清单

### 需要替换的按钮

| 现有 class | 所在页面 | 替换为 |
|---|---|---|
| `.page-action-btn--primary` | 多个页面 | `type="primary"` |
| `.page-action-btn--secondary` | 多个页面 | `type="secondary"` |
| `.page-action-btn--damage` | BookDetail | `type="danger"` |
| `.login-btn` | Login | `type="primary" size="large"` |
| `.submit-btn` | 多个表单 | `type="primary"` |
| `.query-btn` | BookSearch | `type="secondary"` |
| `.vote-btn` | PurchaseSuggestions | `type="primary" size="small"` |
| `.admin-save-btn` | UserManagement | `type="primary"` |
| `.back-btn` | 多个页面 | `type="ghost"` |
| `.icon-btn` | 导航栏 | `type="ghost" size="small"` |
| `.popup-action-btn` | 弹窗 | `type="primary/secondary"` |
| `.toggle-btn` | 视图切换 | `type="ghost" size="small"` |
| 分页按钮 | 各列表页 | 使用 `el-pagination` 替换 |

### 迁移顺序（风险从低到高）

1. 安装 Element Plus + 主题定制 + 创建 LibraryButton 组件 + useButtonState composable
2. 替换简单页面（NotFound、MyAccount）的按钮
3. 替换表单类按钮（Login、ForgotPassword、Dashboard）
4. 替换复杂页面（BookSearch、BookDetail、UserManagement）
5. 替换弹窗、导航、分页中的按钮
6. 清理 `app.css` 中废弃的按钮样式代码

## 文件影响

### 新增文件

- `frontend/src/components/common/LibraryButton.vue` — 通用按钮组件
- `frontend/src/composables/useButtonState.ts` — 按钮状态管理 composable

### 修改文件

- `frontend/package.json` — 添加 element-plus 依赖
- `frontend/src/main.ts` — 注册 Element Plus
- `frontend/src/styles/variables.css` — 添加 Element Plus 主题覆盖变量
- `frontend/src/styles/app.css` — 后期清理废弃按钮样式
- 所有包含上述按钮 class 的 `.vue` 文件 — 替换为 LibraryButton

## 技术约束

- Element Plus 按需导入（通过 `unplugin-vue-components` + `unplugin-auto-import`），避免全量打包
- 主题覆盖仅通过 CSS 变量，不修改 Element Plus 源码
- 所有动画使用 CSS transition/animation，不依赖 JS 动画库（GSAP 不参与按钮动画）
- 禁用状态使用 `el-tooltip` 实现提示，保持与 Element Plus 一致的 tooltip 行为
