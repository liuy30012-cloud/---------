<template>
  <section ref="heroRef" class="hero-section" aria-label="Library home search entry">
    <div class="hero-atmosphere" aria-hidden="true">
      <div class="hero-lantern-glow"></div>
      <div ref="mistBackRef" class="hero-mist hero-mist--back"></div>
      <div ref="mistMidRef" class="hero-mist hero-mist--mid"></div>
      <div ref="mistFrontRef" class="hero-mist hero-mist--front"></div>

      <svg ref="landscapeRef" class="hero-landscape" viewBox="0 0 1440 540" xmlns="http://www.w3.org/2000/svg">
        <path
          class="hero-landscape-layer hero-landscape-layer--far"
          d="M0 388C88 348 164 324 260 329C356 334 430 392 514 392C604 392 676 326 774 324C874 322 954 384 1040 384C1126 384 1218 320 1304 318C1362 316 1410 330 1440 342V540H0Z"
        />
        <path
          class="hero-landscape-layer hero-landscape-layer--mid"
          d="M0 430C114 372 208 352 300 360C394 368 458 432 542 432C624 432 712 356 814 358C918 360 998 430 1092 430C1186 430 1294 362 1440 356V540H0Z"
        />
        <path
          class="hero-landscape-layer hero-landscape-layer--low"
          d="M0 460C140 418 222 402 312 406C422 410 520 468 616 468C716 468 792 420 900 420C1010 420 1116 486 1224 486C1302 486 1374 462 1440 438V540H0Z"
        />
        <path class="hero-landscape-path" d="M840 418C910 420 960 440 1036 482" />
      </svg>

      <svg ref="leftBambooRef" class="hero-bamboo hero-bamboo--left" viewBox="0 0 260 620" xmlns="http://www.w3.org/2000/svg">
        <path class="hero-bamboo-stalk" d="M82 640C80 480 78 340 80 0" />
        <path class="hero-bamboo-stalk hero-bamboo-stalk--soft" d="M128 640C126 474 124 332 126 48" />
        <path class="hero-bamboo-stalk hero-bamboo-stalk--deep" d="M164 640C166 490 170 350 176 72" />
        <path class="hero-bamboo-leaf" d="M82 144C112 108 146 90 196 74C166 118 136 142 94 160Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--soft" d="M82 214C48 182 24 152 0 108C48 130 82 156 106 198Z" />
        <path class="hero-bamboo-leaf" d="M128 242C170 220 208 212 254 214C214 242 176 258 138 264Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--soft" d="M126 304C90 280 58 248 36 210C82 224 114 250 142 288Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--deep" d="M176 348C214 330 236 316 260 288C228 290 200 304 176 336Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--soft" d="M170 414C126 382 102 358 74 316C122 330 150 350 180 394Z" />
        <path class="hero-bamboo-leaf" d="M80 466C120 438 148 424 194 418C156 454 128 474 86 484Z" />
      </svg>

      <svg ref="rightBambooRef" class="hero-bamboo hero-bamboo--right" viewBox="0 0 260 620" xmlns="http://www.w3.org/2000/svg">
        <path class="hero-bamboo-stalk hero-bamboo-stalk--deep" d="M178 640C176 482 176 332 182 0" />
        <path class="hero-bamboo-stalk hero-bamboo-stalk--soft" d="M138 640C140 486 144 362 152 54" />
        <path class="hero-bamboo-stalk" d="M96 640C98 510 96 394 96 116" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--soft" d="M178 144C138 112 110 100 62 88C98 126 126 146 170 162Z" />
        <path class="hero-bamboo-leaf" d="M180 218C220 190 244 164 260 124C222 146 196 170 170 206Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--deep" d="M150 254C112 226 84 212 30 208C74 244 104 260 144 268Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--soft" d="M96 324C130 296 164 272 220 256C188 308 150 334 102 344Z" />
        <path class="hero-bamboo-leaf" d="M148 394C110 358 78 334 26 306C62 354 96 382 140 406Z" />
        <path class="hero-bamboo-leaf hero-bamboo-leaf--deep" d="M94 472C136 450 170 442 216 442C174 472 144 486 104 492Z" />
      </svg>
    </div>
    <div class="hero-shell">
      <aside class="hero-rail" aria-label="Home timing and library access information">
        <div class="hero-rail-card hero-rail-card--clock">
          <div class="hero-rail-heading">
            <span class="hero-rail-label">Shi Chen</span>
            <span class="hero-rail-caption">{{ t('hero.clockLabel') }}</span>
          </div>
          <div class="dizhi-clock-container">
            <DizhiClock :subtle="true" />
          </div>
        </div>

        <div class="hero-rail-card hero-rail-card--compact">
          <div class="hero-rail-heading">
            <span class="hero-rail-label">Library Time</span>
            <span class="hero-rail-caption">{{ t('hero.libraryTimeLabel') }}</span>
          </div>
          <div class="modern-clock-container">
            <ModernClock />
          </div>
        </div>
      </aside>

      <div class="hero-content">
        <div class="hero-stage">
          <div ref="copyRef" class="hero-intro">
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
        </div>

        <div class="hero-value-grid">
          <div class="hero-value-card">
            <span class="hero-value-card__title">{{ t('hero.valueCard1Title') }}</span>
            <span class="hero-value-card__desc">{{ t('hero.valueCard1Desc') }}</span>
          </div>
          <div class="hero-value-card">
            <span class="hero-value-card__title">{{ t('hero.valueCard2Title') }}</span>
            <span class="hero-value-card__desc">{{ t('hero.valueCard2Desc') }}</span>
          </div>
          <div class="hero-value-card">
            <span class="hero-value-card__title">{{ t('hero.valueCard3Title') }}</span>
            <span class="hero-value-card__desc">{{ t('hero.valueCard3Desc') }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { bookApi } from '../../api/bookApi'
import { useSearchAutocomplete } from '../../composables/useSearchAutocomplete'
import DizhiClock from '../../dizhi/DizhiClock.vue'
import ModernClock from '../../dizhi/ModernClock.vue'
import { gsap, prefersReducedMotion } from '../../motion'

const emit = defineEmits<{
  (e: 'search', keyword: string): void
}>()

const { t, locale } = useI18n()
const searchQuery = ref('')
const searchComboboxRef = ref<HTMLElement | null>(null)
const heroRef = ref<HTMLElement | null>(null)
const copyRef = ref<HTMLElement | null>(null)
const searchPanelRef = ref<HTMLElement | null>(null)
const landscapeRef = ref<SVGElement | null>(null)
const leftBambooRef = ref<SVGElement | null>(null)
const rightBambooRef = ref<SVGElement | null>(null)
const mistBackRef = ref<HTMLElement | null>(null)
const mistMidRef = ref<HTMLElement | null>(null)
const mistFrontRef = ref<HTMLElement | null>(null)
let cleanupMotion: (() => void) | undefined
const {
  listId: searchAutocompleteListId,
  suggestions: searchAutocompleteSuggestions,
  loading: searchAutocompleteLoading,
  isOpen: searchAutocompleteOpen,
  activeIndex: searchAutocompleteActiveIndex,
  getOptionId: getSearchAutocompleteOptionId,
  schedule: scheduleSearchAutocomplete,
  openIfAvailable: openSearchAutocompleteIfAvailable,
  close: closeSearchAutocomplete,
  clear: clearSearchAutocomplete,
  selectSuggestion: selectSearchAutocomplete,
  handleKeydown: handleSearchAutocompleteKeydown,
} = useSearchAutocomplete(async (query, limit) => {
  const response = await bookApi.getSearchSuggestions(query, limit)
  return response.data.success ? response.data.data : []
})

const trendingTags = computed(() => [
  { label: t('hero.tag1Label'), query: t('hero.tag1Query') },
  { label: t('hero.tag2Label'), query: t('hero.tag2Query') },
  { label: t('hero.tag3Label'), query: t('hero.tag3Query') },
  { label: t('hero.tag4Label'), query: t('hero.tag4Query') },
])

const searchLoadingLabel = computed(() =>
  locale.value.startsWith('zh') ? '正在匹配相关书名与作者…' : 'Finding related titles and authors…',
)

function submitSearch() {
  closeSearchAutocomplete()
  emit('search', searchQuery.value.trim())
}

function handleSearchInput() {
  scheduleSearchAutocomplete(searchQuery.value)
}

function applySuggestion(suggestion: string) {
  searchQuery.value = selectSearchAutocomplete(suggestion)
  submitSearch()
}

function handleSearchKeydown(event: KeyboardEvent) {
  if (event.isComposing || event.keyCode === 229) {
    return
  }

  const selectedSuggestion = handleSearchAutocompleteKeydown(event)
  if (selectedSuggestion) {
    applySuggestion(selectedSuggestion)
    return
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    submitSearch()
  }
}

function applyTag(query: string) {
  searchQuery.value = query
  clearSearchAutocomplete()
  submitSearch()
}

function handleSearchBlur() {
  window.setTimeout(() => {
    if (!searchComboboxRef.value?.contains(document.activeElement)) {
      closeSearchAutocomplete()
    }
  }, 0)
}

function handleDocumentPointerDown(event: MouseEvent) {
  if (!searchComboboxRef.value) return
  if (!searchComboboxRef.value.contains(event.target as Node)) {
    closeSearchAutocomplete()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentPointerDown)

  if (prefersReducedMotion() || !heroRef.value) {
    return
  }

  requestAnimationFrame(() => {
    if (!heroRef.value) return

    const context = gsap.context(() => {
      const railCards = gsap.utils.toArray<HTMLElement>('.hero-rail-card').filter(el => el instanceof HTMLElement && el.isConnected)
      const metaCards = gsap.utils.toArray<HTMLElement>('.hero-meta-card').filter(el => el instanceof HTMLElement && el.isConnected)
      const titleTargets = [copyRef.value, ...metaCards, searchPanelRef.value].filter(Boolean)

      if (titleTargets.length > 0) {
        gsap.set(titleTargets, { willChange: 'transform, opacity, filter' })
      }

      const enterTimeline = gsap.timeline({
        defaults: {
          ease: 'power3.out',
        },
      })

      enterTimeline
        .fromTo(
          railCards,
          {
            autoAlpha: 0,
            x: -24,
            y: 18,
            filter: 'blur(14px)',
          },
          {
            autoAlpha: 1,
            x: 0,
            y: 0,
            filter: 'blur(0px)',
            duration: 0.7,
            stagger: 0.08,
            clearProps: 'filter',
          }
        )
        .fromTo(
          copyRef.value,
          {
            autoAlpha: 0,
            y: 34,
            filter: 'blur(16px)',
          },
          {
            autoAlpha: 1,
            y: 0,
            filter: 'blur(0px)',
            duration: 0.8,
            clearProps: 'filter',
          },
          0.12
        )
        .fromTo(
          metaCards,
          {
            autoAlpha: 0,
            y: 22,
            filter: 'blur(12px)',
          },
          {
            autoAlpha: 1,
            y: 0,
            filter: 'blur(0px)',
            duration: 0.56,
            stagger: 0.08,
            clearProps: 'filter',
          },
          0.3
        )
        .fromTo(
          searchPanelRef.value,
          {
            autoAlpha: 0,
            y: 24,
            filter: 'blur(14px)',
          },
          {
            autoAlpha: 1,
            y: 0,
            filter: 'blur(0px)',
            duration: 0.62,
            clearProps: 'filter',
          },
          0.42
        )

      if (landscapeRef.value) {
        gsap.to(landscapeRef.value, {
          yPercent: -6,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.65,
          },
        })
      }

      if (leftBambooRef.value) {
        gsap.to(leftBambooRef.value, {
          yPercent: -8,
          xPercent: -2,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.5,
          },
        })
      }

      if (rightBambooRef.value) {
        gsap.to(rightBambooRef.value, {
          yPercent: -6,
          xPercent: 2,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.48,
          },
        })
      }

      if (mistBackRef.value) {
        gsap.to(mistBackRef.value, {
          yPercent: -10,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.7,
          },
        })
      }

      if (mistMidRef.value) {
        gsap.to(mistMidRef.value, {
          yPercent: -6,
          xPercent: 2,
          ease: 'none',
          scrollTrigger: {
            trigger: heroRef.value,
            start: 'top top',
            end: 'bottom top',
            scrub: 0.6,
          },
        })
      }

      if (mistFrontRef.value) {
        gsap.to(mistFrontRef.value, {
          yPercent: -14,
          xPercent: -2,
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

onUnmounted(() => {
  cleanupMotion?.()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentPointerDown)
})
</script>

<style scoped>
.hero-section {
  position: relative;
  padding-bottom: var(--space-6);
  isolation: isolate;
}

.hero-section::before,
.hero-section::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-section::before {
  top: 2.5rem;
  left: 8%;
  right: 8%;
  bottom: auto;
  height: 31rem;
  border-radius: 3rem;
  background:
    radial-gradient(circle at 18% 22%, rgba(255, 246, 225, 0.82) 0%, rgba(255, 246, 225, 0.2) 28%, transparent 62%),
    linear-gradient(180deg, rgba(248, 245, 238, 0.88) 0%, rgba(226, 235, 224, 0.5) 100%);
  filter: blur(28px);
  opacity: 0.92;
}

.hero-section::after {
  background:
    radial-gradient(circle at 15% 10%, rgba(255, 244, 220, 0.28) 0%, transparent 18%),
    radial-gradient(circle at 78% 16%, rgba(153, 177, 147, 0.18) 0%, transparent 18%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, transparent 30%);
  opacity: 0.88;
}

.hero-atmosphere {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.hero-lantern-glow {
  position: absolute;
  top: 2.2rem;
  right: 12%;
  width: clamp(10rem, 18vw, 17rem);
  height: clamp(10rem, 18vw, 17rem);
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(251, 241, 212, 0.72) 0%, rgba(220, 198, 147, 0.22) 34%, transparent 74%);
  filter: blur(10px);
  animation: heroGlowPulse 8s ease-in-out infinite alternate;
}

.hero-mist {
  position: absolute;
  border-radius: 999px;
  background:
    radial-gradient(circle, rgba(248, 246, 240, 0.86) 0%, rgba(229, 237, 228, 0.5) 46%, transparent 80%);
  filter: blur(24px);
  opacity: 0.96;
}

.hero-mist--back {
  top: 9rem;
  left: 10%;
  width: min(40rem, 48vw);
  height: 8rem;
  animation: heroMistDrift 22s ease-in-out infinite;
}

.hero-mist--mid {
  top: 15rem;
  right: 12%;
  width: min(28rem, 32vw);
  height: 7rem;
  animation: heroMistDrift 26s ease-in-out infinite reverse;
}

.hero-mist--front {
  bottom: 3rem;
  left: 30%;
  width: min(34rem, 40vw);
  height: 7.5rem;
  opacity: 0.58;
  animation: heroMistDrift 28s ease-in-out infinite;
}

.hero-landscape {
  position: absolute;
  left: 50%;
  bottom: -0.5rem;
  width: min(100%, 90rem);
  height: auto;
  transform: translateX(-50%);
  opacity: 0.74;
}

.hero-landscape-layer {
  fill: rgba(170, 184, 164, 0.26);
}

.hero-landscape-layer--far {
  fill: rgba(126, 149, 123, 0.18);
}

.hero-landscape-layer--mid {
  fill: rgba(139, 160, 134, 0.24);
}

.hero-landscape-layer--low {
  fill: rgba(190, 194, 168, 0.34);
}

.hero-landscape-path {
  fill: none;
  stroke: rgba(143, 124, 96, 0.34);
  stroke-width: 6;
  stroke-linecap: round;
}

.hero-bamboo {
  position: absolute;
  bottom: -1.75rem;
  width: clamp(11rem, 18vw, 15rem);
  height: auto;
  opacity: 0.6;
}

.hero-bamboo--left {
  left: -1.5rem;
  transform-origin: left bottom;
  animation: heroBambooSway 16s ease-in-out infinite;
}

.hero-bamboo--right {
  right: -1.5rem;
  transform-origin: right bottom;
  animation: heroBambooSway 18s ease-in-out infinite reverse;
}

.hero-bamboo-stalk,
.hero-bamboo-leaf {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-bamboo-stalk {
  stroke: rgba(72, 98, 77, 0.34);
  stroke-width: 9;
}

.hero-bamboo-stalk--soft {
  stroke: rgba(111, 137, 115, 0.22);
  stroke-width: 7;
}

.hero-bamboo-stalk--deep {
  stroke: rgba(58, 80, 63, 0.42);
  stroke-width: 8;
}

.hero-bamboo-leaf {
  fill: rgba(95, 122, 96, 0.2);
  stroke: rgba(75, 103, 78, 0.12);
  stroke-width: 1.5;
}

.hero-bamboo-leaf--soft {
  fill: rgba(134, 155, 132, 0.16);
}

.hero-bamboo-leaf--deep {
  fill: rgba(72, 92, 70, 0.22);
}

.hero-shell {
  position: relative;
  z-index: 1;
  width: min(100% - 2 * var(--page-gutter), var(--page-width-home));
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(19rem, 23rem) minmax(0, 1fr);
  gap: clamp(1.4rem, 2vw, 2.4rem);
  align-items: stretch;
}

.hero-rail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.hero-rail-card,
.hero-content,
.hero-meta-card,
.hero-search-panel {
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.hero-rail-card {
  position: relative;
  padding: clamp(1.15rem, 2vw, 1.5rem);
  border-radius: calc(var(--radius-xl) + 0.35rem);
  background:
    linear-gradient(180deg, rgba(251, 249, 244, 0.9) 0%, rgba(237, 233, 223, 0.82) 100%);
  border: 1px solid rgba(103, 110, 93, 0.16);
  box-shadow: var(--home-shadow-lift);
  overflow: hidden;
}

.hero-rail-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.66) 0%, transparent 42%),
    radial-gradient(circle at top right, rgba(202, 163, 111, 0.16) 0%, transparent 28%),
    radial-gradient(circle at bottom left, rgba(129, 156, 127, 0.12) 0%, transparent 22%);
  pointer-events: none;
}

.hero-rail-card::after {
  content: '';
  position: absolute;
  inset: 0.9rem;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.42);
  opacity: 0.54;
  pointer-events: none;
}

.hero-rail-card--clock {
  min-height: 27rem;
}

.hero-rail-card--compact {
  min-height: 14.5rem;
}

.hero-rail-heading {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: var(--space-3);
}

.hero-rail-label {
  font-size: 0.74rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(110, 88, 61, 0.7);
}

.hero-rail-caption {
  font-size: 0.88rem;
  color: rgba(69, 80, 66, 0.76);
}

.dizhi-clock-container,
.modern-clock-container {
  position: relative;
  z-index: 1;
  width: 100%;
}

.dizhi-clock-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 22rem;
  filter: saturate(0.82) hue-rotate(-6deg);
}

.modern-clock-container {
  display: flex;
  justify-content: center;
  filter: saturate(0.88) hue-rotate(-8deg);
}

.hero-content {
  position: relative;
  overflow: hidden;
  padding: clamp(2rem, 4vw, 4rem);
  border-radius: 2.45rem;
  background:
    linear-gradient(180deg, rgba(250, 248, 242, 0.92) 0%, rgba(236, 242, 233, 0.84) 100%);
  border: 1px solid rgba(103, 110, 93, 0.18);
  box-shadow: var(--home-shadow-deep);
}

.hero-content::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.64) 0%, transparent 42%),
    radial-gradient(circle at top right, rgba(202, 163, 111, 0.18) 0%, transparent 26%),
    radial-gradient(circle at bottom left, rgba(136, 168, 136, 0.14) 0%, transparent 26%);
  pointer-events: none;
}

.hero-content::after {
  content: '';
  position: absolute;
  inset: 1rem;
  border-radius: calc(2.45rem - 1rem);
  border: 1px solid rgba(255, 255, 255, 0.42);
  opacity: 0.68;
  pointer-events: none;
}

.hero-copy,
.hero-metadata,
.hero-search-panel {
  position: relative;
  z-index: 1;
}

.hero-stage {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.hero-intro {
  max-width: 47rem;
}

.hero-intro::after {
  content: '';
  display: block;
  width: clamp(5rem, 10vw, 8rem);
  height: 2px;
  margin-top: var(--space-5);
  border-radius: 999px;
  background:
    linear-gradient(90deg, rgba(103, 128, 101, 0.62) 0%, rgba(193, 167, 122, 0.34) 54%, transparent 100%);
  box-shadow: 0 10px 20px rgba(139, 123, 95, 0.12);
}

.hero-value-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.hero-value-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(252, 249, 243, 0.76) 0%, rgba(242, 237, 228, 0.68) 100%);
  border: 1px solid rgba(105, 116, 98, 0.14);
  box-shadow: 0 14px 26px rgba(43, 52, 44, 0.08);
}

.hero-value-card__title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--home-ink);
}

.hero-value-card__desc {
  font-size: 0.84rem;
  color: rgba(83, 94, 82, 0.72);
}

.hero-copy {
  max-width: 47rem;
}

.hero-copy::after {
  content: '';
  display: block;
  width: clamp(5rem, 10vw, 8rem);
  height: 2px;
  margin-top: var(--space-5);
  border-radius: 999px;
  background:
    linear-gradient(90deg, rgba(103, 128, 101, 0.62) 0%, rgba(193, 167, 122, 0.34) 54%, transparent 100%);
  box-shadow: 0 10px 20px rgba(139, 123, 95, 0.12);
}

.hero-kicker {
  margin: 0 0 var(--space-4);
  font-size: 0.76rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: rgba(118, 92, 58, 0.76);
}

.hero-title {
  margin: 0;
  max-width: 12ch;
  font-family: var(--font-headline);
  font-size: clamp(3rem, 5vw, 4.8rem);
  line-height: 1.02;
  letter-spacing: -0.045em;
  color: var(--home-ink);
  text-wrap: balance;
}

.hero-subtitle {
  margin: var(--space-4) 0 0;
  max-width: 48rem;
  font-size: 1.02rem;
  line-height: 1.9;
  color: rgba(54, 67, 55, 0.8);
}

.hero-metadata {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.hero-meta-card {
  position: relative;
  overflow: hidden;
  padding: var(--space-4);
  border-radius: calc(var(--radius-lg) + 0.1rem);
  background:
    linear-gradient(180deg, rgba(252, 249, 243, 0.76) 0%, rgba(242, 237, 228, 0.68) 100%);
  border: 1px solid rgba(105, 116, 98, 0.14);
  box-shadow: 0 14px 26px rgba(43, 52, 44, 0.08);
}

.hero-meta-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.46) 0%, transparent 48%),
    radial-gradient(circle at bottom right, rgba(202, 163, 111, 0.1) 0%, transparent 24%);
  pointer-events: none;
}

.hero-meta-key {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.72rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(108, 84, 58, 0.68);
}

.hero-meta-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--home-ink);
}

.hero-search-panel {
  position: relative;
  overflow: hidden;
  margin-top: var(--space-7);
  padding: clamp(1.25rem, 3vw, 1.6rem);
  border-radius: calc(var(--radius-xl) + 0.2rem);
  background:
    linear-gradient(180deg, rgba(250, 247, 241, 0.82) 0%, rgba(239, 234, 224, 0.74) 100%);
  border: 1px solid rgba(103, 110, 93, 0.16);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.46),
    0 16px 34px rgba(49, 59, 50, 0.08);
}

.hero-search-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.48) 0%, transparent 46%),
    radial-gradient(circle at top right, rgba(202, 163, 111, 0.12) 0%, transparent 24%),
    radial-gradient(circle at bottom left, rgba(140, 163, 136, 0.12) 0%, transparent 24%);
  pointer-events: none;
}

.hero-search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.hero-search-title {
  font-family: var(--font-headline);
  font-size: 1rem;
  font-weight: 700;
  color: var(--home-ink);
}

.hero-search-note {
  font-size: 0.84rem;
  color: rgba(83, 94, 82, 0.7);
}

.search-combobox {
  position: relative;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 4.4rem;
  padding: 0.55rem 0.6rem 0.55rem 1.15rem;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.94);
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
    0 20px 36px rgba(61, 70, 57, 0.14);
}

.search-icon {
  color: rgba(109, 89, 58, 0.72);
  font-size: 1.35rem;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 1rem;
  line-height: 1.5;
  color: var(--home-ink);
}

.search-input::placeholder {
  color: rgba(89, 99, 87, 0.56);
}

.search-btn {
  min-width: 8rem;
  min-height: 3.2rem;
  padding: 0 1.35rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #d9bf8f 0%, #bb975f 48%, #efe0bf 100%);
  color: #1c1a16;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 16px 30px rgba(121, 93, 57, 0.14);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(121, 93, 57, 0.18);
  filter: saturate(1.04);
}

.hero-suggestion-popover {
  position: absolute;
  top: calc(100% + 0.55rem);
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.5rem;
  border-radius: 1.35rem;
  background: rgba(255, 252, 247, 0.97);
  border: 1px solid rgba(109, 117, 99, 0.14);
  box-shadow: 0 22px 40px rgba(49, 59, 50, 0.14);
  z-index: 4;
}

.hero-suggestion-loading,
.hero-suggestion-item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  width: 100%;
  min-height: 2.85rem;
  padding: 0.75rem 0.9rem;
  border: none;
  border-radius: 1rem;
  background: transparent;
  color: var(--home-ink);
  text-align: left;
}

.hero-suggestion-loading {
  color: rgba(83, 94, 82, 0.72);
}

.hero-suggestion-item {
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.hero-suggestion-item:hover,
.hero-suggestion-item--active {
  background: rgba(221, 205, 171, 0.28);
  transform: translateY(-1px);
}

.hero-suggestion-item .material-symbols-outlined,
.hero-suggestion-loading .material-symbols-outlined {
  color: rgba(109, 89, 58, 0.64);
  font-size: 1.1rem;
}

.trending-tags {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: var(--space-4);
}

.trending-label {
  margin-right: var(--space-1);
  font-size: 0.82rem;
  font-weight: 700;
  color: rgba(94, 84, 70, 0.74);
}

.tag-btn {
  padding: 0.55rem 0.92rem;
  border-radius: 999px;
  border: 1px solid rgba(109, 117, 99, 0.14);
  background: rgba(255, 252, 247, 0.8);
  color: var(--home-bamboo-deep);
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.tag-btn:hover {
  transform: translateY(-1px);
  background: rgba(248, 243, 232, 0.98);
  border-color: rgba(106, 138, 104, 0.24);
  box-shadow: 0 12px 22px rgba(65, 81, 70, 0.08);
}

.search-btn:focus-visible,
.tag-btn:focus-visible,
.search-input:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 3px;
}

@keyframes heroMistDrift {
  0% {
    transform: translate3d(-2%, 0, 0) scale(0.98);
  }

  50% {
    transform: translate3d(2%, -2%, 0) scale(1.02);
  }

  100% {
    transform: translate3d(-1%, 1%, 0) scale(0.99);
  }
}

@keyframes heroBambooSway {
  0%,
  100% {
    transform: rotate(-1.8deg);
  }

  50% {
    transform: rotate(1.6deg);
  }
}

@keyframes heroGlowPulse {
  0% {
    transform: scale(0.94);
    opacity: 0.72;
  }

  100% {
    transform: scale(1.04);
    opacity: 1;
  }
}

@media (max-width: 1180px) {
  .hero-metadata {
    grid-template-columns: 1fr;
  }

  .hero-value-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .hero-section::before {
    left: 4%;
    right: 4%;
    height: 34rem;
  }

  .hero-shell {
    grid-template-columns: 1fr;
  }

  .hero-rail {
    grid-template-columns: 1fr 1fr;
    display: grid;
  }

  .hero-bamboo {
    opacity: 0.46;
  }
}

@media (max-width: 767px) {
  .hero-section::before {
    top: 1.5rem;
    height: 30rem;
    filter: blur(18px);
  }

  .hero-landscape {
    width: 72rem;
    left: 48%;
  }

  .hero-lantern-glow {
    right: -2rem;
    top: 4rem;
  }

  .hero-bamboo {
    width: 10rem;
    opacity: 0.34;
  }

  .hero-content {
    padding: var(--space-6);
    border-radius: var(--radius-xl);
  }

  .hero-title {
    max-width: none;
    font-size: clamp(2.4rem, 10vw, 3.35rem);
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

  .hero-suggestion-popover {
    position: static;
    margin-top: 0.7rem;
  }

  .search-btn {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-lantern-glow,
  .hero-mist,
  .hero-bamboo {
    animation: none;
  }
}
</style>
