# 首页 HeroSection 视觉重设计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于已冻结的首页设计 spec，重构 `HeroSection` 的结构、文案、样式与动效，让首页首屏从“概念展示”提升为“更现代、更精致、更像正式产品入口”的版本，同时保留双时钟、山水、雾气、竹影与现有搜索行为。

**Architecture:** 保持 `HeroSection.vue` 作为首页首屏的单一承载组件，不改动 `App.vue` 的搜索路由入口与业务流，只在组件内部重排模板层级、更新 i18n 文案、提炼视觉变量、重写 scoped 样式并重调 GSAP 入场/景深参数。交互逻辑继续复用现有 `useSearchAutocomplete`、`bookApi.getSearchSuggestions()` 和 `emit('search')` 模式，确保结果页跳转方式不变。

**Tech Stack:** Vue 3 SFC、TypeScript、vue-i18n、GSAP/ScrollTrigger、Vitest、Vue Test Utils、CSS Variables

---

## 文件清单

**新增文件：**
- `frontend/src/test/components/home/HeroSection.spec.ts` - 首页 HeroSection 组件级测试，覆盖新结构、按钮搜索、热门标签与自动补全提交流程

**修改文件：**
- `frontend/src/components/home/HeroSection.vue:1-166,169-463,466-1220` - 重排首屏结构、保留搜索逻辑、重写视觉层级与动效顺序
- `frontend/src/locales/modules/hero.ts:1-58` - 同步中英文标题、副文案、价值卡文案、搜索说明文案
- `frontend/src/styles/variables.css:27-65` - 新增首页 HeroSection 的尺寸、阴影、玻璃层与景深强度变量

**回归验证文件（只运行，不修改）：**
- `frontend/src/test/App.spec.ts` - 确认 `HeroSection @search` 到 `BookSearch` 的集成行为未回退
- `frontend/package.json` - 使用现有 `test:run` / `build` / `dev` 脚本，不新增脚本

**明确不改：**
- `frontend/src/App.vue` - 现有 `goToSearch()` 已满足需求，不要为这次首屏改版改动路由逻辑
- `frontend/src/styles/app.css` - 除非在实现中确认出现首页全局布局溢出，否则不动全局样式

---

## Task 1: 锁定新文案与首屏结构骨架

**Files:**
- Create: `frontend/src/test/components/home/HeroSection.spec.ts`
- Modify: `frontend/src/components/home/HeroSection.vue:1-166,169-259`
- Modify: `frontend/src/locales/modules/hero.ts:1-58`

- [ ] **Step 1: 先写会失败的 HeroSection 组件测试**

```ts
import { defineComponent, h, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import heroMessages from '@/locales/modules/hero'

const locale = ref<'zh' | 'en'>('zh')
const getSearchSuggestionsMock = vi.fn()
const contextRevertMock = vi.fn()
const mountedWrappers: Array<ReturnType<typeof mount>> = []

const resolveMessage = (key: string) => key
  .split('.')
  .reduce<any>((current, part) => current?.[part], heroMessages[locale.value]) ?? key

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    locale,
    t: (key: string) => resolveMessage(key),
  }),
}))

vi.mock('@/api/bookApi', () => ({
  bookApi: {
    getSearchSuggestions: getSearchSuggestionsMock,
  },
}))

vi.mock('@/motion', () => ({
  prefersReducedMotion: () => true,
  gsap: {
    utils: { toArray: () => [] },
    set: vi.fn(),
    to: vi.fn(),
    timeline: vi.fn(() => ({ fromTo: vi.fn().mockReturnThis() })),
    context: (callback: () => void) => {
      callback()
      return { revert: contextRevertMock }
    },
  },
}))

const createClockStub = (name: string) => defineComponent({
  name,
  setup: () => () => h('div', { 'data-component': name }),
})

vi.mock('@/dizhi/DizhiClock.vue', () => ({ default: createClockStub('DizhiClock') }))
vi.mock('@/dizhi/ModernClock.vue', () => ({ default: createClockStub('ModernClock') }))

const mountHero = async () => {
  const module = await import('@/components/home/HeroSection.vue')
  const wrapper = mount(module.default)
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('HeroSection', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    locale.value = 'zh'
    getSearchSuggestionsMock.mockReset()
    getSearchSuggestionsMock.mockResolvedValue({
      data: {
        success: true,
        data: ['设计模式', '设计心理学'],
      },
    })
  })

  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it('renders the refined hero hierarchy with rail, stage, search panel, and value cards', async () => {
    const wrapper = await mountHero()

    expect(wrapper.find('.hero-rail').exists()).toBe(true)
    expect(wrapper.find('.hero-stage').exists()).toBe(true)
    expect(wrapper.find('.hero-intro').exists()).toBe(true)
    expect(wrapper.find('.hero-search-panel').exists()).toBe(true)
    expect(wrapper.findAll('.hero-value-card')).toHaveLength(3)
    expect(wrapper.text()).toContain('一次检索，直接进入馆藏结果与借阅动作')
  })

  it('emits trimmed keyword from the primary CTA button', async () => {
    const wrapper = await mountHero()

    await wrapper.get('.search-input').setValue('  算法导论  ')
    await wrapper.get('.search-btn').trigger('click')

    expect(wrapper.emitted('search')).toEqual([['算法导论']])
  })

  it('uses a quick tag as a direct search entry', async () => {
    const wrapper = await mountHero()

    await wrapper.get('.tag-btn').trigger('click')

    expect(wrapper.emitted('search')).toEqual([['人工智能']])
  })

  it('selects an autocomplete suggestion and emits it', async () => {
    const wrapper = await mountHero()

    await wrapper.get('.search-input').setValue('设计')
    vi.advanceTimersByTime(180)
    await flushPromises()

    expect(getSearchSuggestionsMock).toHaveBeenCalledWith('设计', 6)

    const suggestionButtons = wrapper.findAll('.hero-suggestion-item')
    expect(suggestionButtons).toHaveLength(2)

    await suggestionButtons[1].trigger('mousedown')
    expect(wrapper.emitted('search')).toEqual([['设计心理学']])
  })
})
```

- [ ] **Step 2: 运行测试，确认新结构断言先失败**

Run:

```bash
npm --prefix frontend run test:run -- src/test/components/home/HeroSection.spec.ts
```

Expected: FAIL，报错包含以下至少一项：
- `Unable to get .hero-stage`
- `expected [] to have a length of 3`（因为还没有 `.hero-value-card`）
- 标题文本仍是旧版本，不匹配新文案

- [ ] **Step 3: 按 spec 更新文案与模板结构，但保留搜索行为**

在 `frontend/src/locales/modules/hero.ts` 中把 `hero` 文案改为：

```ts
export default {
  zh: {
    hero: {
      clockLabel: '时序校准',
      libraryTimeLabel: '开馆状态',
      kicker: '读者高频检索入口',
      title: '一次检索，直接进入馆藏结果与借阅动作',
      subtitle: '首页只负责输入条件并进入统一结果页。查询条件、分页与排序会保留在链接里，方便刷新、分享与连续追书。',
      metaTrust: '结果可信',
      metaTrustValue: '只显示后端真实馆藏',
      metaContinuity: '状态可回放',
      metaContinuityValue: '链接可保留查询、分页与排序',
      metaNextStep: '后续动作直达',
      metaNextStepValue: '搜索后可继续详情、借阅或预约',
      searchTitle: '统一结果页入口',
      searchNote: '支持书名、作者、ISBN 与常用检索词',
      searchPlaceholder: '输入书名、作者或 ISBN',
      searchBtn: '进入结果页',
      trendingLabel: '常用入口',
      tag1Label: '人工智能',
      tag1Query: '人工智能',
      tag2Label: '设计模式',
      tag2Query: '设计模式',
      tag3Label: '数字人文',
      tag3Query: '数字人文',
      tag4Label: '古典文学',
      tag4Query: '古典文学',
    },
  },
  en: {
    hero: {
      clockLabel: 'Temporal Index',
      libraryTimeLabel: 'Library Hours',
      kicker: 'Reader Search Flow',
      title: 'Search once and move directly into results and borrowing actions.',
      subtitle: 'The home page only captures search conditions and opens the unified results page. Query, pagination, and sorting stay in the URL for refresh, sharing, and continuous lookup.',
      metaTrust: 'Trusted Results',
      metaTrustValue: 'Backend records only',
      metaContinuity: 'Replayable State',
      metaContinuityValue: 'Query, page, and sort stay in the URL',
      metaNextStep: 'Direct Next Step',
      metaNextStepValue: 'Continue to details, borrow, or reserve',
      searchTitle: 'Unified results entry',
      searchNote: 'Search by title, author, ISBN, and common queries',
      searchPlaceholder: 'Search title, author, or ISBN',
      searchBtn: 'Open results',
      trendingLabel: 'Quick picks',
      tag1Label: 'AI',
      tag1Query: 'Artificial Intelligence',
      tag2Label: 'Patterns',
      tag2Query: 'Design Patterns',
      tag3Label: 'Digital Humanities',
      tag3Query: 'Digital Humanities',
      tag4Label: 'Classics',
      tag4Query: 'Classical Literature',
    },
  },
}
```

把 `frontend/src/components/home/HeroSection.vue` 顶部结构改成下面这个骨架，注意只重排结构，不改 `submitSearch()` / `applyTag()` / `applySuggestion()` 的行为：

```vue
<div class="hero-content">
  <div class="hero-stage">
    <div ref="copyRef" class="hero-copy hero-intro">
      <p class="hero-kicker">{{ t('hero.kicker') }}</p>
      <h1 class="hero-title">{{ t('hero.title') }}</h1>
      <p class="hero-subtitle">{{ t('hero.subtitle') }}</p>
    </div>

    <div ref="searchPanelRef" class="hero-search-panel">
      <div class="hero-search-header">
        <span class="hero-search-title">{{ t('hero.searchTitle') }}</span>
        <span class="hero-search-note">{{ t('hero.searchNote') }}</span>
      </div>

      <div ref="searchComboboxRef" class="search-combobox">
        <div class="search-bar" role="search" aria-label="Search the library collection">
          <span class="material-symbols-outlined search-icon" aria-hidden="true">search</span>
          <input
            v-model="searchQuery"
            type="text"
            class="search-input"
            role="combobox"
            aria-autocomplete="list"
            aria-label="Search title, author, or ISBN"
            :aria-expanded="searchAutocompleteOpen"
            :aria-controls="searchAutocompleteListId"
            :aria-activedescendant="searchAutocompleteActiveIndex >= 0 ? getSearchAutocompleteOptionId(searchAutocompleteActiveIndex) : undefined"
            autocomplete="off"
            spellcheck="false"
            :placeholder="t('hero.searchPlaceholder')"
            @focus="openSearchAutocompleteIfAvailable()"
            @blur="handleSearchBlur"
            @input="handleSearchInput"
            @keydown="handleSearchKeydown"
          />
          <button class="search-btn" @click="submitSearch">{{ t('hero.searchBtn') }}</button>
        </div>

        <div
          v-if="searchAutocompleteLoading || searchAutocompleteOpen"
          :id="searchAutocompleteListId"
          class="hero-suggestion-popover"
          role="listbox"
        >
          <div v-if="searchAutocompleteLoading && searchAutocompleteSuggestions.length === 0" class="hero-suggestion-loading">
            <span class="material-symbols-outlined" aria-hidden="true">hourglass_top</span>
            <span>{{ searchLoadingLabel }}</span>
          </div>
          <button
            v-for="(suggestion, index) in searchAutocompleteSuggestions"
            v-else
            :id="getSearchAutocompleteOptionId(index)"
            :key="`${suggestion}-${index}`"
            :class="['hero-suggestion-item', { 'hero-suggestion-item--active': index === searchAutocompleteActiveIndex }]"
            type="button"
            role="option"
            :aria-selected="index === searchAutocompleteActiveIndex"
            @mousedown.prevent="applySuggestion(suggestion)"
          >
            <span class="material-symbols-outlined" aria-hidden="true">history</span>
            <span>{{ suggestion }}</span>
          </button>
        </div>
      </div>

      <div class="trending-tags">
        <span class="trending-label">{{ t('hero.trendingLabel') }}</span>
        <button
          v-for="tag in trendingTags"
          :key="tag.label"
          class="tag-btn"
          @click="applyTag(tag.query)"
        >
          {{ tag.label }}
        </button>
      </div>
    </div>

    <div class="hero-value-grid">
      <div class="hero-value-card">
        <span class="hero-meta-key">{{ t('hero.metaTrust') }}</span>
        <span class="hero-meta-value">{{ t('hero.metaTrustValue') }}</span>
      </div>
      <div class="hero-value-card">
        <span class="hero-meta-key">{{ t('hero.metaContinuity') }}</span>
        <span class="hero-meta-value">{{ t('hero.metaContinuityValue') }}</span>
      </div>
      <div class="hero-value-card">
        <span class="hero-meta-key">{{ t('hero.metaNextStep') }}</span>
        <span class="hero-meta-value">{{ t('hero.metaNextStepValue') }}</span>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: 重新运行 HeroSection 组件测试，确认结构和交互都通过**

Run:

```bash
npm --prefix frontend run test:run -- src/test/components/home/HeroSection.spec.ts
```

Expected: PASS，4 个用例全部通过。

- [ ] **Step 5: Commit**

```bash
git add frontend/src/test/components/home/HeroSection.spec.ts frontend/src/components/home/HeroSection.vue frontend/src/locales/modules/hero.ts
git commit -m "feat: 重构首页 HeroSection 结构与文案骨架"
```

---

## Task 2: 提炼视觉变量并重写首屏层级样式

**Files:**
- Modify: `frontend/src/styles/variables.css:27-65`
- Modify: `frontend/src/components/home/HeroSection.vue:466-1220`

- [ ] **Step 1: 在全局变量里加入 HeroSection 专用视觉 token**

把下面这组变量插入到 `frontend/src/styles/variables.css` 现有 `--home-*` 变量附近：

```css
  --hero-rail-width: clamp(13.5rem, 18vw, 15.75rem);
  --hero-stage-width: min(100%, 50rem);
  --hero-panel-radius: 2rem;
  --hero-panel-padding: clamp(1.25rem, 3vw, 1.75rem);
  --hero-search-height: 4.75rem;
  --hero-shell-gap: clamp(1.25rem, 2vw, 2.2rem);
  --hero-surface: linear-gradient(180deg, rgba(250, 248, 242, 0.94) 0%, rgba(235, 241, 232, 0.86) 100%);
  --hero-surface-strong: linear-gradient(180deg, rgba(252, 249, 243, 0.92) 0%, rgba(241, 236, 226, 0.82) 100%);
  --hero-card-surface: linear-gradient(180deg, rgba(255, 251, 245, 0.76) 0%, rgba(242, 237, 228, 0.68) 100%);
  --hero-card-border: rgba(103, 110, 93, 0.14);
  --hero-search-shadow: 0 22px 42px rgba(49, 59, 50, 0.12);
  --hero-card-shadow: 0 16px 30px rgba(43, 52, 44, 0.08);
  --hero-depth-opacity: 0.74;
```

- [ ] **Step 2: 重写 HeroSection 的主布局与搜索主角样式**

把 `frontend/src/components/home/HeroSection.vue` 里的结构样式改成下面这一组方向，重点是“左侧信息塔收敛、标题雅致、搜索绝对主角、三卡下沉承接”：

```css
.hero-shell {
  position: relative;
  z-index: 1;
  width: min(100% - 2 * var(--page-gutter), var(--page-width-home));
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(12.5rem, var(--hero-rail-width)) minmax(0, 1fr);
  gap: var(--hero-shell-gap);
  align-items: stretch;
}

.hero-content {
  position: relative;
  overflow: hidden;
  padding: clamp(2rem, 4vw, 3.5rem);
  border-radius: 2.4rem;
  background: var(--hero-surface);
  border: 1px solid rgba(103, 110, 93, 0.18);
  box-shadow: var(--home-shadow-deep);
}

.hero-stage {
  position: relative;
  z-index: 1;
  display: grid;
  gap: clamp(1.25rem, 2vw, 1.75rem);
  max-width: var(--hero-stage-width);
}

.hero-intro {
  max-width: 42rem;
}

.hero-kicker {
  margin: 0 0 var(--space-3);
  font-size: 0.76rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: rgba(118, 92, 58, 0.76);
}

.hero-title {
  margin: 0;
  max-width: 14ch;
  font-family: var(--font-headline);
  font-size: clamp(2.8rem, 4vw, 4rem);
  line-height: 1.1;
  letter-spacing: -0.035em;
  color: var(--home-ink);
  text-wrap: balance;
}

.hero-subtitle {
  margin: var(--space-4) 0 0;
  max-width: 42rem;
  font-size: 1rem;
  line-height: 1.9;
  color: rgba(54, 67, 55, 0.8);
}

.hero-search-panel {
  position: relative;
  overflow: hidden;
  padding: var(--hero-panel-padding);
  border-radius: calc(var(--hero-panel-radius) + 0.15rem);
  background: var(--hero-surface-strong);
  border: 1px solid rgba(103, 110, 93, 0.16);
  box-shadow: var(--hero-search-shadow);
}

.hero-search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: var(--hero-search-height);
  padding: 0.6rem 0.65rem 0.6rem 1.2rem;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.96);
  border: 1px solid rgba(107, 113, 96, 0.16);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 16px 30px rgba(49, 59, 50, 0.08);
  transition: border-color 0.24s ease, box-shadow 0.24s ease, background 0.24s ease;
}

.search-bar:focus-within {
  border-color: rgba(106, 138, 104, 0.38);
  box-shadow:
    0 0 0 4px rgba(133, 160, 131, 0.16),
    0 22px 40px rgba(61, 70, 57, 0.16);
}

.search-btn {
  min-width: 9rem;
  min-height: 3.35rem;
  padding: 0 1.45rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #d9bf8f 0%, #bb975f 48%, #efe0bf 100%);
  color: #1c1a16;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 18px 34px rgba(121, 93, 57, 0.16);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.hero-value-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.hero-value-card {
  position: relative;
  overflow: hidden;
  padding: var(--space-4);
  border-radius: calc(var(--radius-lg) + 0.1rem);
  background: var(--hero-card-surface);
  border: 1px solid var(--hero-card-border);
  box-shadow: var(--hero-card-shadow);
}
```

- [ ] **Step 3: 同步收敛背景层和移动端规则，确保手机端以清晰度优先**

把响应式段落调整为下面这个方向，重点是移动端弱化景深、双时钟保留但重新排布、搜索按钮整行显示：

```css
.hero-landscape {
  opacity: var(--hero-depth-opacity);
}

@media (max-width: 1180px) {
  .hero-value-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .hero-shell {
    grid-template-columns: 1fr;
  }

  .hero-rail {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-3);
  }

  .hero-bamboo {
    opacity: 0.42;
  }
}

@media (max-width: 767px) {
  .hero-section::before {
    top: 1.5rem;
    height: 28rem;
    filter: blur(16px);
  }

  .hero-content {
    padding: var(--space-6);
    border-radius: var(--radius-xl);
  }

  .hero-title {
    max-width: none;
    font-size: clamp(2.2rem, 9vw, 3.1rem);
  }

  .hero-subtitle {
    font-size: 0.95rem;
    line-height: 1.8;
  }

  .hero-rail {
    grid-template-columns: 1fr;
  }

  .hero-search-header,
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    min-height: auto;
    padding: var(--space-3);
    border-radius: calc(var(--radius-xl) + 0.1rem);
  }

  .search-btn {
    width: 100%;
  }

  .hero-bamboo {
    opacity: 0.24;
  }

  .hero-mist {
    filter: blur(18px);
  }
}
```

- [ ] **Step 4: 运行测试与构建，确保样式重写没有破坏结构与类型**

Run:

```bash
npm --prefix frontend run test:run -- src/test/components/home/HeroSection.spec.ts src/test/App.spec.ts
npm --prefix frontend run build
```

Expected:
- `HeroSection.spec.ts` PASS
- `App.spec.ts` PASS
- `vue-tsc && vite build` 成功，无类型错误、无构建失败

- [ ] **Step 5: Commit**

```bash
git add frontend/src/styles/variables.css frontend/src/components/home/HeroSection.vue
git commit -m "feat: 提升首页 HeroSection 视觉层级与搜索主角样式"
```

---

## Task 3: 重调入场顺序、景深滚动，并完成浏览器验收

**Files:**
- Modify: `frontend/src/components/home/HeroSection.vue:276-455,503-556,1091-1219`
- Test: `frontend/src/test/components/home/HeroSection.spec.ts`

- [ ] **Step 1: 调整 GSAP 入场顺序，让戏剧感来自层次而不是标题体量**

把 `onMounted()` 里的动画目标从旧的 `.hero-meta-card` 切到新的 `.hero-value-card`，并把入场顺序调整为“信息塔 → 标题说明 → 搜索面板 → 价值卡”：

```ts
onMounted(() => {
  document.addEventListener('mousedown', handleDocumentPointerDown)

  if (prefersReducedMotion() || !heroRef.value) {
    return
  }

  requestAnimationFrame(() => {
    if (!heroRef.value) return

    const isCompactViewport = window.matchMedia('(max-width: 767px)').matches
    const parallaxFactor = isCompactViewport ? 0.35 : 1

    const context = gsap.context(() => {
      const railCards = gsap.utils.toArray<HTMLElement>('.hero-rail-card').filter(el => el instanceof HTMLElement && el.isConnected)
      const valueCards = gsap.utils.toArray<HTMLElement>('.hero-value-card').filter(el => el instanceof HTMLElement && el.isConnected)
      const introTargets = [copyRef.value, searchPanelRef.value, ...valueCards].filter(Boolean)

      if (introTargets.length > 0) {
        gsap.set(introTargets, { willChange: 'transform, opacity, filter' })
      }

      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .fromTo(
          railCards,
          { autoAlpha: 0, x: -20, y: 18, filter: 'blur(12px)' },
          { autoAlpha: 1, x: 0, y: 0, filter: 'blur(0px)', duration: 0.68, stagger: 0.08, clearProps: 'filter' },
        )
        .fromTo(
          copyRef.value,
          { autoAlpha: 0, y: 28, filter: 'blur(14px)' },
          { autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 0.74, clearProps: 'filter' },
          0.1,
        )
        .fromTo(
          searchPanelRef.value,
          { autoAlpha: 0, y: 24, filter: 'blur(12px)' },
          { autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 0.6, clearProps: 'filter' },
          0.24,
        )
        .fromTo(
          valueCards,
          { autoAlpha: 0, y: 18, filter: 'blur(10px)' },
          { autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 0.5, stagger: 0.08, clearProps: 'filter' },
          0.38,
        )

      if (!isCompactViewport && landscapeRef.value) {
        gsap.to(landscapeRef.value, {
          yPercent: -6 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.65,
          },
        })
      }

      if (!isCompactViewport && leftBambooRef.value) {
        gsap.to(leftBambooRef.value, {
          yPercent: -8 * parallaxFactor,
          xPercent: -2 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.5,
          },
        })
      }

      if (!isCompactViewport && rightBambooRef.value) {
        gsap.to(rightBambooRef.value, {
          yPercent: -6 * parallaxFactor,
          xPercent: 2 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.48,
          },
        })
      }

      if (!isCompactViewport && mistBackRef.value) {
        gsap.to(mistBackRef.value, {
          yPercent: -10 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.7,
          },
        })
      }

      if (!isCompactViewport && mistMidRef.value) {
        gsap.to(mistMidRef.value, {
          yPercent: -6 * parallaxFactor,
          xPercent: 2 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.6,
          },
        })
      }

      if (!isCompactViewport && mistFrontRef.value) {
        gsap.to(mistFrontRef.value, {
          yPercent: -14 * parallaxFactor,
          xPercent: -2 * parallaxFactor,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.72,
          },
        })
      }
    }, heroRef.value)

    cleanupMotion = () => context.revert()
  })
})
```

- [ ] **Step 2: 同步弱化移动端背景存在感，保留戏剧性但不干扰阅读**

把背景层动画参数收敛成下面的方向：

```css
.hero-mist--back {
  animation: heroMistDrift 24s ease-in-out infinite;
}

.hero-mist--mid {
  animation: heroMistDrift 28s ease-in-out infinite reverse;
}

.hero-mist--front {
  opacity: 0.52;
  animation: heroMistDrift 30s ease-in-out infinite;
}

.hero-bamboo--left {
  animation: heroBambooSway 18s ease-in-out infinite;
}

.hero-bamboo--right {
  animation: heroBambooSway 20s ease-in-out infinite reverse;
}

@media (max-width: 767px) {
  .hero-landscape {
    width: 68rem;
    left: 48%;
    opacity: 0.58;
  }

  .hero-lantern-glow {
    right: -2.25rem;
    top: 4rem;
    opacity: 0.74;
  }

  .hero-mist--front {
    opacity: 0.38;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-lantern-glow,
  .hero-mist,
  .hero-bamboo {
    animation: none;
  }
}
```

- [ ] **Step 3: 跑组件回归测试，确认结构改名后没有把搜索交互带坏**

Run:

```bash
npm --prefix frontend run test:run -- src/test/components/home/HeroSection.spec.ts src/test/App.spec.ts
```

Expected: PASS，`HeroSection` 与 `App` 两组测试全部通过。

- [ ] **Step 4: 启动前端，在浏览器里做真实交互验收**

Run:

```bash
npm --prefix frontend run dev -- --host 127.0.0.1 --port 4173
```

在浏览器打开：

```text
http://127.0.0.1:4173/
```

按下面顺序人工验收：
1. 首屏第一眼是“成熟产品首页”，不是“概念展示海报”
2. 左侧双时钟仍在，且明显退为信息塔，不再和标题抢中心
3. 标题尺寸收敛，没有巨型大字压迫感
4. 搜索框、按钮、热门标签形成一个完整主操作区，视觉重心最强
5. 三张价值卡位于搜索区下方，存在感低于搜索区，但内容清楚可读
6. 输入 `算法导论` 后点击 `进入结果页`，页面进入结果页，不出现首页停留
7. 返回首页后点击热门标签，仍能直接进入结果页
8. 缩窄到移动端宽度后，搜索框和按钮仍清晰，背景景深明显减弱但双时钟仍保留
9. 页面滚动时，远山/雾层/竹影有层次位移，但正文和搜索框稳定、不漂移

Expected: 以上 9 项全部满足；若第 6/7 项失败，先检查是否误改了 `emit('search')` 或 `App.vue` 的既有搜索路由行为。

- [ ] **Step 5: 运行最终构建并提交**

Run:

```bash
npm --prefix frontend run build
```

Expected: PASS，生成生产构建成功。

Commit:

```bash
git add frontend/src/components/home/HeroSection.vue frontend/src/styles/variables.css frontend/src/locales/modules/hero.ts frontend/src/test/components/home/HeroSection.spec.ts
git commit -m "feat: 完成首页 HeroSection 视觉重设计"
```

---

## 完成定义

满足以下条件才算这份计划执行完成：
- `HeroSection.spec.ts` 通过，覆盖新层级、按钮搜索、热门标签、自动补全选择
- `App.spec.ts` 通过，证明首页搜索仍然跳转到 `BookSearch`
- `npm --prefix frontend run build` 通过
- 浏览器人工验收通过，尤其是“雅致标题 + 强搜索主角”“双时钟保留”“移动端背景收敛”这三项
- 最终改动只落在 `HeroSection.vue`、`hero.ts`、`variables.css` 和新增测试文件，不外溢到结果页或全局路由逻辑
