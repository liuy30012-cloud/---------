# 渐进式按钮状态系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Element Plus 封装 LibraryButton 通用组件，实现完整的按钮状态动画系统（悬停、加载、成功、失败、禁用），并全面替换项目中所有散落的按钮样式。

**Architecture:** 安装 Element Plus + 按需导入插件 → 覆盖 CSS 变量定制竹院风主题 → 创建 useButtonState composable 管理状态机 → 创建 LibraryButton 组件包裹 el-button → 分批替换所有现有按钮 → 清理废弃样式。

**Tech Stack:** Vue 3, Element Plus, TypeScript, CSS Variables

---

## 文件结构

### 新增文件
- `frontend/src/components/common/LibraryButton.vue` — 通用按钮组件，包裹 el-button
- `frontend/src/composables/useButtonState.ts` — 按钮状态管理 composable（idle/loading/success/error）
- `frontend/src/styles/element-theme.css` — Element Plus 主题覆盖变量

### 修改文件
- `frontend/package.json` — 添加 element-plus、unplugin-vue-components、unplugin-auto-import
- `frontend/vite.config.ts` — 配置按需导入插件
- `frontend/src/main.ts` — 引入 Element Plus 主题覆盖样式
- `frontend/src/styles/app.css` — 后期清理废弃按钮样式
- 所有使用旧按钮 class 的 .vue 文件（约 18 个文件）

---

## Task 1: 安装 Element Plus 和按需导入插件

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`

- [ ] **Step 1: 安装依赖**

```bash
cd frontend && npm install element-plus && npm install -D unplugin-vue-components unplugin-auto-import
```

- [ ] **Step 2: 验证安装成功**

```bash
cd frontend && cat node_modules/element-plus/package.json | head -5
```

Expected: 看到 element-plus 版本信息

- [ ] **Step 3: 配置 vite.config.ts 按需导入**

将 `frontend/vite.config.ts` 修改为：

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
          'echarts': ['echarts'],
          'axios': ['axios'],
          'element-plus': ['element-plus']
        }
      }
    },
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    cssCodeSplit: true,
    chunkSizeWarningLimit: 1000
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'vue-i18n', 'element-plus'],
    exclude: ['echarts']
  }
})
```

- [ ] **Step 4: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -20
```

Expected: 构建成功，无错误

- [ ] **Step 5: 提交**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts
git commit -m "chore: 安装 Element Plus 和按需导入插件"
```

---

## Task 2: 创建 Element Plus 竹院风主题覆盖

**Files:**
- Create: `frontend/src/styles/element-theme.css`
- Modify: `frontend/src/main.ts`

- [ ] **Step 1: 创建主题覆盖文件**

创建 `frontend/src/styles/element-theme.css`：

```css
/* Element Plus 竹院风主题覆盖 */
:root {
  /* 主色：竹院暖金 */
  --el-color-primary: #c7a067;
  --el-color-primary-light-3: #d4b98e;
  --el-color-primary-light-5: #e0cfaa;
  --el-color-primary-light-7: #ece4c7;
  --el-color-primary-light-8: #f2ede0;
  --el-color-primary-light-9: #f8f4ed;
  --el-color-primary-dark-2: #8f6738;

  /* 功能色 */
  --el-color-success: #5b8c5a;
  --el-color-success-light-3: #7ea87d;
  --el-color-success-light-5: #a1c4a0;
  --el-color-success-light-7: #c4dfc3;
  --el-color-success-light-8: #d5e9d4;
  --el-color-success-light-9: #e8f4e7;
  --el-color-success-dark-2: #3d6b3c;

  --el-color-warning: #c7956d;
  --el-color-warning-light-9: #f8f0e8;

  --el-color-danger: #b85c38;
  --el-color-danger-light-3: #cb8060;
  --el-color-danger-light-5: #dea588;
  --el-color-danger-light-7: #efc9b5;
  --el-color-danger-light-8: #f5dac9;
  --el-color-danger-light-9: #fbece2;
  --el-color-danger-dark-2: #8b3d22;

  --el-color-info: #8a7863;
  --el-color-info-light-9: #f2ede6;

  /* 字体 */
  --el-font-family: 'PingFang SC', 'Source Han Sans CN', 'Microsoft YaHei', sans-serif;
  --el-font-size-base: 0.92rem;

  /* 圆角 */
  --el-border-radius-base: 0.75rem;
  --el-border-radius-small: 0.5rem;
  --el-border-radius-round: 9999px;

  /* 过渡 */
  --el-transition-duration: 0.24s;
  --el-transition-duration-fast: 0.1s;

  /* 阴影 */
  --el-box-shadow: 0 12px 22px rgba(47, 58, 48, 0.12);
  --el-box-shadow-light: 0 6px 16px rgba(47, 58, 48, 0.08);
}

/* 午夜档案馆主题覆盖 */
[data-theme="midnight-archive"] {
  --el-color-primary: #d7b37a;
  --el-color-primary-light-3: #e0c496;
  --el-color-primary-light-5: #e8d5b0;
  --el-color-primary-light-7: #f0e3c8;
  --el-color-primary-light-8: #f4ebd6;
  --el-color-primary-light-9: #f9f3e8;
  --el-color-primary-dark-2: #b8954f;
  --el-bg-color: #090705;
  --el-bg-color-overlay: #19120f;
  --el-text-color-primary: #f3e9d7;
  --el-text-color-regular: #b4a692;
  --el-border-color: rgba(199, 160, 103, 0.18);
  --el-fill-color-blank: #19120f;
}

/* 竹院主题覆盖 */
[data-theme="bamboo-monastery"] {
  --el-color-primary: #b8944f;
  --el-color-primary-light-3: #ccaa6e;
  --el-color-primary-light-5: #dcc08e;
  --el-color-primary-light-7: #ead5ad;
  --el-color-primary-light-8: #f0e0bc;
  --el-color-primary-light-9: #f7efd7;
  --el-color-primary-dark-2: #8f6738;
  --el-bg-color: #f4f0e5;
  --el-bg-color-overlay: #ffffff;
  --el-text-color-primary: #1b2821;
  --el-text-color-regular: #324338;
  --el-border-color: rgba(88, 100, 81, 0.16);
  --el-fill-color-blank: #ffffff;
}
```

- [ ] **Step 2: 在 main.ts 中引入主题文件**

在 `frontend/src/main.ts` 的 import 区域添加（在第 11 行 `import './dizhi/dizhi.css'` 之后）：

```typescript
import './styles/element-theme.css'
```

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

Expected: 构建成功

- [ ] **Step 4: 提交**

```bash
git add frontend/src/styles/element-theme.css frontend/src/main.ts
git commit -m "feat: 添加 Element Plus 竹院风主题覆盖"
```

---

## Task 3: 创建 useButtonState composable

**Files:**
- Create: `frontend/src/composables/useButtonState.ts`

- [ ] **Step 1: 创建 composable**

创建 `frontend/src/composables/useButtonState.ts`：

```typescript
import { ref, readonly } from 'vue'

export type ButtonState = 'idle' | 'loading' | 'success' | 'error'

const FEEDBACK_DURATION = 2000

export function useButtonState() {
  const state = ref<ButtonState>('idle')
  let feedbackTimer: ReturnType<typeof setTimeout> | null = null

  function clearTimer() {
    if (feedbackTimer) {
      clearTimeout(feedbackTimer)
      feedbackTimer = null
    }
  }

  async function execute(action: () => Promise<unknown>): Promise<boolean> {
    if (state.value === 'loading') return false

    clearTimer()
    state.value = 'loading'

    try {
      await action()
      state.value = 'success'
      feedbackTimer = setTimeout(() => {
        state.value = 'idle'
        feedbackTimer = null
      }, FEEDBACK_DURATION)
      return true
    } catch {
      state.value = 'error'
      feedbackTimer = setTimeout(() => {
        state.value = 'idle'
        feedbackTimer = null
      }, FEEDBACK_DURATION)
      return false
    }
  }

  function reset() {
    clearTimer()
    state.value = 'idle'
  }

  return {
    state: readonly(state),
    execute,
    reset,
  }
}
```

- [ ] **Step 2: 验证 TypeScript 编译通过**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | head -20
```

Expected: 无类型错误（或仅有与本次无关的已存在错误）

- [ ] **Step 3: 提交**

```bash
git add frontend/src/composables/useButtonState.ts
git commit -m "feat: 添加 useButtonState 按钮状态管理 composable"
```

---

## Task 4: 创建 LibraryButton 组件

**Files:**
- Create: `frontend/src/components/common/LibraryButton.vue`

- [ ] **Step 1: 创建组件**

创建 `frontend/src/components/common/LibraryButton.vue`：

```vue
<template>
  <el-tooltip
    :disabled="!disabled || !disabledReason"
    :content="disabledReason"
    placement="top"
  >
    <span class="library-button-wrapper" :class="{ 'is-block': block }">
      <el-button
        :type="elType"
        :size="size"
        :disabled="disabled"
        :class="[
          'library-button',
          `library-button--${type}`,
          { 'library-button--loading': isLoading },
          { 'library-button--success': isSuccess },
          { 'library-button--error': isError },
        ]"
        @click="handleClick"
      >
        <span class="library-button__content">
          <span
            v-if="showIcon"
            class="library-button__icon"
            aria-hidden="true"
          >
            <component :is="currentIcon" />
          </span>
          <span class="library-button__text">
            <template v-if="isLoading && loadingText">{{ loadingText }}</template>
            <template v-else-if="isSuccess">{{ successText }}</template>
            <template v-else-if="isError">{{ errorText }}</template>
            <slot v-else />
          </span>
        </span>
      </el-button>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { ElButton, ElTooltip } from 'element-plus'

type ButtonType = 'primary' | 'secondary' | 'danger' | 'ghost'
type ButtonSize = 'small' | 'default' | 'large'
type ButtonState = 'idle' | 'loading' | 'success' | 'error'

const props = withDefaults(defineProps<{
  type?: ButtonType
  size?: ButtonSize
  loading?: boolean
  loadingText?: string
  disabled?: boolean
  disabledReason?: string
  icon?: Component
  successText?: string
  errorText?: string
  state?: ButtonState
  block?: boolean
}>(), {
  type: 'primary',
  size: 'default',
  loading: false,
  loadingText: '',
  disabled: false,
  disabledReason: '',
  successText: '操作成功',
  errorText: '操作失败',
  state: 'idle',
  block: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const isLoading = computed(() => props.loading || props.state === 'loading')
const isSuccess = computed(() => props.state === 'success')
const isError = computed(() => props.state === 'error')

const elType = computed(() => {
  if (props.type === 'danger') return 'danger'
  if (props.type === 'primary') return 'primary'
  return 'default'
})

const currentIcon = computed(() => {
  if (isSuccess.value) return undefined
  if (isError.value) return undefined
  return props.icon
})

const showIcon = computed(() => {
  if (isSuccess.value || isError.value) return false
  return !!props.icon
})

function handleClick(event: MouseEvent) {
  if (props.disabled || isLoading.value) return
  emit('click', event)
}
</script>

<style scoped>
.library-button-wrapper {
  display: inline-flex;
}
.library-button-wrapper.is-block {
  display: flex;
  width: 100%;
}
.library-button-wrapper.is-block :deep(.el-button) {
  width: 100%;
}

/* 基础按钮增强 */
.library-button {
  transition:
    transform var(--motion-duration-fast, 0.24s) ease,
    box-shadow var(--motion-duration-fast, 0.24s) ease,
    filter var(--motion-duration-fast, 0.24s) ease,
    opacity var(--motion-duration-fast, 0.24s) ease !important;
}

/* 悬停：上浮 + 阴影增强 */
.library-button:hover:not(:disabled):not(.library-button--loading) {
  transform: translateY(-2px) !important;
  filter: brightness(1.1);
}

/* 点击：缩放 */
.library-button:active:not(:disabled):not(.library-button--loading) {
  transform: scale(0.97) translateY(0) !important;
}

/* primary 类型：竹院暖金渐变 */
.library-button--primary:not(:disabled) {
  background: linear-gradient(
    135deg,
    var(--el-color-primary) 0%,
    var(--el-color-primary-dark-2) 100%
  ) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* secondary 类型：白底 + 暖色边框 */
.library-button--secondary:not(:disabled) {
  background: rgba(255, 255, 255, 0.9) !important;
  border: 1px solid rgba(115, 124, 129, 0.16) !important;
  color: var(--el-text-color-primary, #1b2821) !important;
}

/* ghost 类型：无背景 */
.library-button--ghost {
  background: transparent !important;
  border-color: transparent !important;
  color: var(--el-text-color-regular, #b4a692) !important;
  box-shadow: none !important;
}
.library-button--ghost:hover:not(:disabled) {
  background: var(--el-color-primary-light-9, rgba(199, 160, 103, 0.08)) !important;
}

/* danger 类型 */
.library-button--danger:not(:disabled) {
  background: var(--el-color-danger) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* 禁用状态：带禁止感 */
.library-button:disabled {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
}

/* 加载状态：按钮宽度不变 */
.library-button--loading {
  cursor: wait !important;
  pointer-events: none;
}

/* 成功状态：绿色闪烁 */
@keyframes btn-success-flash {
  0% { box-shadow: 0 0 0 0 rgba(91, 140, 90, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(91, 140, 90, 0); }
  100% { box-shadow: 0 0 0 0 rgba(91, 140, 90, 0); }
}
.library-button--success {
  animation: btn-success-flash 0.6s ease-out !important;
}
.library-button--success:not(:disabled) {
  background: var(--el-color-success) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* 失败状态：红色 + 水平抖动 */
@keyframes btn-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-4px); }
  40% { transform: translateX(4px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(3px); }
}
.library-button--error {
  animation: btn-shake 0.4s ease-out !important;
}
.library-button--error:not(:disabled) {
  background: var(--el-color-danger) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* 内容布局 */
.library-button__content {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
.library-button__icon {
  display: inline-flex;
  font-size: 1.1em;
}
</style>
```

- [ ] **Step 2: 验证 TypeScript 编译通过**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | head -20
```

Expected: 无类型错误（或仅有已存在的无关错误）

- [ ] **Step 3: 提交**

```bash
git add frontend/src/components/common/LibraryButton.vue
git commit -m "feat: 添加 LibraryButton 通用按钮组件"
```

---

## Task 5: 替换简单页面按钮（NotFound、MyBookshelf）

**Files:**
- Modify: `frontend/src/views/NotFound.vue`
- Modify: `frontend/src/views/MyBookshelf.vue`

这是风险最低的替换，两个页面都只有简单的 `page-action-btn` 按钮。

- [ ] **Step 1: 替换 NotFound.vue 的按钮**

在 `frontend/src/views/NotFound.vue` 模板中，将所有 `<button class="page-action-btn page-action-btn--primary">` 和 `<button class="page-action-btn page-action-btn--secondary">` 替换为 `<LibraryButton>`。

具体步骤：
1. 在 `<script setup>` 中添加 `import LibraryButton from '@/components/common/LibraryButton.vue'`
2. 将 `<button class="page-action-btn page-action-btn--primary" @click="goHome">` 替换为 `<LibraryButton type="primary" @click="goHome">`
3. 将 `<button class="page-action-btn page-action-btn--secondary" @click="goBack">` 替换为 `<LibraryButton type="secondary" @click="goBack">`

- [ ] **Step 2: 替换 MyBookshelf.vue 的按钮**

在 `frontend/src/views/MyBookshelf.vue` 中：
1. 在 `<script setup>` 中添加 `import LibraryButton from '@/components/common/LibraryButton.vue'`
2. 将 `<button class="page-action-btn page-action-btn--primary" type="button" @click="goToSearch">` 替换为 `<LibraryButton type="primary" @click="goToSearch">`

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

Expected: 构建成功

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/NotFound.vue frontend/src/views/MyBookshelf.vue
git commit -m "refactor: 替换 NotFound 和 MyBookshelf 页面按钮为 LibraryButton"
```

---

## Task 6: 替换 Dashboard、DamageReports、InventoryAlerts 页面按钮

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/views/DamageReports.vue`
- Modify: `frontend/src/views/InventoryAlerts.vue`

这三个页面各只有 1 个简单按钮。

- [ ] **Step 1: 替换 Dashboard.vue**

1. 添加 `import LibraryButton from '@/components/common/LibraryButton.vue'`
2. 将 `<button class="page-action-btn page-action-btn--primary" @click="loadData">` 替换为 `<LibraryButton type="primary" @click="loadData">`

- [ ] **Step 2: 替换 DamageReports.vue**

1. 添加 import
2. 将 `<button class="page-action-btn page-action-btn--secondary" @click="refresh">` 替换为 `<LibraryButton type="secondary" @click="refresh">`

- [ ] **Step 3: 替换 InventoryAlerts.vue**

1. 添加 import
2. 将 `<button class="page-action-btn page-action-btn--primary" @click="loadAlerts">` 替换为 `<LibraryButton type="primary" @click="loadAlerts">`

- [ ] **Step 4: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/Dashboard.vue frontend/src/views/DamageReports.vue frontend/src/views/InventoryAlerts.vue
git commit -m "refactor: 替换 Dashboard/DamageReports/InventoryAlerts 按钮为 LibraryButton"
```

---

## Task 7: 替换 MyAccount 页面按钮（含禁用状态 + loading）

**Files:**
- Modify: `frontend/src/views/MyAccount.vue`

MyAccount 有 6 个按钮，部分带 `:disabled` 状态，适合测试禁用功能。

- [ ] **Step 1: 替换 MyAccount.vue 按钮**

1. 添加 `import LibraryButton from '@/components/common/LibraryButton.vue'`
2. 添加 `import { useButtonState } from '@/composables/useButtonState'`
3. 在 script setup 中创建状态：`const passwordState = useButtonState()` 和 `const exportState = useButtonState()`
4. 替换所有 6 个按钮：

| 原始 | 替换为 |
|------|--------|
| `<button class="page-action-btn page-action-btn--primary" :disabled="isSubmitting" @click="changePassword">` | `<LibraryButton type="primary" :loading="isSubmitting" disabled-reason="请先填写完整表单" @click="changePassword">` |
| `<button class="page-action-btn page-action-btn--secondary" @click="savePreferences">` | `<LibraryButton type="secondary" @click="savePreferences">` |
| 4 个导出按钮 (class `page-action-btn--secondary` 带 `:disabled="isExporting"`) | `<LibraryButton type="secondary" :loading="isExporting" disabled-reason="正在导出中..." @click="...">` |
| `<button class="page-action-btn page-action-btn--primary" :disabled="isExporting" @click="exportAllData">` | `<LibraryButton type="primary" :loading="isExporting" disabled-reason="正在导出中..." @click="exportAllData">` |

- [ ] **Step 2: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/MyAccount.vue
git commit -m "refactor: 替换 MyAccount 页面按钮为 LibraryButton，含禁用和 loading 状态"
```

---

## Task 8: 替换 MyBorrows、MyReservations 页面按钮

**Files:**
- Modify: `frontend/src/views/MyBorrows.vue`
- Modify: `frontend/src/views/MyReservations.vue`

- [ ] **Step 1: 替换 MyBorrows.vue**

3 个按钮（1 primary + 1 secondary + 1 primary）：
1. 添加 import
2. 逐个替换为 LibraryButton

- [ ] **Step 2: 替换 MyReservations.vue**

3 个按钮（2 secondary + 1 primary）：
1. 添加 import
2. 逐个替换为 LibraryButton

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/MyBorrows.vue frontend/src/views/MyReservations.vue
git commit -m "refactor: 替换 MyBorrows 和 MyReservations 按钮为 LibraryButton"
```

---

## Task 9: 替换 ForgotPassword 页面按钮（back-btn + query-btn）

**Files:**
- Modify: `frontend/src/views/ForgotPassword.vue`

- [ ] **Step 1: 替换按钮**

1. 添加 import
2. 将 `<button class="back-btn" @click="goBack">` 替换为 `<LibraryButton type="ghost" @click="goBack">`
3. 将 `<button class="query-btn" :disabled="isLoading || !studentId.trim()">` 替换为 `<LibraryButton type="primary" :loading="isLoading" :disabled="!studentId.trim()" disabled-reason="请输入学号" block>`

注意：query-btn 有 `type="submit"`，需要改为在 form 上使用 `@submit.prevent` 并让 LibraryButton 触发 submit 逻辑。

- [ ] **Step 2: 删除 ForgotPassword.vue 中废弃的 .back-btn 和 .query-btn 样式**

移除 `<style scoped>` 中 `.back-btn`（行 298-319）和 `.query-btn`（行 391-421）的 CSS 规则。

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/ForgotPassword.vue
git commit -m "refactor: 替换 ForgotPassword 页面按钮为 LibraryButton"
```

---

## Task 10: 替换 PurchaseSuggestions 页面按钮（submit-btn、vote-btn、admin-save-btn）

**Files:**
- Modify: `frontend/src/views/PurchaseSuggestions.vue`

这是最复杂的单页面迁移，有 5 种按钮类型。

- [ ] **Step 1: 替换按钮**

1. 添加 import LibraryButton 和 useButtonState
2. 创建 submitState = useButtonState()
3. 替换映射：

| 原始 class | 替换为 |
|---|---|
| `page-action-btn--secondary` @click="loadBoard" | `<LibraryButton type="secondary" @click="loadBoard">` |
| `page-action-btn--secondary` (tab 切换) | `<LibraryButton type="secondary">` |
| `page-action-btn--primary` (tab 切换) | `<LibraryButton type="primary">` |
| `submit-btn` :disabled="submitting" | `<LibraryButton type="primary" :loading="submitting" loading-text="提交中..." success-text="建议已提交" error-text="提交失败" @click="...">` |
| `vote-btn` | `<LibraryButton type="secondary" size="small">` |
| `admin-save-btn` | `<LibraryButton type="primary" size="small">` |

- [ ] **Step 2: 删除废弃的 scoped 样式**

移除 `.submit-btn`、`.vote-btn`、`.admin-save-btn` 相关的 scoped CSS 规则（行 872-924、1177、1198-1200），以及移动端覆盖中对应的按钮样式（行 1297 附近）。

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/PurchaseSuggestions.vue
git commit -m "refactor: 替换 PurchaseSuggestions 页面所有按钮为 LibraryButton"
```

---

## Task 11: 替换 BookDetail 页面按钮（含 damage 类型）

**Files:**
- Modify: `frontend/src/views/BookDetail.vue`

- [ ] **Step 1: 替换按钮**

1. 添加 import
2. 替换所有 page-action-btn 按钮：

| 原始 | 替换为 |
|---|---|
| `page-action-btn--secondary` @click="goBack" (2处) | `<LibraryButton type="ghost" @click="goBack">` |
| `page-action-btn--primary` (借阅/预约) | `<LibraryButton type="primary">` |
| `page-action-btn--secondary` (另一个操作) | `<LibraryButton type="secondary">` |
| `page-action-btn--damage` (损坏报告) | `<LibraryButton type="danger">` |
| `page-action-btn--primary` :disabled="isSubmitting" @click="submitReview" | `<LibraryButton type="primary" :loading="isSubmitting" success-text="评论已提交" error-text="评论提交失败" @click="submitReview">` |

- [ ] **Step 2: 删除 BookDetail.vue 中废弃的 .page-action-btn--damage 样式**

移除行 925-931 的 `.page-action-btn--damage` 和 `.page-action-btn--damage:hover` CSS 规则。

- [ ] **Step 3: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/BookDetail.vue
git commit -m "refactor: 替换 BookDetail 页面按钮为 LibraryButton"
```

---

## Task 12: 替换 BookSearch 和 UserManagement 页面按钮（含分页）

**Files:**
- Modify: `frontend/src/views/BookSearch.vue`
- Modify: `frontend/src/views/UserManagement.vue`

- [ ] **Step 1: 替换 BookSearch.vue 按钮**

1. 添加 import
2. 替换 `page-action-btn` 按钮：
   - `--secondary` @click="resetFilters" → `<LibraryButton type="secondary">`
   - `--primary` :disabled="loading" @click="saveCurrentSearch" → `<LibraryButton type="primary" :loading="loading">`
3. 替换分页按钮（pagination-btn 和 pagination-number）为 `<el-pagination>` 组件：
   - 导入 `import { ElPagination } from 'element-plus'`
   - 将整个分页 div 替换为 `<el-pagination>`
   - 配置：`:current-page`、`:page-size`、`:total`、`layout="prev, pager, next"`
   - 覆盖 el-pagination 的主题样式使其融入竹院风

- [ ] **Step 2: 替换 UserManagement.vue 按钮**

1. 添加 import
2. 替换 3 个 page-action-btn 按钮
3. 替换分页按钮为 `<el-pagination>`

- [ ] **Step 3: 删除废弃的 scoped 样式**

移除两个文件中 `.pagination-btn`、`.pagination-number` 相关的 CSS。

- [ ] **Step 4: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/BookSearch.vue frontend/src/views/UserManagement.vue
git commit -m "refactor: 替换 BookSearch 和 UserManagement 按钮和分页为 LibraryButton + el-pagination"
```

---

## Task 13: 替换组件中的按钮（TopNav、Panels、BookGrid、LoginAuthCard）

**Files:**
- Modify: `frontend/src/components/navigation/TopNav.vue`
- Modify: `frontend/src/components/panels/SearchHistoryPanel.vue`
- Modify: `frontend/src/components/panels/NotificationPanel.vue`
- Modify: `frontend/src/components/home/BookGrid.vue`
- Modify: `frontend/src/components/login/LoginAuthCard.vue`

- [ ] **Step 1: 替换 TopNav.vue**

1. 添加 import
2. 替换 `login-btn` → `<LibraryButton type="primary" size="small">`
3. 替换 `icon-btn` → `<LibraryButton type="ghost" size="small" :icon="...">`
4. 替换 `popup-action-btn` → `<LibraryButton type="ghost" size="small">`

注意：icon-btn 和 popup-action-btn 嵌套在弹出面板中，需要确保 LibraryButton 的尺寸和行为与原有一致。

- [ ] **Step 2: 替换 SearchHistoryPanel.vue**

1. 添加 import
2. 替换 `icon-btn` → `<LibraryButton type="ghost" size="small">`
3. 替换 `popup-action-btn` → `<LibraryButton type="ghost" size="small">`

- [ ] **Step 3: 替换 NotificationPanel.vue**

1. 添加 import
2. 替换同 SearchHistoryPanel

- [ ] **Step 4: 替换 BookGrid.vue 的 toggle-btn**

1. 添加 import
2. 将 `<button class="toggle-btn" :class="{ active: ... }">` 替换为 `<LibraryButton type="ghost" size="small">`
   - 需要在组件中通过 CSS 处理 active 状态

- [ ] **Step 5: 替换 LoginAuthCard.vue 的 submit-btn**

1. 添加 import
2. 将 `<button class="submit-btn" type="submit">` 替换为 `<LibraryButton type="primary" size="large" block :loading="...">`
   - form 的 submit 行为需要调整：将 LibraryButton 的 click 事件关联到 form submit

- [ ] **Step 6: 验证构建通过**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 7: 提交**

```bash
git add frontend/src/components/navigation/TopNav.vue frontend/src/components/panels/SearchHistoryPanel.vue frontend/src/components/panels/NotificationPanel.vue frontend/src/components/home/BookGrid.vue frontend/src/components/login/LoginAuthCard.vue
git commit -m "refactor: 替换组件中的按钮为 LibraryButton（TopNav/Panels/BookGrid/LoginAuthCard）"
```

---

## Task 14: 清理 app.css 中废弃的按钮样式

**Files:**
- Modify: `frontend/src/styles/app.css`

- [ ] **Step 1: 清理全局按钮样式**

从 `frontend/src/styles/app.css` 中移除以下已不再使用的 CSS 规则：

1. `.page-action-btn` 基础及变体（行 1774-1803）
2. `.login-btn`（行 1421-1436）
3. `.icon-btn`（行 126-141）
4. `.toggle-btn`（行 1676-1695）
5. `.popup-action-btn`（行 264-277）
6. 以上 class 的主题覆盖：
   - Exhibition Polish 覆盖（行 1956-1962, 2225-2235）
   - Midnight Archive 覆盖（行 2412-2418, 2432-2438, 2484-2490, 2566-2589）
   - Bamboo Monastery 覆盖（行 3521-3551, 3961-3967, 3981-3993, 4043-4045）
7. 移动端按钮覆盖（行 1867-1869）

同时移除 `login-auth-card.css` 中的 `.submit-btn` 规则（行 357-385）。

- [ ] **Step 2: 验证构建通过且无样式回归**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -10
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/styles/app.css
git commit -m "chore: 清理废弃的按钮 CSS 样式"
```

---

## Task 15: 最终验证和样式微调

**Files:**
- 可能修改: `frontend/src/styles/element-theme.css`
- 可能修改: `frontend/src/components/common/LibraryButton.vue`

- [ ] **Step 1: 启动开发服务器验证所有页面**

```bash
cd frontend && npx vite --port 5173
```

逐页检查以下页面，确认按钮显示和交互正常：

1. `/` — 首页（SuperButton 不受影响）
2. `/login` — 登录页（LibraryButton 替换 submit-btn）
3. `/forgot-password` — 忘记密码页（back-btn + query-btn）
4. `/search` — 图书搜索页（筛选按钮 + 分页）
5. `/book/:id` — 图书详情页（操作按钮 + damage 按钮）
6. `/dashboard` — 仪表盘
7. `/purchase-suggestions` — 采购建议（submit + vote + admin-save）
8. `/my-borrows` — 我的借阅
9. `/my-reservations` — 我的预约
10. `/my-account` — 我的账户（禁用状态 + loading）
11. `/my-bookshelf` — 我的书架
12. `/user-management` — 用户管理（分页）
13. `/inventory-alerts` — 库存预警
14. `/damage-reports` — 损坏报告
15. `/not-found` — 404 页面

- [ ] **Step 2: 检查双主题切换**

1. 切换到 "午夜档案馆" 主题，验证按钮颜色正确
2. 切换到 "竹院" 主题，验证按钮颜色正确
3. 确认 Element Plus 的主题变量跟随主题切换

- [ ] **Step 3: 检查响应式布局**

在移动端宽度（< 768px）下验证按钮布局，特别是 MyAccount 中多个按钮的排列。

- [ ] **Step 4: 修复发现的问题并提交**

```bash
git add -A
git commit -m "fix: 按钮状态系统最终样式微调"
```
