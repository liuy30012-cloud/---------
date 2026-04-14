<template>
  <section v-reveal="{ preset: 'section', once: true }" class="related-volumes-section">
    <div class="related-header">
      <div class="related-title-group">
        <p class="related-kicker">{{ t('related.kicker') }}</p>
        <h3 class="related-title">{{ t('related.title') }}</h3>
        <p class="related-subtitle">{{ t('related.subtitle') }}</p>
      </div>
    </div>

    <div v-if="loading" class="related-loading">{{ t('related.loading') }}</div>

    <div v-else-if="books.length === 0" class="related-empty">{{ t('related.empty') }}</div>

    <div v-else class="related-scroll-container" ref="scrollContainerEl">
      <div v-for="(book, index) in books" :key="book.bookId" class="related-item-shell">
        <button
          v-reveal="{ preset: 'card', delay: index * 0.06, once: true }"
          type="button"
          class="related-item"
          :aria-label="`Open book ${book.title}`"
          @click="openBook(book.bookId)"
        >
        <div class="related-cover">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" class="related-img" />
          <div v-else class="related-placeholder">
            <span class="material-symbols-outlined">menu_book</span>
          </div>
        </div>
        <h4 class="related-item-title">{{ book.title }}</h4>
        <p class="related-item-author">{{ book.author }}</p>
        <span class="related-badge">{{ t('related.borrowCount', { count: book.borrowCount }) }}</span>
        </button>
      </div>
    </div>

    <div class="carousel-controls" v-if="books.length > 0">
      <button v-reveal="{ preset: 'card', delay: 0.04, once: true }" class="nav-arrow" @click="scrollCarousel('left')"><span class="material-symbols-outlined">chevron_left</span></button>
      <button v-reveal="{ preset: 'card', delay: 0.1, once: true }" class="nav-arrow" @click="scrollCarousel('right')"><span class="material-symbols-outlined">chevron_right</span></button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { statisticsApi, type PopularBook } from '../../api/statisticsApi'
import { logger } from '../../utils/logger'

const { t } = useI18n()
const router = useRouter()
const books = ref<PopularBook[]>([])
const loading = ref(false)
const scrollContainerEl = ref<HTMLElement | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    const response = await statisticsApi.getPopularBooks(10)
    books.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load popular books:', error)
    books.value = []
  } finally {
    loading.value = false
  }
})

function scrollCarousel(direction: 'left' | 'right') {
  scrollContainerEl.value?.scrollBy({
    left: direction === 'left' ? -320 : 320,
    behavior: 'smooth',
  })
}

function openBook(bookId: number) {
  router.push({ name: 'BookDetail', params: { id: bookId } }).catch(() => {})
}
</script>

<style scoped>
.related-loading,
.related-empty {
  padding: 2rem 0;
  color: var(--on-surface-variant);
}

.related-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(228, 230, 225, 0.85);
  color: rgba(18, 59, 93, 0.35);
}

.related-badge {
  display: inline-flex;
  width: fit-content;
  margin-top: 0.5rem;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: rgba(18, 59, 93, 0.06);
  color: var(--primary);
  font-size: 0.74rem;
  font-weight: 700;
}

.related-item-shell {
  width: 12rem;
  flex-shrink: 0;
}

.related-item {
  width: 100%;
  display: block;
  appearance: none;
  padding: 0;
  border: none;
  background: none;
  color: inherit;
  font: inherit;
  text-align: left;
}

.related-item:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 4px;
}
</style>
