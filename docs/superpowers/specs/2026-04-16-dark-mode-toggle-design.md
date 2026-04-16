# 暗色模式切换开关设计

**日期:** 2026-04-16
**状态:** 已批准

## 背景

app.css 中已定义完整的 Midnight Archive 暗色主题样式（第 2268-2917 行），但没有任何 UI 让用户激活它。暗色 CSS 是直接的全局样式，被紧随其后的 Bamboo Monastery 亮色层（第 2919 行起）覆盖，导致暗色主题永远不会生效。

## 方案

使用 `data-theme` 属性隔离两个主题的 CSS，在 TopNav 中添加切换按钮，localStorage 持久化用户偏好。

## 改动范围

### 1. CSS 层改造 — app.css

- Bamboo Monastery Home Layer（2919-3303 行）：选择器加 `[data-theme="bamboo-monastery"]` 前缀
- Bamboo Monastery Page Layer（3305-3889 行）：同上
- Shared Monastery Shell（3890-4089 行）：同上
- Midnight Archive Layer（2268-2917 行）：选择器加 `[data-theme="midnight-archive"]` 前缀
- 末尾添加全局平滑过渡属性

### 2. CSS 层改造 — pet.css

- Midnight Archive Theme 部分（第 606 行起）：加 `[data-theme="midnight-archive"]` 前缀

### 3. 主题管理 Composable — 新文件

`frontend/src/composables/useTheme.ts`:
- 读取 localStorage（键名 `library-theme`）
- 默认值 `bamboo-monastery`
- 提供 `toggle()` 和 `isDark` 响应式状态
- 设置 `document.documentElement.dataset.theme`
- 使用已有的 storageHelpers.ts

### 4. TopNav 切换按钮

- 在语言切换按钮旁添加主题图标按钮
- 亮色 → 月亮图标，暗色 → 太阳图标
- 点击调用 toggle()

### 5. App.vue 初始化

- onMounted 中调用 useTheme() 确保主题立即生效

### 6. 持久化

- 键名：`library-theme`
- 值：`bamboo-monastery` 或 `midnight-archive`
