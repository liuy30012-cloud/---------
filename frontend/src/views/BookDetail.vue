<template>
  <div class="book-detail page-stack">
    <div v-if="loading" v-reveal="{ preset: 'section', once: true }" class="loading surface-card">
      <div class="spinner"></div>
      <p>正在加载图书详情…</p>
    </div>

    <template v-else-if="book">
      <PageHeader
        :title="book.title"
        eyebrow="Book Profile"
        description="从真实评分、最新评论、馆藏定位到借阅/预约动作，都在同一个读者详情页里完成。"
      >
        <template #actions>
          <div class="page-actions">
            <button class="page-action-btn page-action-btn--secondary" type="button" @click="goBack">
              返回结果页
            </button>
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
            {{ book.availableCopies > 0 ? '可立即处理' : '当前需排队' }}
          </div>

          <div class="action-stack">
            <button
              class="page-action-btn page-action-btn--primary"
              type="button"
              :disabled="isSubmitting || !book.availabilityContext.canBorrow"
              @click="requestBorrow"
            >
              {{ borrowButtonText }}
            </button>
            <button
              class="page-action-btn page-action-btn--secondary"
              type="button"
              :disabled="isSubmitting || !book.availabilityContext.canReserve"
              @click="requestReservation"
            >
              加入预约队列
            </button>
          </div>
        </div>

        <div class="book-info-panel">
          <div class="rating-strip">
            <div class="stars">
              <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(book.averageRating || 0) }">★</span>
            </div>
            <span class="rating-text">{{ Number(book.averageRating || 0).toFixed(1) }} · {{ book.totalReviews }} 条评价</span>
          </div>

          <div class="meta-grid">
            <div class="meta-card">
              <span class="meta-label">作者</span>
              <span class="meta-value">{{ book.author }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">ISBN</span>
              <span class="meta-value">{{ book.isbn }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">分类</span>
              <span class="meta-value">{{ book.category }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">流通策略</span>
              <span class="meta-value">{{ circulationLabel(book.circulationPolicy) }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">语言</span>
              <span class="meta-value">{{ book.languageCode }}</span>
            </div>
            <div class="meta-card">
              <span class="meta-label">年份</span>
              <span class="meta-value">{{ book.year || '未知' }}</span>
            </div>
          </div>

          <div class="status-cards">
            <div class="status-card">
              <span class="status-card-label">可借副本</span>
              <strong>{{ book.availableCopies }}/{{ book.totalCopies }}</strong>
              <p>{{ book.availabilityContext.summary }}</p>
            </div>
            <div class="status-card">
              <span class="status-card-label">预约队列</span>
              <strong>{{ book.queueContext.waitingCount }}</strong>
              <p>{{ book.queueContext.summary }}</p>
            </div>
            <div class="status-card">
              <span class="status-card-label">累计借阅</span>
              <strong>{{ book.borrowHistorySummary.totalBorrows }}</strong>
              <p>当前在借 {{ book.borrowHistorySummary.activeBorrowCount }} 册</p>
            </div>
          </div>

          <div class="description-section">
            <h3>内容简介</h3>
            <p>{{ book.description || '暂无简介。' }}</p>
          </div>
        </div>
      </section>

      <section class="detail-grid">
        <article v-reveal="{ preset: 'card', delay: 0.04, once: true }" class="surface-card">
          <h2 class="section-title">定位与取书</h2>
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
          <h2 class="section-title">流通摘要</h2>
          <div class="summary-metric">
            <span>最近一次借出</span>
            <strong>{{ formatDate(book.borrowHistorySummary.lastBorrowedAt) }}</strong>
          </div>
          <div class="timeline-list">
            <div v-for="item in book.borrowHistorySummary.recentActivity" :key="item.id" class="timeline-item">
              <div>
                <p class="timeline-title">{{ statusLabel(item.status) }}</p>
                <p class="timeline-meta">借出 {{ formatDate(item.borrowDate) }} · 到期 {{ formatDate(item.dueDate) }}</p>
              </div>
              <span class="timeline-status">{{ item.status }}</span>
            </div>
          </div>
        </article>
      </section>

      <section v-reveal="{ preset: 'section', delay: 0.08, once: true }" class="surface-card">
        <h2 class="section-title">最新评论</h2>
        <div v-if="userStore.isLoggedIn" class="review-form">
          <div class="review-form-head">
            <span>我来评分</span>
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
          <textarea v-model="reviewDraft.content" rows="4" placeholder="分享你对这本书的真实感受" />
          <div class="review-actions">
            <button class="page-action-btn page-action-btn--primary" type="button" :disabled="isSubmitting" @click="submitReview">
              提交评价
            </button>
          </div>
        </div>
        <div v-else class="review-guest-note">登录后可提交评价并参与评分。</div>

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
            <p>{{ review.content || '这位读者只留下了评分。' }}</p>
          </article>
        </div>
        <div v-else class="empty-copy">还没有读者评价，欢迎留下第一条真实反馈。</div>
      </section>

      <section v-reveal="{ preset: 'section', delay: 0.14, once: true }" class="surface-card">
        <h2 class="section-title">相关馆藏</h2>
        <div v-if="book.relatedBooks.length > 0" class="related-grid">
          <button
            v-for="(related, index) in book.relatedBooks"
            :key="related.id"
            v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
            class="related-card"
            type="button"
            :aria-label="`查看相关图书：${related.title}`"
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
        <div v-else class="empty-copy">当前没有同类推荐。</div>
      </section>
    </template>

    <div v-else v-reveal="{ preset: 'section', once: true }" class="error-state surface-card">
      <p>图书详情加载失败。</p>
      <button class="page-action-btn page-action-btn--secondary" type="button" @click="goBack">返回</button>
    </div>

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
    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookApi, type BookDetail } from '../api/bookApi'
import { borrowApi, reservationApi } from '../api/borrowApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { useUserStore } from '../stores/user'
import { handleImageError } from '../utils/imageHelpers'
import { logger } from '../utils/logger'
import { formatLocalDate as formatDate } from '../utils/timeHelpers'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const book = ref<BookDetail | null>(null)
const isSubmitting = ref(false)
const reviewDraft = reactive({
  rating: 5,
  content: '',
})

const dialog = reactive({
  open: false,
  eyebrow: '',
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  action: '' as 'borrow' | 'reserve' | '',
})

const toast = reactive<{ message: string; type: 'success' | 'error' | 'info' }>({
  message: '',
  type: 'info',
})

const borrowButtonText = computed(() => {
  if (!book.value) return '借阅'
  if (book.value.circulationPolicy === 'REFERENCE_ONLY') return '馆内阅览'
  if (book.value.circulationPolicy === 'MANUAL') return '申请借阅（人工审批）'
  return '申请借阅（自动审批）'
})

const onImgError = (event: Event) => handleImageError(event, '/logo-photo.jpg')

onMounted(() => {
  void loadBookDetail()
})

watch(() => route.params.id, () => {
  void loadBookDetail()
})

async function loadBookDetail() {
  loading.value = true
  try {
    const bookId = Number(route.params.id)
    const response = await bookApi.getBookDetail(bookId)
    book.value = response.data.success ? response.data.data : null
  } catch (error) {
    logger.error('Failed to load book detail:', error)
    book.value = null
  } finally {
    loading.value = false
  }
}

function ensureAuth() {
  if (userStore.isLoggedIn) return true
  router.push({ name: 'Login', query: { redirect: route.fullPath } }).catch(() => {})
  return false
}

function requestBorrow() {
  if (!book.value || !ensureAuth()) return
  dialog.open = true
  dialog.action = 'borrow'
  dialog.eyebrow = 'Borrow'
  dialog.title = '确认借阅请求'
  dialog.message = book.value.circulationPolicy === 'MANUAL'
    ? '这本书会进入人工审批队列。提交后你可以在“我的借阅”里看到审批人和下一步动作。'
    : '这本书会走自动审批流程，通过后直接进入待取书状态。'
  dialog.confirmText = '提交借阅'
  dialog.cancelText = '再想想'
}

function requestReservation() {
  if (!book.value || !ensureAuth()) return
  dialog.open = true
  dialog.action = 'reserve'
  dialog.eyebrow = 'Reserve'
  dialog.title = '加入预约队列'
  dialog.message = '提交后你会在“我的预约”里看到排队人数、预计等待时间和到馆提醒。'
  dialog.confirmText = '确认预约'
  dialog.cancelText = '取消'
}

function closeDialog() {
  dialog.open = false
  dialog.action = ''
}

async function runDialogAction() {
  if (!book.value || !dialog.action) return
  isSubmitting.value = true
  try {
    if (dialog.action === 'borrow') {
      const response = await borrowApi.applyBorrow({ bookId: book.value.id, notes: '' })
      showToast(response.data.message || '借阅请求已提交。', 'success')
    } else {
      const response = await reservationApi.reserveBook({ bookId: book.value.id })
      showToast(response.data.message || '预约已提交。', 'success')
    }
    closeDialog()
    await loadBookDetail()
  } catch (error: any) {
    showToast(error.response?.data?.message || '操作失败，请稍后重试。', 'error')
  } finally {
    isSubmitting.value = false
  }
}

async function submitReview() {
  if (!book.value || !ensureAuth()) return
  if (!reviewDraft.content.trim()) {
    showToast('请先输入评价内容。', 'error')
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
    showToast('评价已提交。', 'success')
    await loadBookDetail()
  } catch (error: any) {
    logger.error('Failed to submit review:', error)
    showToast(error.response?.data?.message || '评价提交失败。', 'error')
  } finally {
    isSubmitting.value = false
  }
}

function showToast(message: string, type: 'success' | 'error' | 'info') {
  toast.message = message
  toast.type = type
  window.setTimeout(() => {
    toast.message = ''
  }, 2600)
}

function circulationLabel(policy: string) {
  if (policy === 'REFERENCE_ONLY') return '馆内阅览'
  if (policy === 'MANUAL') return '人工审批'
  return '自动审批'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审批',
    APPROVED: '待取书',
    BORROWED: '借阅中',
    RETURNED: '已归还',
    OVERDUE: '已逾期',
    REJECTED: '未通过',
  }
  return map[status] || status
}

function goBack() {
  router.back()
}

function goToBook(bookId: number) {
  router.push({ name: 'BookDetail', params: { id: bookId } }).catch(() => {})
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
</style>
