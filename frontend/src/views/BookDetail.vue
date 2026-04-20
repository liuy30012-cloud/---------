<template>
  <div class="book-detail page-stack">
    <div v-if="loading" v-reveal="{ preset: 'section', once: true }" class="loading surface-card">
      <div class="spinner"></div>
      <p>{{ t('bookDetail.loading') }}</p>
    </div>

    <template v-else-if="book">
      <PageHeader
        :title="book.title"
        eyebrow="Book Profile"
        :description="t('bookDetail.description')"
      >
        <template #actions>
          <div class="page-actions">
            <LibraryButton type="ghost" @click="goBack">{{ t('bookDetail.backToResults') }}</LibraryButton>
          </div>
        </template>
      </PageHeader>

      <section v-reveal="{ preset: 'section', once: true }" class="book-main surface-card">
        <div class="book-cover-panel">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" class="book-cover-large" @error="onImgError" />
          <div v-else class="book-cover-placeholder-large">
            <span class="material-symbols-outlined">menu_book</span>
          </div>

          <div class="status-ribbon" :class="book.availableCopies > 0 ? 'status-ribbon--available' : 'status-ribbon--busy'">
            {{ book.availableCopies > 0 ? t('bookDetail.statusRibbon.available') : t('bookDetail.statusRibbon.busy') }}
          </div>

          <div class="action-stack">
            <LibraryButton
              type="primary"
              :disabled="isSubmitting || !book.availabilityContext.canBorrow"
              @click="requestBorrow"
            >
              {{ borrowButtonText }}
            </LibraryButton>
            <LibraryButton
              type="secondary"
              :disabled="isSubmitting || !book.availabilityContext.canReserve"
              @click="requestReservation"
            >
              {{ t('bookDetail.joinQueue') }}
            </LibraryButton>
            <LibraryButton
              v-if="userStore.isLoggedIn"
              type="danger"
              @click="showDamageModal = true"
            >
              {{ t('bookDetail.reportDamage') }}
            </LibraryButton>
          </div>
        </div>

        <div class="book-info-panel">
          <div class="rating-strip">
            <div class="stars">
              <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(book.averageRating || 0) }">★</span>
            </div>
            <span class="rating-text">{{ Number(book.averageRating || 0).toFixed(1) }} · {{ t('bookDetail.rating', { count: book.totalReviews }) }}</span>
          </div>

          <div v-if="userStore.isLoggedIn" class="personal-actions">
            <button
              class="fav-toggle"
              :class="{ 'fav-toggle--active': isFavorited }"
              type="button"
              @click="toggleFavorite"
            >
              <span class="material-symbols-outlined">{{ isFavorited ? 'favorite' : 'favorite_border' }}</span>
              <span>{{ isFavorited ? t('bookDetail.favorite.active') : t('bookDetail.favorite.inactive') }}</span>
            </button>
            <div class="reading-status-selector">
              <select v-model="currentReadingStatus" @change="updateReadingStatus">
                <option value="">{{ t('bookDetail.readingStatus.label') }}</option>
                <option value="WANT_TO_READ">{{ t('bookDetail.readingStatus.wantToRead') }}</option>
                <option value="READING">{{ t('bookDetail.readingStatus.reading') }}</option>
                <option value="READ">{{ t('bookDetail.readingStatus.read') }}</option>
              </select>
            </div>
            <div v-if="currentReadingStatus" class="notes-section">
              <textarea
                v-model="readingNotes"
                rows="2"
                :placeholder="t('bookDetail.notes.placeholder')"
                @blur="saveNotes"
              />
            </div>
          </div>

          <div class="meta-grid">
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.author') }}</span>
              <span class="meta-value">{{ book.author }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.isbn') }}</span>
              <span class="meta-value">{{ book.isbn }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.category') }}</span>
              <span class="meta-value">{{ book.category }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.circulationPolicy') }}</span>
              <span class="meta-value">{{ circulationLabel(book.circulationPolicy) }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.language') }}</span>
              <span class="meta-value">{{ book.languageCode }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">{{ t('bookDetail.meta.year') }}</span>
              <span class="meta-value">{{ book.year || t('bookDetail.meta.unknown') }}</span>
            </div>
          </div>

          <div class="status-cards">
            <div class="status-card">
              <span class="status-card-label">{{ t('bookDetail.statusCards.availableCopies') }}</span>
              <strong>{{ book.availableCopies }}/{{ book.totalCopies }}</strong>
              <p>{{ book.availabilityContext.summary }}</p>
            </div>
            <div class="status-card">
              <span class="status-card-label">{{ t('bookDetail.statusCards.queue') }}</span>
              <strong>{{ book.queueContext.waitingCount }}</strong>
              <p>{{ book.queueContext.summary }}</p>
            </div>
            <div class="status-card">
              <span class="status-card-label">{{ t('bookDetail.statusCards.totalBorrows') }}</span>
              <strong>{{ book.borrowHistorySummary.totalBorrows }}</strong>
              <p>{{ t('bookDetail.statusCards.activeBorrow', { count: book.borrowHistorySummary.activeBorrowCount }) }}</p>
            </div>
          </div>

          <div class="description-section">
            <h3>{{ t('bookDetail.descriptionSection.title') }}</h3>
            <p>{{ book.description || t('bookDetail.descriptionSection.empty') }}</p>
          </div>
        </div>
      </section>

      <section class="detail-grid">
        <article v-reveal="{ preset: 'card', delay: 0.04, once: true }" class="surface-card">
          <h2 class="section-title">{{ t('bookDetail.location.title') }}</h2>
          <div class="breadcrumb-list">
            <span v-for="crumb in book.locationContext.breadcrumbs" :key="crumb" class="breadcrumb-chip">{{ crumb }}</span>
          </div>
          <div class="pickup-card">
            <h3>{{ book.locationContext.pickupCardTitle }}</h3>
            <p>{{ book.locationContext.pickupHint }}</p>
            <p v-if="book.locationContext.adjacentRecommendation" class="adjacent-tip">
              {{ book.locationContext.adjacentRecommendation }}
            </p>
          </div>
        </article>

        <article v-reveal="{ preset: 'card', delay: 0.12, once: true }" class="surface-card">
          <h2 class="section-title">{{ t('bookDetail.circulationSummary.title') }}</h2>
          <div class="summary-metric">
            <span>{{ t('bookDetail.circulationSummary.lastBorrowed') }}</span>
            <strong>{{ formatDate(book.borrowHistorySummary.lastBorrowedAt) }}</strong>
          </div>
          <div class="timeline-list">
            <div v-for="item in book.borrowHistorySummary.recentActivity" :key="item.id" class="timeline-item">
              <div>
                <p class="timeline-title">{{ statusLabel(item.status) }}</p>
                <p class="timeline-meta">{{ t('bookDetail.circulationSummary.borrowDue', { borrowDate: formatDate(item.borrowDate), dueDate: formatDate(item.dueDate) }) }}</p>
              </div>
              <span class="timeline-status">{{ item.status }}</span>
            </div>
          </div>
        </article>
      </section>

      <section v-reveal="{ preset: 'section', delay: 0.08, once: true }" class="surface-card">
        <h2 class="section-title">{{ t('bookDetail.reviews.title') }}</h2>
        <div v-if="userStore.isLoggedIn" class="review-form">
          <div class="review-form-head">
            <span>{{ t('bookDetail.reviews.myRating') }}</span>
            <div class="star-input">
              <button
                v-for="i in 5"
                :key="i"
                class="star-toggle"
                :class="{ active: i <= reviewDraft.rating }"
                type="button"
                @click="reviewDraft.rating = i"
              >
                ★
              </button>
            </div>
          </div>
          <textarea v-model="reviewDraft.content" rows="4" :placeholder="t('bookDetail.reviews.placeholder')" />
          <div class="review-actions">
            <LibraryButton type="primary" :loading="isSubmitting" success-text="评论已提交" error-text="评论提交失败" @click="submitReview">
              {{ t('bookDetail.reviews.submit') }}
            </LibraryButton>
          </div>
        </div>
        <div v-else class="review-guest-note">{{ t('bookDetail.reviews.guestNote') }}</div>

        <div v-if="book.latestReviews.length > 0" class="review-list">
          <article v-for="review in book.latestReviews" :key="review.id" class="review-item">
            <div class="review-header">
              <div>
                <strong>{{ review.username }}</strong>
                <div class="stars stars--small">
                  <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= review.rating }">★</span>
                </div>
              </div>
              <span>{{ formatDate(review.createdAt) }}</span>
            </div>
            <p>{{ review.content || t('bookDetail.reviews.ratingOnly') }}</p>
          </article>
        </div>
        <div v-else class="empty-copy">{{ t('bookDetail.reviews.empty') }}</div>
      </section>

      <section v-reveal="{ preset: 'section', delay: 0.14, once: true }" class="surface-card">
        <h2 class="section-title">{{ t('bookDetail.related.title') }}</h2>
        <div v-if="book.relatedBooks.length > 0" class="related-grid">
          <button
            v-for="(related, index) in book.relatedBooks"
            :key="related.id"
            v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
            class="related-card"
            type="button"
            :aria-label="t('bookDetail.related.ariaLabel', { title: related.title })"
            @click="goToBook(related.id)"
          >
            <img v-if="related.coverUrl" :src="related.coverUrl" :alt="related.title" @error="onImgError" />
            <div v-else class="related-placeholder">
              <span class="material-symbols-outlined">menu_book</span>
            </div>
            <h3>{{ related.title }}</h3>
            <p>{{ related.author }}</p>
            <span>{{ related.location }}</span>
          </button>
        </div>
        <div v-else class="empty-copy">{{ t('bookDetail.related.empty') }}</div>
      </section>
    </template>

    <div v-else v-reveal="{ preset: 'section', once: true }" class="error-state surface-card">
      <p>{{ t('bookDetail.error.title') }}</p>
      <LibraryButton type="ghost" @click="goBack">{{ t('bookDetail.error.back') }}</LibraryButton>
    </div>

    <DamageReportModal
      v-if="showDamageModal && book"
      :book-id="book.id"
      :book-title="book.title"
      @close="showDamageModal = false"
      @submitted="onDamageReportSubmitted"
    />
    <ConfirmDialog
      :open="dialog.open"
      :eyebrow="dialog.eyebrow"
      :title="dialog.title"
      :message="dialog.message"
      :confirm-text="dialog.confirmText"
      :cancel-text="dialog.cancelText"
      @cancel="closeDialog"
      @confirm="runDialogAction"
    />
    <FeedbackToast v-if="toast.message" :toast="toast" @close="toast.message = ''" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { bookApi, type BookDetail } from '../api/bookApi'
import { borrowApi, reservationApi } from '../api/borrowApi'
import { favoriteApi, readingStatusApi, type ReadingStatusEnum } from '../api/favoriteApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import DamageReportModal from '../components/damage/DamageReportModal.vue'
import LibraryButton from '@/components/common/LibraryButton.vue'
import { useUserStore } from '../stores/user'
import { handleImageError } from '../utils/imageHelpers'
import { logger } from '../utils/logger'
import { formatLocalDate as formatDate } from '../utils/timeHelpers'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { useOffline } from '../composables/useOffline'
import { toggleFavoriteOfflineAware } from '../composables/useFavoriteOffline'
import { applyBorrowOfflineAware, reserveBookOfflineAware } from '../composables/useBorrowOffline'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const { isOnline, enqueueOperation } = useOffline()

const loading = ref(true)
const book = ref<BookDetail | null>(null)
const isSubmitting = ref(false)
const showDamageModal = ref(false)
const reviewDraft = reactive({
  rating: 5,
  content: '',
})

const { dialog, openDialog, closeDialog: closeDialogBase } = useConfirmDialog()
const { toast, showToast } = useToast()

const isFavorited = ref(false)
const currentReadingStatus = ref<ReadingStatusEnum | ''>('')
const readingNotes = ref('')
const savedReadingNotes = ref('')
let latestBookDetailRequestId = 0

const borrowButtonText = computed(() => {
  if (!book.value) return t('bookDetail.borrowButton.default')
  if (book.value.circulationPolicy === 'REFERENCE_ONLY') return t('bookDetail.borrowButton.referenceOnly')
  if (book.value.circulationPolicy === 'MANUAL') return t('bookDetail.borrowButton.manualApproval')
  return t('bookDetail.borrowButton.autoApproval')
})

const onImgError = (event: Event) => handleImageError(event, '/logo-photo.jpg')

onMounted(() => {
  void loadBookDetail()
})

watch(() => route.params.id, () => {
  void loadBookDetail()
})

function normalizeReadingNotes(notes: string | null | undefined) {
  return notes ?? ''
}

function resetPersonalBookState() {
  isFavorited.value = false
  currentReadingStatus.value = ''
  readingNotes.value = ''
  savedReadingNotes.value = ''
}

async function loadBookDetail() {
  const requestId = ++latestBookDetailRequestId
  const requestedBookId = Number(route.params.id)
  loading.value = true
  resetPersonalBookState()
  try {
    const response = await bookApi.getBookDetail(requestedBookId)
    if (requestId !== latestBookDetailRequestId || requestedBookId !== Number(route.params.id)) return

    book.value = response.data.success ? response.data.data : null

    if (userStore.isLoggedIn && book.value) {
      const [favRes, statusRes] = await Promise.allSettled([
        favoriteApi.checkFavorite(book.value.id),
        readingStatusApi.getReadingStatus(book.value.id),
      ])
      if (requestId !== latestBookDetailRequestId || requestedBookId !== Number(route.params.id)) return

      if (favRes.status === 'fulfilled' && favRes.value.data.success) {
        isFavorited.value = favRes.value.data.data
      }
      if (statusRes.status === 'fulfilled' && statusRes.value.data.success && statusRes.value.data.data) {
        currentReadingStatus.value = statusRes.value.data.data.status
        const notes = normalizeReadingNotes(statusRes.value.data.data.notes)
        readingNotes.value = notes
        savedReadingNotes.value = notes
      }
    }
  } catch (error) {
    if (requestId !== latestBookDetailRequestId || requestedBookId !== Number(route.params.id)) return
    logger.error('Failed to load book detail:', error)
    book.value = null
  } finally {
    if (requestId === latestBookDetailRequestId && requestedBookId === Number(route.params.id)) {
      loading.value = false
    }
  }
}

function ensureAuth() {
  if (userStore.isLoggedIn) return true
  router.push({ name: 'Login', query: { redirect: route.fullPath } }).catch(() => {})
  return false
}

function requestBorrow() {
  if (!book.value || !ensureAuth()) return
  openDialog({
    eyebrow: 'Borrow',
    title: t('bookDetail.dialog.borrowTitle'),
    message: book.value.circulationPolicy === 'MANUAL'
      ? t('bookDetail.dialog.borrowManualMsg')
      : t('bookDetail.dialog.borrowAutoMsg'),
    confirmText: t('bookDetail.dialog.submitBorrow'),
    cancelText: t('bookDetail.dialog.thinkAgain'),
    action: 'borrow',
  })
}

function requestReservation() {
  if (!book.value || !ensureAuth()) return
  openDialog({
    eyebrow: 'Reserve',
    title: t('bookDetail.dialog.reserveTitle'),
    message: t('bookDetail.dialog.reserveMsg'),
    confirmText: t('bookDetail.dialog.confirmReserve'),
    cancelText: t('bookDetail.dialog.cancel'),
    action: 'reserve',
  })
}

function closeDialog() {
  closeDialogBase()
}

async function runDialogAction() {
  if (!book.value || !dialog.action) return
  isSubmitting.value = true
  try {
    if (dialog.action === 'borrow') {
      await applyBorrowOfflineAware({
        bookId: book.value.id,
        notes: '',
        isOnline,
        enqueueOperation,
      })
      showToast(t('bookDetail.toast.borrowSubmitted'), 'success')
    } else {
      await reserveBookOfflineAware({
        bookId: book.value.id,
        isOnline,
        enqueueOperation,
      })
      showToast(t('bookDetail.toast.reserveSubmitted'), 'success')
    }
    closeDialog()
    await loadBookDetail()
  } catch (error: any) {
    showToast(error.response?.data?.message || t('bookDetail.toast.operationFailed'), 'error')
  } finally {
    isSubmitting.value = false
  }
}

async function submitReview() {
  if (!book.value || !ensureAuth()) return
  if (!reviewDraft.content.trim()) {
    showToast(t('bookDetail.toast.reviewEmpty'), 'error')
    return
  }

  isSubmitting.value = true
  try {
    await bookApi.addReview({
      bookId: book.value.id,
      rating: reviewDraft.rating,
      content: reviewDraft.content.trim(),
    })
    reviewDraft.rating = 5
    reviewDraft.content = ''
    showToast(t('bookDetail.toast.reviewSubmitted'), 'success')
    await loadBookDetail()
  } catch (error: any) {
    logger.error('Failed to submit review:', error)
    showToast(error.response?.data?.message || t('bookDetail.toast.reviewFailed'), 'error')
  } finally {
    isSubmitting.value = false
  }
}

function circulationLabel(policy: string) {
  if (policy === 'REFERENCE_ONLY') return t('bookDetail.circulation.referenceOnly')
  if (policy === 'MANUAL') return t('bookDetail.circulation.manual')
  return t('bookDetail.circulation.auto')
}

function statusLabel(status: string) {
  const key = `bookDetail.statusLabel.${status}` as const
  const translated = t(key)
  return translated !== key ? translated : status
}

function goBack() {
  router.back()
}

function goToBook(bookId: number) {
  router.push({ name: 'BookDetail', params: { id: bookId } }).catch(() => {})
}

function onDamageReportSubmitted() {
  showToast(t('bookDetail.toast.damageReported'), 'success')
}

async function toggleFavorite() {
  if (!book.value || !ensureAuth()) return
  try {
    await toggleFavoriteOfflineAware({
      bookId: book.value.id,
      isFavorited: isFavorited.value,
      isOnline,
      enqueueOperation,
    })
    isFavorited.value = !isFavorited.value
    showToast(
      isFavorited.value ? t('bookDetail.toast.favoriteAdded') : t('bookDetail.toast.favoriteRemoved'),
      isFavorited.value ? 'success' : 'info',
    )
  } catch (error: any) {
    showToast(error.response?.data?.message || t('bookDetail.toast.operationFailed'), 'error')
  }
}

async function legacyUpdateReadingStatus() {
  if (!book.value || !ensureAuth()) return
  if (!currentReadingStatus.value) return
  try {
    await readingStatusApi.upsertReadingStatus(book.value.id, currentReadingStatus.value, readingNotes.value || undefined)
    showToast(t('bookDetail.toast.readingStatusUpdated'), 'success')
  } catch (error: any) {
    showToast(error.response?.data?.message || t('bookDetail.toast.readingStatusUpdateFailed'), 'error')
  }
}

async function legacySaveNotes() {
  if (!book.value || !currentReadingStatus.value) return
  try {
    await readingStatusApi.upsertReadingStatus(book.value.id, currentReadingStatus.value, readingNotes.value || undefined)
  } catch (error) {
    logger.warn('legacySaveNotes should not be used.', error)
    // 备注保存失败静默处理
  }
}
async function persistReadingStatus(options: {
  successMessage?: string
  errorMessage: string
  logMessage: string
}) {
  if (!book.value || !currentReadingStatus.value) return false

  try {
    const response = await readingStatusApi.upsertReadingStatus(
      book.value.id,
      currentReadingStatus.value,
      readingNotes.value || undefined,
    )
    const persistedStatus = response.data.data
    const notes = normalizeReadingNotes(persistedStatus.notes)

    currentReadingStatus.value = persistedStatus.status
    readingNotes.value = notes
    savedReadingNotes.value = notes

    if (options.successMessage) {
      showToast(options.successMessage, 'success')
    }

    return true
  } catch (error: any) {
    logger.error(options.logMessage, error)
    showToast(error.response?.data?.message || options.errorMessage, 'error')
    return false
  }
}

async function updateReadingStatus() {
  if (!book.value || !ensureAuth()) return
  if (!currentReadingStatus.value) return

  await persistReadingStatus({
    successMessage: t('bookDetail.toast.readingStatusUpdated'),
    errorMessage: t('bookDetail.toast.readingStatusUpdateFailed'),
    logMessage: 'Failed to update reading status:',
  })
}

async function saveNotes() {
  if (!book.value || !currentReadingStatus.value) return
  if (normalizeReadingNotes(readingNotes.value) === savedReadingNotes.value) return

  await persistReadingStatus({
    errorMessage: t('bookDetail.toast.notesSaveFailed'),
    logMessage: 'Failed to save reading notes:',
  })
}
</script>

<style scoped>
.book-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.loading,
.error-state {
  min-height: 16rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid rgba(0, 83, 219, 0.16);
  border-top-color: var(--primary);
  animation: spin 0.8s linear infinite;
}

.book-main {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-7);
}

.book-cover-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.book-cover-large,
.book-cover-placeholder-large {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 1.35rem;
}

.book-cover-large {
  object-fit: cover;
}

.book-cover-placeholder-large {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(219, 225, 255, 0.95), rgba(217, 227, 244, 0.85));
  color: rgba(0, 83, 219, 0.35);
}

.status-ribbon {
  position: absolute;
  top: 1rem;
  left: 1rem;
  padding: 0.5rem 0.8rem;
  border-radius: 999px;
  color: white;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-ribbon--available {
  background: #2f855a;
}

.status-ribbon--busy {
  background: #c05621;
}

.action-stack {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.book-info-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.rating-strip {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.stars {
  display: flex;
  gap: 0.2rem;
}

.star {
  color: #d1d5db;
}

.star.filled {
  color: #f59e0b;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.meta-card,
.status-card {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(133, 122, 107, 0.12);
}

.meta-label,
.status-card-label {
  font-size: 0.74rem;
  font-family: var(--font-label);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--outline);
}

.meta-value {
  font-weight: 700;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.status-card strong {
  font-size: 1.55rem;
  font-family: var(--font-headline);
}

.status-card p,
.description-section p,
.pickup-card p,
.review-item p {
  margin: 0;
  line-height: 1.75;
  color: var(--on-surface-variant);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.section-title {
  margin: 0 0 1rem;
  font-family: var(--font-headline);
  font-size: 1.25rem;
}

.breadcrumb-list {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.breadcrumb-chip {
  padding: 0.55rem 0.8rem;
  border-radius: 999px;
  background: rgba(18, 59, 93, 0.06);
  color: var(--primary);
  font-size: 0.84rem;
}

.pickup-card {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.72);
}

.adjacent-tip {
  margin-top: 0.5rem;
  color: var(--primary);
}

.summary-metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid rgba(133, 122, 107, 0.12);
}

.timeline-list,
.review-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.timeline-item,
.review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.72);
}

.timeline-title {
  margin: 0;
  font-weight: 700;
}

.timeline-meta {
  margin: 0.35rem 0 0;
  color: var(--on-surface-variant);
}

.timeline-status {
  font-size: 0.78rem;
  font-family: var(--font-label);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--outline);
}

.review-form {
  padding: 1rem;
  margin-bottom: 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.72);
}

.review-form-head,
.review-header,
.review-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.star-input {
  display: flex;
  gap: 0.2rem;
}

.star-toggle {
  border: none;
  background: transparent;
  color: #d1d5db;
  font-size: 1.3rem;
  cursor: pointer;
}

.star-toggle.active {
  color: #f59e0b;
}

.review-form textarea {
  width: 100%;
  margin-top: 0.85rem;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(133, 122, 107, 0.16);
}

.review-guest-note,
.empty-copy {
  color: var(--on-surface-variant);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 1rem;
}

.related-card {
  appearance: none;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.related-card:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 4px;
}

.related-card img,
.related-placeholder {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 1rem;
}

.related-card img {
  object-fit: cover;
}

.related-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(228, 230, 225, 0.85);
  color: rgba(18, 59, 93, 0.35);
}

.related-card h3,
.related-card p,
.related-card span {
  margin: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1024px) {
  .book-main,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .meta-grid,
  .status-cards {
    grid-template-columns: 1fr;
  }

  .timeline-item,
  .review-header,
  .review-form-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

.personal-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(133, 122, 107, 0.1);
}

.fav-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  border: 1px solid rgba(133, 122, 107, 0.18);
  background: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--on-surface-variant);
  transition: all 0.2s ease;
}

.fav-toggle:hover {
  background: rgba(0, 83, 219, 0.06);
}

.fav-toggle .material-symbols-outlined {
  font-size: 1.15rem;
}

.fav-toggle--active {
  color: #e53e3e;
  border-color: rgba(229, 62, 62, 0.25);
  background: rgba(229, 62, 62, 0.06);
}

.fav-toggle--active .material-symbols-outlined {
  color: #e53e3e;
}

.fav-toggle--active:hover {
  background: rgba(229, 62, 62, 0.12);
}

.reading-status-selector select {
  padding: 0.5rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(133, 122, 107, 0.18);
  background: rgba(255, 255, 255, 0.85);
  font-size: 0.88rem;
  color: var(--on-surface);
  cursor: pointer;
  outline: none;
}

.reading-status-selector select:focus {
  border-color: var(--primary);
}

.notes-section {
  flex-basis: 100%;
}

.notes-section textarea {
  width: 100%;
  padding: 0.6rem 0.85rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(133, 122, 107, 0.18);
  background: rgba(255, 255, 255, 0.85);
  font-size: 0.85rem;
  color: var(--on-surface);
  resize: vertical;
  outline: none;
  font-family: inherit;
}

.notes-section textarea:focus {
  border-color: var(--primary);
}
</style>
