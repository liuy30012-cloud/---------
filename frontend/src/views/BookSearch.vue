<template>
  <div class="book-search page-stack">
    <PageHeader
      :title="t('bookSearch.title')"
      eyebrow="ARCHIVE SEARCH"
      :description="t('bookSearch.description')"
    >
      <template #actions>
        <div class="page-actions">
          <button class="page-action-btn page-action-btn--secondary" type="button" @click="resetFilters">
            {{ t('bookSearch.resetFilters') }}
          </button>
          <button class="page-action-btn page-action-btn--primary" type="button" @click="saveCurrentSearch" :disabled="loading">
            {{ t('bookSearch.saveSearch') }}
          </button>
        </div>
      </template>
    </PageHeader>

    <div class="search-layout">
      <aside v-reveal="{ preset: 'sidebar', once: true }" class="search-sidebar surface-card">
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.keyword') }}</label>
          <input v-model="form.keyword" type="text" :placeholder="t('bookSearch.sidebar.keywordPlaceholder')" @keyup.enter="submitSearch" />
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.author') }}</label>
          <input v-model="form.author" type="text" :placeholder="t('bookSearch.sidebar.authorPlaceholder')" @keyup.enter="submitSearch" />
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.year') }}</label>
          <input v-model="form.year" type="text" :placeholder="t('bookSearch.sidebar.yearPlaceholder')" @keyup.enter="submitSearch" />
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.category') }}</label>
          <select v-model="form.category">
            <option value="">{{ t('bookSearch.sidebar.allCategories') }}</option>
            <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
          </select>
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.language') }}</label>
          <select v-model="form.language">
            <option value="">{{ t('bookSearch.sidebar.allLanguages') }}</option>
            <option v-for="language in languages" :key="language" :value="language">{{ language }}</option>
          </select>
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.status') }}</label>
          <select v-model="form.status">
            <option value="">{{ t('bookSearch.sidebar.allStatus') }}</option>
            <option value="AVAILABLE">{{ t('bookSearch.sidebar.availableNow') }}</option>
            <option value="CHECKED_OUT">{{ t('bookSearch.sidebar.checkedOut') }}</option>
          </select>
        </div>
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.sort') }}</label>
          <select v-model="form.sort">
            <option value="relevance">{{ t('bookSearch.sortOptions.relevance') }}</option>
            <option value="popular">{{ t('bookSearch.sortOptions.popular') }}</option>
            <option value="availability">{{ t('bookSearch.sortOptions.availability') }}</option>
            <option value="year_desc">{{ t('bookSearch.sortOptions.yearDesc') }}</option>
            <option value="title_asc">{{ t('bookSearch.sortOptions.titleAsc') }}</option>
          </select>
        </div>
        <button class="sidebar-submit" type="button" @click="submitSearch">{{ t('bookSearch.sidebar.updateResults') }}</button>
      </aside>

      <section class="search-results">
        <div v-if="errorMessage" v-reveal="{ preset: 'section', once: true }" class="error-banner">
          <span class="material-symbols-outlined">warning</span>
          <span>{{ errorMessage }}</span>
        </div>

        <div v-reveal="{ preset: 'section', delay: 0.06, once: true }" class="results-toolbar surface-card">
          <div>
            <p class="toolbar-label">{{ t('bookSearch.toolbar.currentQuery') }}</p>
            <h2 class="toolbar-title">{{ querySummary }}</h2>
            <p class="toolbar-meta">
              {{ t('bookSearch.toolbar.results', { total: totalResults, page: pagination.page + 1, totalPages: Math.max(pagination.totalPages, 1) }) }}
            </p>
          </div>
          <div class="toolbar-actions">
            <label>
              {{ t('bookSearch.toolbar.perPage') }}
              <select v-model.number="form.size" @change="submitSearch">
                <option :value="12">12</option>
                <option :value="24">24</option>
                <option :value="36">36</option>
              </select>
            </label>
          </div>
        </div>

        <div v-if="loading" :key="`loading-${resultMotionKey}`" v-reveal="{ preset: 'section', once: true }" class="state-card surface-card">
          <div class="spinner"></div>
          <p>{{ t('bookSearch.loading') }}</p>
        </div>

        <div v-else-if="books.length === 0" :key="`empty-${resultMotionKey}`" v-reveal="{ preset: 'section', once: true }" class="state-card surface-card">
          <p class="state-title">{{ t('bookSearch.emptyTitle') }}</p>
          <p class="state-copy">{{ t('bookSearch.emptyCopy') }}</p>
        </div>

        <div v-else :key="`results-${resultMotionKey}`" class="result-grid">
          <button
            v-for="(book, index) in books"
            :key="`${book.id}-${resultMotionKey}`"
            v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
            class="result-card"
            type="button"
            :aria-label="t('bookSearch.favorite.viewDetail', { title: book.title })"
            @click="goToBookDetail(book.id)"
          >
            <div class="result-cover">
              <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" />
              <div v-else class="result-cover-placeholder">
                <span class="material-symbols-outlined">menu_book</span>
              </div>
              <span class="result-badge" :class="book.availableCopies > 0 ? 'result-badge--available' : 'result-badge--busy'">
                {{ book.availableCopies > 0 ? t('bookSearch.badge.available') : t('bookSearch.badge.reserve') }}
              </span>
              <button
                v-if="userStore.isLoggedIn"
                class="fav-heart"
                :class="{ 'fav-heart--active': favoritedBookIds.has(book.id) }"
                type="button"
                :aria-label="favoritedBookIds.has(book.id) ? t('bookSearch.favorite.remove') : t('bookSearch.favorite.add')"
                @click.stop="toggleFavoriteFromSearch(book.id)"
              >
                <span class="material-symbols-outlined">{{ favoritedBookIds.has(book.id) ? 'favorite' : 'favorite_border' }}</span>
              </button>
            </div>
            <div class="result-copy">
              <h3>{{ book.title }}</h3>
              <p class="result-author">{{ book.author }}</p>
              <p class="result-meta">{{ book.category }} · {{ book.languageCode }} · {{ book.year || t('bookSearch.yearUnknown') }}</p>
              <div class="location-pill">
                <span class="material-symbols-outlined">location_on</span>
                <span>{{ book.location }}</span>
              </div>
              <div class="result-footer">
                <span>{{ t('bookSearch.borrowable', { available: book.availableCopies, total: book.totalCopies }) }}</span>
                <span>{{ circulationLabel(book.circulationPolicy) }}</span>
              </div>
            </div>
          </button>
        </div>

        <div v-if="pagination.totalPages > 1" v-reveal="{ preset: 'section', delay: 0.08, once: true }" class="pagination surface-card">
          <button class="pagination-btn" type="button" :disabled="pagination.page <= 0" @click="goToPage(pagination.page - 1)">
            {{ t('bookSearch.pagination.prev') }}
          </button>
          <button
            v-for="pageNumber in visiblePages"
            :key="pageNumber"
            class="pagination-number"
            :class="{ active: pageNumber === pagination.page }"
            type="button"
            @click="goToPage(pageNumber)"
          >
            {{ pageNumber + 1 }}
          </button>
          <button
            class="pagination-btn"
            type="button"
            :disabled="pagination.page >= pagination.totalPages - 1"
            @click="goToPage(pagination.page + 1)"
          >
            {{ t('bookSearch.pagination.next') }}
          </button>
        </div>
      </section>
    </div>

    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { bookApi, type Book, type BookSearchParams } from '../api/bookApi'
import { favoriteApi } from '../api/favoriteApi'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'
import { useUserStore } from '../stores/user'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const history = inject<any>('searchHistoryController', null)

const form = reactive<BookSearchParams>({
  keyword: '',
  author: '',
  year: '',
  category: '',
  status: '',
  language: '',
  sort: 'relevance',
  page: 0,
  size: 12,
})

const books = ref<Book[]>([])
const categories = ref<string[]>([])
const languages = ref<string[]>([])
const loading = ref(false)
const totalResults = ref(0)
const errorMessage = ref('')
const pagination = reactive({
  page: 0,
  totalPages: 0,
})
const toast = reactive<{ message: string; type: 'success' | 'error' | 'info' }>({
  message: '',
  type: 'info',
})

const favoritedBookIds = ref<Set<number>>(new Set())

const querySummary = computed(() => {
  const parts = [form.keyword, form.author, form.category, form.language].filter(Boolean)
  return parts.length > 0 ? parts.join(' · ') : t('bookSearch.toolbar.allCollection')
})

const resultMotionKey = computed(() => route.fullPath)

const visiblePages = computed(() => {
  const start = Math.max(pagination.page - 2, 0)
  const end = Math.min(start + 5, pagination.totalPages)
  return Array.from({ length: end - start }, (_, index) => start + index)
})

onMounted(async () => {
  await Promise.all([loadCategories(), loadLanguages()])
  syncFromRoute()
})

watch(() => route.query, () => {
  syncFromRoute()
})

async function loadCategories() {
  try {
    const response = await bookApi.getCategories()
    categories.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load categories:', error)
    categories.value = []
  }
}

async function loadLanguages() {
  try {
    const response = await bookApi.getLanguages()
    languages.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load languages:', error)
    languages.value = []
  }
}

function syncFromRoute() {
  form.keyword = String(route.query.keyword || '')
  form.author = String(route.query.author || '')
  form.year = String(route.query.year || '')
  form.category = String(route.query.category || '')
  form.status = String(route.query.status || '')
  form.language = String(route.query.language || '')
  form.sort = String(route.query.sort || 'relevance')
  form.page = Number(route.query.page || 0)
  form.size = Number(route.query.size || 12)
  void fetchResults()
}

function buildQuery(nextPage = 0) {
  const query: Record<string, string> = {
    sort: String(form.sort || 'relevance'),
    page: String(nextPage),
    size: String(form.size || 12),
  }

  const optionalKeys: Array<keyof BookSearchParams> = ['keyword', 'author', 'year', 'category', 'status', 'language']
  optionalKeys.forEach((key) => {
    const value = form[key]
    if (value) {
      query[key] = String(value)
    }
  })

  return query
}

async function submitSearch() {
  router.push({ name: 'BookSearch', query: buildQuery(0) }).catch(() => {})
}

async function goToPage(page: number) {
  router.push({ name: 'BookSearch', query: buildQuery(page) }).catch(() => {})
}

function resetFilters() {
  form.keyword = ''
  form.author = ''
  form.year = ''
  form.category = ''
  form.status = ''
  form.language = ''
  form.sort = 'relevance'
  form.size = 12
  submitSearch()
}

async function fetchResults() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      keyword: form.keyword,
      author: form.author,
      year: form.year,
      category: form.category,
      status: form.status,
      language: form.language,
      sort: form.sort,
      page: form.page,
      size: form.size,
    }
    const response = await bookApi.searchBooks(params)
    books.value = response.data.data || []
    totalResults.value = response.data.total || books.value.length
    pagination.page = response.data.page || 0
    pagination.totalPages = response.data.totalPages || 0

    // 批量查询收藏状态
    if (userStore.isLoggedIn && books.value.length > 0) {
      try {
        const bookIds = books.value.map(b => b.id)
        const favRes = await favoriteApi.batchCheckFavorites(bookIds)
        if (favRes.data.success) {
          favoritedBookIds.value = new Set(favRes.data.data)
        }
      } catch {
        favoritedBookIds.value = new Set()
      }
    } else {
      favoritedBookIds.value = new Set()
    }

    if (history && (form.keyword || form.author || form.year || form.category || form.status || form.language)) {
      const canonicalQuery = buildQuery(pagination.page)
      const targetPath = `/books/search?${new URLSearchParams(canonicalQuery).toString()}`
      await history.addToHistory({
        keyword: querySummary.value,
        resultCount: totalResults.value,
        targetPath,
        queryPayload: JSON.stringify(canonicalQuery),
      })
    }
  } catch (error: any) {
    logger.error('Failed to search books:', error)
    books.value = []
    totalResults.value = 0
    pagination.totalPages = 0
    errorMessage.value = error.response?.data?.message || t('bookSearch.toast.serviceUnavailable')
  } finally {
    loading.value = false
  }
}

async function saveCurrentSearch() {
  if (!history) {
    showToast(t('bookSearch.toast.noHistoryController'), 'error')
    return
  }

  const payload = {
    keyword: querySummary.value,
    resultCount: totalResults.value,
    targetPath: `/books/search?${new URLSearchParams(buildQuery(pagination.page)).toString()}`,
    queryPayload: JSON.stringify(buildQuery(pagination.page)),
    label: querySummary.value,
  }

  await history.saveSearch(payload)
  showToast(t('bookSearch.toast.searchSaved'), 'success')
}

async function toggleFavoriteFromSearch(bookId: number) {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } }).catch(() => {})
    return
  }
  try {
    if (favoritedBookIds.value.has(bookId)) {
      await favoriteApi.removeFavorite(bookId)
      favoritedBookIds.value.delete(bookId)
    } else {
      await favoriteApi.addFavorite(bookId)
      favoritedBookIds.value.add(bookId)
    }
    // 触发响应式更新
    favoritedBookIds.value = new Set(favoritedBookIds.value)
  } catch (error: any) {
    showToast(error.response?.data?.message || t('bookSearch.toast.operationFailed'), 'error')
  }
}

function goToBookDetail(bookId: number) {
  router.push({ name: 'BookDetail', params: { id: bookId } }).catch(() => {})
}

function circulationLabel(policy: string) {
  if (policy === 'REFERENCE_ONLY') return t('bookSearch.circulation.referenceOnly')
  if (policy === 'MANUAL') return t('bookSearch.circulation.manualApproval')
  return t('bookSearch.circulation.autoApproval')
}

function showToast(message: string, type: 'success' | 'error' | 'info') {
  toast.message = message
  toast.type = type
  window.setTimeout(() => {
    toast.message = ''
  }, 2600)
}
</script>

<style scoped>
.search-layout {
  display: grid;
  grid-template-columns: minmax(17rem, 20rem) minmax(0, 1fr);
  gap: var(--space-6);
}

.search-sidebar {
  position: sticky;
  top: calc(var(--nav-height) + var(--space-5));
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: fit-content;
}

.search-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.search-group label {
  font-size: 0.82rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(215, 179, 122, 0.72);
}

.search-group input,
.search-group select,
.toolbar-actions select {
  width: 100%;
  min-height: 3rem;
  padding: 0.8rem 0.95rem;
  border-radius: 1rem;
  color: rgba(243, 233, 215, 0.94);
  border: 1px solid rgba(199, 160, 103, 0.16);
  background: linear-gradient(180deg, rgba(16, 12, 10, 0.9) 0%, rgba(11, 8, 6, 0.96) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.03),
    0 10px 24px rgba(0, 0, 0, 0.18);
  appearance: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.search-group input::placeholder {
  color: rgba(229, 217, 197, 0.42);
}

.search-group input:focus,
.search-group select:focus,
.toolbar-actions select:focus {
  outline: none;
  border-color: rgba(215, 179, 122, 0.36);
  box-shadow:
    0 0 0 4px rgba(199, 160, 103, 0.08),
    0 18px 36px rgba(0, 0, 0, 0.24);
  transform: translateY(-1px);
}

.search-group select option,
.toolbar-actions select option {
  color: rgba(243, 233, 215, 0.94);
  background: #120d0b;
}

.sidebar-submit {
  min-height: 3rem;
  border: none;
  border-radius: 999px;
  color: #1a130d;
  font-weight: 700;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #d7b37a 0%, #ba8850 48%, #efd0a6 100%);
  box-shadow: 0 18px 36px rgba(199, 160, 103, 0.26);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.sidebar-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 42px rgba(199, 160, 103, 0.34);
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.error-banner {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(80, 20, 24, 0.62) 0%, rgba(49, 11, 14, 0.78) 100%);
  color: #f4cbc5;
  border: 1px solid rgba(197, 48, 48, 0.24);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.18);
}

.results-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.toolbar-label {
  margin: 0 0 0.3rem;
  font-size: 0.75rem;
  font-family: var(--font-label);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(215, 179, 122, 0.72);
}

.toolbar-title {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.6rem;
  color: #fff0d5;
}

.toolbar-meta {
  margin: 0.5rem 0 0;
  color: rgba(229, 217, 197, 0.7);
}

.state-card {
  min-height: 15rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.85rem;
  text-align: center;
}

.state-title {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.3rem;
}

.state-copy {
  margin: 0;
  color: var(--on-surface-variant);
}

.spinner {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid rgba(215, 179, 122, 0.14);
  border-top-color: var(--primary);
  animation: spin 0.8s linear infinite;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1.2rem;
}

.result-card {
  appearance: none;
  display: flex;
  flex-direction: column;
  padding: 1rem;
  width: 100%;
  text-align: left;
  border-radius: 1.35rem;
  background:
    linear-gradient(180deg, rgba(246, 236, 218, 0.94) 0%, rgba(235, 222, 201, 0.92) 100%);
  border: 1px solid rgba(133, 122, 107, 0.18);
  box-shadow:
    0 22px 42px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.result-card:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 4px;
}

.result-card:hover {
  transform: translateY(-6px);
  border-color: rgba(199, 160, 103, 0.36);
  box-shadow:
    0 28px 54px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

.result-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  border-radius: 1rem;
  overflow: hidden;
  background: rgba(228, 230, 225, 0.85);
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(18, 59, 93, 0.4);
}

.result-badge {
  position: absolute;
  top: 0.8rem;
  right: 0.8rem;
  padding: 0.38rem 0.72rem;
  border-radius: 999px;
  color: white;
  font-size: 0.74rem;
  font-weight: 700;
}

.result-badge--available {
  background: #2f855a;
}

.result-badge--busy {
  background: #c05621;
}

.result-copy {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding-top: 1rem;
}

.result-copy h3 {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.2rem;
}

.result-author,
.result-meta {
  margin: 0;
  color: var(--on-surface-variant);
}

.location-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  width: fit-content;
  margin-top: 0.25rem;
  padding: 0.55rem 0.85rem;
  border-radius: 999px;
  background: rgba(86, 58, 32, 0.1);
  color: #7a502d;
  font-size: 0.84rem;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 0.6rem;
  padding-top: 0.85rem;
  border-top: 1px solid rgba(133, 122, 107, 0.12);
  font-size: 0.8rem;
  color: rgba(68, 48, 34, 0.78);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
}

.pagination-btn,
.pagination-number {
  min-width: 2.8rem;
  min-height: 2.8rem;
  padding: 0 0.85rem;
  color: rgba(243, 233, 215, 0.92);
  border: 1px solid rgba(199, 160, 103, 0.16);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(16, 12, 10, 0.88) 0%, rgba(10, 7, 5, 0.94) 100%);
  cursor: pointer;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.pagination-btn:hover:not(:disabled),
.pagination-number:hover:not(.active) {
  transform: translateY(-2px);
  border-color: rgba(215, 179, 122, 0.28);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.22);
}

.pagination-btn:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.pagination-number.active {
  color: #1a130d;
  background: linear-gradient(135deg, #d7b37a 0%, #ba8850 48%, #efd0a6 100%);
  box-shadow: 0 16px 32px rgba(199, 160, 103, 0.26);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1024px) {
  .search-layout {
    grid-template-columns: 1fr;
  }

  .search-sidebar {
    position: static;
  }
}

@media (max-width: 767px) {
  .results-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .pagination {
    flex-wrap: wrap;
  }
}

.fav-heart {
  position: absolute;
  bottom: 0.6rem;
  left: 0.6rem;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.88);
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  z-index: 2;
}

.fav-heart .material-symbols-outlined {
  font-size: 1.1rem;
}

.fav-heart:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}

.fav-heart--active {
  color: #e53e3e;
  background: rgba(229, 62, 62, 0.1);
}

.fav-heart--active:hover {
  background: rgba(229, 62, 62, 0.18);
}
</style>
