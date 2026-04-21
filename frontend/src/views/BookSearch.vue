<template>
  <div class="book-search page-stack">
    <PageHeader
      :title="t('bookSearch.title')"
      eyebrow="ARCHIVE SEARCH"
      :description="t('bookSearch.description')"
    >
      <template #actions>
        <div class="page-actions">
          <LibraryButton type="secondary" @click="resetFilters">
            {{ t('bookSearch.resetFilters') }}
          </LibraryButton>
          <LibraryButton type="primary" :loading="loading" @click="saveCurrentSearch">
            {{ t('bookSearch.saveSearch') }}
          </LibraryButton>
        </div>
      </template>
    </PageHeader>

    <div class="search-layout">
      <aside v-reveal="{ preset: 'sidebar', once: true }" class="search-sidebar surface-card">
        <div class="search-group">
          <label>{{ t('bookSearch.sidebar.keyword') }}</label>
          <div ref="keywordComboboxRef" class="search-combobox">
            <input
              v-model="form.keyword"
              type="text"
              role="combobox"
              aria-autocomplete="list"
              :aria-expanded="keywordAutocompleteOpen"
              :aria-controls="keywordAutocompleteListId"
              :aria-activedescendant="keywordAutocompleteActiveIndex >= 0 ? getKeywordAutocompleteOptionId(keywordAutocompleteActiveIndex) : undefined"
              :placeholder="t('bookSearch.sidebar.keywordPlaceholder')"
              @focus="openKeywordAutocompleteIfAvailable()"
              @blur="handleKeywordBlur"
              @input="handleKeywordInput"
              @keydown="handleKeywordKeydown"
            />
            <div
              v-if="keywordAutocompleteLoading || keywordAutocompleteOpen"
              :id="keywordAutocompleteListId"
              class="search-autocomplete"
              role="listbox"
            >
              <div v-if="keywordAutocompleteLoading && keywordAutocompleteSuggestions.length === 0" class="search-autocomplete__loading">
                <span class="material-symbols-outlined" aria-hidden="true">hourglass_top</span>
                <span>{{ autocompleteLoadingLabel }}</span>
              </div>
              <button
                v-for="(suggestion, index) in keywordAutocompleteSuggestions"
                v-else
                :id="getKeywordAutocompleteOptionId(index)"
                :key="`${suggestion}-${index}`"
                :class="['search-autocomplete__item', { 'search-autocomplete__item--active': index === keywordAutocompleteActiveIndex }]"
                type="button"
                role="option"
                :aria-selected="index === keywordAutocompleteActiveIndex"
                @mousedown.prevent="applyKeywordSuggestion(suggestion)"
              >
                <span class="material-symbols-outlined" aria-hidden="true">history</span>
                <span>{{ suggestion }}</span>
              </button>
            </div>
          </div>
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
        <button class="sidebar-submit" type="button" :disabled="loading" @click="submitSearch">
          <span v-if="loading" class="btn-spinner"></span>
          {{ loading ? t('bookSearch.sidebar.searching') : t('bookSearch.sidebar.updateResults') }}
        </button>
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
            <p v-if="smartSearchEngine" class="toolbar-engine">
              {{ searchEngineCopy.label }}: {{ smartSearchEngine === 'ELASTICSEARCH' ? searchEngineCopy.elasticsearch : searchEngineCopy.fallback }}
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

        <div v-if="showSearchAssistance" v-reveal="{ preset: 'section', delay: 0.07, once: true }" class="search-assistance surface-card">
          <p v-if="smartInterpretation" class="search-assistance__eyebrow">{{ assistanceCopy.interpretationLabel }}</p>
          <p v-if="smartInterpretation" class="search-assistance__interpretation">{{ smartInterpretation }}</p>
          <div v-if="smartDidYouMean" class="search-assistance__row">
            <span class="search-assistance__label">{{ assistanceCopy.didYouMean }}</span>
            <button class="search-assistance__pill search-assistance__pill--primary" type="button" @click="applySearchAssistance(smartDidYouMean)">
              {{ smartDidYouMean }}
            </button>
          </div>
          <div v-if="filteredSmartSuggestions.length > 0" class="search-assistance__row">
            <span class="search-assistance__label">{{ assistanceCopy.moreSuggestions }}</span>
            <div class="search-assistance__chips">
              <button
                v-for="suggestion in filteredSmartSuggestions"
                :key="suggestion"
                class="search-assistance__pill"
                type="button"
                @click="applySearchAssistance(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>
          <p class="search-assistance__hint">{{ assistanceCopy.hint }}</p>
        </div>

        <div v-if="loading" :key="`loading-${resultMotionKey}`" class="result-grid">
          <div
            v-for="i in form.size"
            :key="`skeleton-${i}`"
            class="skeleton-card"
            :style="{ animationDelay: `${i * 0.06}s` }"
          >
            <div class="skeleton-cover"></div>
            <div class="skeleton-body">
              <div class="skeleton-line skeleton-line--title"></div>
              <div class="skeleton-line skeleton-line--author"></div>
              <div class="skeleton-line skeleton-line--meta"></div>
              <div class="skeleton-pill"></div>
              <div class="skeleton-footer">
                <div class="skeleton-line skeleton-line--short"></div>
                <div class="skeleton-line skeleton-line--short"></div>
              </div>
            </div>
          </div>
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
          <el-pagination
            :current-page="pagination.page + 1"
            :page-size="form.size"
            :total="totalResults"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </div>

    <FeedbackToast v-if="toast.message" :toast="toast" @close="toast.message = ''" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { bookApi, type Book, type BookSearchParams, type SmartSearchResponse } from '../api/bookApi'
import { favoriteApi } from '../api/favoriteApi'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import LibraryButton from '../components/common/LibraryButton.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'
import { useUserStore } from '../stores/user'
import { useToast } from '../composables/useToast'
import { useSearchAutocomplete } from '../composables/useSearchAutocomplete'
import { useOffline } from '../composables/useOffline'
import { toggleFavoriteOfflineAware } from '../composables/useFavoriteOffline'

const { t, locale } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const history = inject<any>('searchHistoryController', null)
const { isOnline, enqueueOperation } = useOffline()
const keywordComboboxRef = ref<HTMLElement | null>(null)
const {
  listId: keywordAutocompleteListId,
  suggestions: keywordAutocompleteSuggestions,
  loading: keywordAutocompleteLoading,
  isOpen: keywordAutocompleteOpen,
  activeIndex: keywordAutocompleteActiveIndex,
  getOptionId: getKeywordAutocompleteOptionId,
  schedule: scheduleKeywordAutocomplete,
  openIfAvailable: openKeywordAutocompleteIfAvailable,
  close: closeKeywordAutocomplete,
  clear: clearKeywordAutocomplete,
  selectSuggestion: selectKeywordAutocomplete,
  handleKeydown: handleKeywordAutocompleteKeydown,
} = useSearchAutocomplete(async (query, limit) => {
  const response = await bookApi.getSearchSuggestions(query, limit)
  return response.data.success ? response.data.data : []
})

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
const { toast, showToast } = useToast()

const favoritedBookIds = ref<Set<number>>(new Set())
const smartDidYouMean = ref('')
const smartSuggestions = ref<string[]>([])
const smartInterpretation = ref('')
const smartSearchEngine = ref<SmartSearchResponse['searchEngine'] | ''>('')

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

const autocompleteLoadingLabel = computed(() =>
  locale.value.startsWith('zh') ? '正在生成联想建议…' : 'Loading suggestions…',
)

const assistanceCopy = computed(() => (
  locale.value.startsWith('zh')
    ? {
        interpretationLabel: '系统理解',
        didYouMean: '您是不是想搜',
        moreSuggestions: '还可以试试',
        hint: '这些建议来自热门检索、书名和作者名。',
      }
    : {
        interpretationLabel: 'System interpretation',
        didYouMean: 'Did you mean',
        moreSuggestions: 'Try one of these',
        hint: 'Suggestions are based on popular searches, titles, and author names.',
      }
))

const searchEngineCopy = computed(() => (
  locale.value.startsWith('zh')
    ? {
        label: '搜索引擎',
        elasticsearch: 'Elasticsearch',
        fallback: 'MySQL 回退',
      }
    : {
        label: 'Search engine',
        elasticsearch: 'Elasticsearch',
        fallback: 'MySQL fallback',
      }
))

const assistanceQuery = computed(() =>
  [String(form.keyword || '').trim(), String(form.author || '').trim()]
    .filter(Boolean)
    .join(' ')
)

const shouldUseSmartSearch = computed(() => Boolean(assistanceQuery.value))

const filteredSmartSuggestions = computed(() => {
  const blocked = new Set([
    normalizeSearchText(assistanceQuery.value),
    normalizeSearchText(smartDidYouMean.value),
  ])

  return Array.from(
    new Set(
      smartSuggestions.value.filter(suggestion => !blocked.has(normalizeSearchText(suggestion))),
    ),
  )
})

const showSearchAssistance = computed(() =>
  Boolean(assistanceQuery.value) &&
  (Boolean(smartDidYouMean.value) || Boolean(smartInterpretation.value) || filteredSmartSuggestions.value.length > 0) &&
  (books.value.length === 0 || totalResults.value < 3),
)

onMounted(async () => {
  document.addEventListener('mousedown', handleDocumentPointerDown)
  await Promise.all([loadCategories(), loadLanguages()])
  syncFromRoute()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentPointerDown)
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
  clearKeywordAutocomplete()
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

function handlePageChange(page: number) {
  goToPage(page - 1)
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

function normalizeSearchText(value: string) {
  return value.trim().toLowerCase()
}

function resetSmartAssistance() {
  smartDidYouMean.value = ''
  smartSuggestions.value = []
  smartInterpretation.value = ''
  smartSearchEngine.value = ''
}

function syncSmartAssistance(payload: SmartSearchResponse | null, query: string) {
  if (!payload) {
    resetSmartAssistance()
    return
  }

  const normalizedQuery = normalizeSearchText(query)
  const nextDidYouMean = payload.didYouMean?.trim() || ''

  smartDidYouMean.value =
    nextDidYouMean && normalizeSearchText(nextDidYouMean) !== normalizedQuery
      ? nextDidYouMean
      : ''
  smartSuggestions.value = Array.from(
    new Set(
      (payload.suggestions || [])
        .map(item => item.trim())
        .filter(item => item && normalizeSearchText(item) !== normalizedQuery),
    ),
  )
  smartInterpretation.value = payload.interpretation?.trim() || ''
  smartSearchEngine.value = payload.searchEngine
}

async function fetchResults() {
  loading.value = true
  errorMessage.value = ''
  try {
    const assistiveQuery = assistanceQuery.value
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
    if (shouldUseSmartSearch.value) {
      const smartResponse = await bookApi.smartSearch({
        query: assistiveQuery,
        page: form.page,
        size: form.size,
      }).then(result => (result.data.success ? result.data.data : null))

      syncSmartAssistance(smartResponse, assistiveQuery)
      books.value = smartResponse?.books || []
      totalResults.value = smartResponse?.total || books.value.length
      pagination.page = smartResponse?.page || 0
      pagination.totalPages = smartResponse?.totalPages || 0
    } else {
      resetSmartAssistance()
      const response = await bookApi.searchBooks(params)
      books.value = response.data.data || []
      totalResults.value = response.data.total || books.value.length
      pagination.page = response.data.page || 0
      pagination.totalPages = response.data.totalPages || 0
    }

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
    resetSmartAssistance()
    books.value = []
    totalResults.value = 0
    pagination.totalPages = 0
    errorMessage.value = error.response?.data?.message || t('bookSearch.toast.serviceUnavailable')
  } finally {
    loading.value = false
  }
}

function handleKeywordInput() {
  scheduleKeywordAutocomplete(String(form.keyword || ''))
}

function handleKeywordKeydown(event: KeyboardEvent) {
  if (event.isComposing || event.keyCode === 229) {
    return
  }

  const selectedSuggestion = handleKeywordAutocompleteKeydown(event)
  if (selectedSuggestion) {
    applyKeywordSuggestion(selectedSuggestion)
    return
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    submitSearch()
  }
}

function applyKeywordSuggestion(suggestion: string) {
  form.keyword = selectKeywordAutocomplete(suggestion)
  submitSearch()
}

function applySearchAssistance(suggestion: string) {
  if (form.keyword?.trim()) {
    form.keyword = suggestion
  } else if (form.author?.trim()) {
    form.author = suggestion
  } else {
    form.keyword = suggestion
  }

  clearKeywordAutocomplete()
  submitSearch()
}

function handleKeywordBlur() {
  window.setTimeout(() => {
    if (!keywordComboboxRef.value?.contains(document.activeElement)) {
      closeKeywordAutocomplete()
    }
  }, 0)
}

function handleDocumentPointerDown(event: MouseEvent) {
  if (!keywordComboboxRef.value) return
  if (!keywordComboboxRef.value.contains(event.target as Node)) {
    closeKeywordAutocomplete()
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
    const isFavorited = favoritedBookIds.value.has(bookId)
    await toggleFavoriteOfflineAware({
      bookId,
      isFavorited,
      isOnline,
      enqueueOperation,
    })
    if (isFavorited) {
      favoritedBookIds.value.delete(bookId)
    } else {
      favoritedBookIds.value.add(bookId)
    }
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

.search-combobox {
  position: relative;
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

.search-autocomplete {
  position: absolute;
  top: calc(100% + 0.45rem);
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.45rem;
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(23, 18, 15, 0.98) 0%, rgba(12, 9, 8, 0.98) 100%);
  border: 1px solid rgba(199, 160, 103, 0.18);
  box-shadow: 0 22px 38px rgba(0, 0, 0, 0.3);
  z-index: 6;
}

.search-autocomplete__loading,
.search-autocomplete__item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  min-height: 2.7rem;
  width: 100%;
  padding: 0.7rem 0.8rem;
  border: none;
  border-radius: 0.85rem;
  background: transparent;
  color: rgba(243, 233, 215, 0.92);
  text-align: left;
}

.search-autocomplete__loading {
  color: rgba(229, 217, 197, 0.7);
}

.search-autocomplete__item {
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.search-autocomplete__item:hover,
.search-autocomplete__item--active {
  background: rgba(215, 179, 122, 0.14);
  transform: translateY(-1px);
}

.search-autocomplete__item .material-symbols-outlined,
.search-autocomplete__loading .material-symbols-outlined {
  color: rgba(215, 179, 122, 0.72);
  font-size: 1.05rem;
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

.sidebar-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-spinner {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  border: 2px solid rgba(26, 19, 13, 0.2);
  border-top-color: #1a130d;
  animation: spin 0.7s linear infinite;
  vertical-align: middle;
  margin-right: 0.35rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.search-assistance {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.search-assistance__eyebrow {
  margin: 0;
  font-size: 0.72rem;
  font-family: var(--font-label);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(215, 179, 122, 0.72);
}

.search-assistance__interpretation,
.search-assistance__hint {
  margin: 0;
  color: rgba(229, 217, 197, 0.78);
}

.search-assistance__row {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.search-assistance__label {
  min-width: 5.5rem;
  padding-top: 0.35rem;
  color: rgba(243, 233, 215, 0.9);
  font-weight: 700;
}

.search-assistance__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.search-assistance__pill {
  min-height: 2.5rem;
  padding: 0.55rem 1rem;
  border-radius: 999px;
  border: 1px solid rgba(215, 179, 122, 0.18);
  background: rgba(215, 179, 122, 0.08);
  color: rgba(255, 240, 213, 0.94);
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.search-assistance__pill:hover {
  transform: translateY(-1px);
  background: rgba(215, 179, 122, 0.14);
  border-color: rgba(215, 179, 122, 0.28);
}

.search-assistance__pill--primary {
  background: linear-gradient(135deg, rgba(215, 179, 122, 0.2) 0%, rgba(186, 136, 80, 0.2) 100%);
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

.toolbar-engine {
  margin: 0.35rem 0 0;
  color: rgba(215, 179, 122, 0.84);
  font-size: 0.82rem;
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

/* ---- Skeleton Loading ---- */
.skeleton-card {
  display: flex;
  flex-direction: column;
  padding: 1rem;
  border-radius: 1.35rem;
  background:
    linear-gradient(180deg, rgba(246, 236, 218, 0.94) 0%, rgba(235, 222, 201, 0.92) 100%);
  border: 1px solid rgba(133, 122, 107, 0.18);
  box-shadow:
    0 22px 42px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.skeleton-cover {
  aspect-ratio: 3 / 4;
  border-radius: 1rem;
  background: rgba(86, 58, 32, 0.08);
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding-top: 1rem;
}

.skeleton-line {
  height: 0.85rem;
  border-radius: 999px;
  background: rgba(86, 58, 32, 0.1);
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-line--title {
  width: 70%;
  height: 1.1rem;
}

.skeleton-line--author {
  width: 45%;
}

.skeleton-line--meta {
  width: 60%;
}

.skeleton-line--short {
  width: 30%;
  height: 0.75rem;
}

.skeleton-pill {
  width: fit-content;
  margin-top: 0.25rem;
  padding: 0.55rem 1.2rem;
  border-radius: 999px;
  background: rgba(86, 58, 32, 0.08);
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 0.6rem;
  padding-top: 0.85rem;
  border-top: 1px solid rgba(133, 122, 107, 0.12);
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
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

:deep(.el-pagination) {
  --el-pagination-button-bg-color: rgba(16, 12, 10, 0.88);
  --el-pagination-button-color: rgba(243, 233, 215, 0.92);
  --el-pagination-hover-color: var(--el-color-primary);
}

@media (max-width: 1024px) {
  .search-layout {
    grid-template-columns: 1fr;
  }

  .search-sidebar {
    position: static;
  }

  .search-autocomplete {
    position: static;
    margin-top: 0.45rem;
  }
}

@media (max-width: 767px) {
  .results-toolbar {
    flex-direction: column;
    align-items: flex-start;
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
