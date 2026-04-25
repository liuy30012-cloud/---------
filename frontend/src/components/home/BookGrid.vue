<template>
  <section class="results-catalog">
    <div class="catalog-header">
      <div class="catalog-title-group">
        <h2 class="catalog-title">{{ $t('catalog.featured') }}</h2>
        <p class="catalog-subtitle">{{ $t('catalog.featuredSub') }}</p>
      </div>
      <div class="view-toggles">
        <LibraryButton type="ghost" size="small" :class="{ 'is-active': viewMode === 'grid' }" :aria-pressed="viewMode === 'grid'" :aria-label="$t('catalog.gridView')" @click="$emit('setViewMode', 'grid')"><span class="material-symbols-outlined" aria-hidden="true">grid_view</span></LibraryButton>
        <LibraryButton type="ghost" size="small" :class="{ 'is-active': viewMode === 'list' }" :aria-pressed="viewMode === 'list'" :aria-label="$t('catalog.listView')" @click="$emit('setViewMode', 'list')"><span class="material-symbols-outlined" aria-hidden="true">view_list</span></LibraryButton>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="state-message" role="status" aria-live="polite">
      <div class="spinner" aria-hidden="true"></div>
      <p>{{ $t('catalog.loading') }}</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="books.length === 0" class="state-message empty" role="status" aria-live="polite">
      <p>{{ $t('catalog.empty') }}</p>
    </div>

    <!-- Book Grid -->
    <div v-else :class="['book-grid', { 'list-view': viewMode === 'list' }]">
      <article
        v-for="book in books"
        :key="book.id + '-' + (book.status || 'db')"
        class="book-card"
        role="button"
        tabindex="0"
        @click="$emit('goToDetail', Number(book.id))"
        @keydown.enter="$emit('goToDetail', Number(book.id))"
      >
        <div class="book-cover-wrapper">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" class="book-cover-img" @error="onImgError" />
          <div v-else class="book-cover-fallback">
            <span class="material-symbols-outlined fallback-icon">menu_book</span>
            <span class="fallback-text">{{ $t('catalog.noCover') }}</span>
          </div>
          <div class="badge-wrapper">
            <span v-if="book.status" :class="['status-badge', book.status.toLowerCase()]">{{ $t(`catalog.badges.${book.status}`) }}</span>
          </div>
        </div>
        <div class="book-details">
          <div class="book-metadata">
            <p class="book-isbn">{{ $t('catalog.isbn') }} {{ book.isbn || 'UNKNOWN' }}</p>
            <h3 class="book-title-text">{{ book.title }}</h3>
            <p class="book-author-text">{{ book.author }} • {{ book.year || 'N/A' }} • {{ book.languageCode || 'N/A' }}</p>
          </div>
          <div class="book-description">
            {{ book.description || 'A comprehensive monograph covering fundamental knowledge. This edition includes localized annotations for modern archival systems.' }}
          </div>
          <div class="book-location">
            <p class="location-heading">{{ $t('catalog.location') }}</p>
            <div class="location-box">
              <span class="material-symbols-outlined location-icon">location_on</span>
              <span class="location-value">{{ book.location }}</span>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div v-if="isOffline && hasSearched && books.length > 0" class="offline-warning">
      <p class="offline-warning-text">{{ $t('catalog.offlineWarning') }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Book } from '../../types/book'
import { handleImageError } from '../../utils/imageHelpers'
import LibraryButton from '@/components/common/LibraryButton.vue'

defineProps<{
  books: Book[]
  loading: boolean
  viewMode: 'grid' | 'list'
  hasSearched: boolean
  isOffline: boolean
}>()

defineEmits<{
  setViewMode: [mode: 'grid' | 'list']
  goToDetail: [id: number]
}>()

const onImgError = (e: Event) => handleImageError(e, '/logo-photo.jpg')
</script>

<style scoped>
.is-active :deep(.el-button) {
  background: rgba(0, 83, 219, 0.1) !important;
  color: var(--primary) !important;
}
</style>
