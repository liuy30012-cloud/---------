<template>
  <div class="purchase-hub page-stack">
    <PageHeader
      :title="t('purchaseSuggestions.title')"
      eyebrow="Reader-Powered Acquisitions"
      :description="t('purchaseSuggestions.description')"
    >
      <template #actions>
        <div class="page-actions">
          <button class="page-action-btn page-action-btn--secondary" @click="loadBoard">
            <span class="material-symbols-outlined">refresh</span>
            <span>{{ t('purchaseSuggestions.buttons.refresh') }}</span>
          </button>
          <button
            v-if="isAdmin && suggestionSummary?.suggestions?.length"
            class="page-action-btn page-action-btn--secondary"
            @click="exportSuggestions"
          >
            <span class="material-symbols-outlined">download</span>
            <span>{{ t('purchaseSuggestions.buttons.exportSuggestions') }}</span>
          </button>
          <button
            v-if="isAdmin"
            class="page-action-btn page-action-btn--primary"
            @click="loadAdminData"
          >
            <span class="material-symbols-outlined">tune</span>
            <span>{{ t('purchaseSuggestions.buttons.refreshAdmin') }}</span>
          </button>
        </div>
      </template>
    </PageHeader>

    <div v-if="boardLoading && !board" class="surface-card loading-card">
      <p>{{ t('purchaseSuggestions.loading') }}</p>
    </div>

    <div v-else-if="boardError && !board" class="surface-card loading-card">
      <p>{{ boardError }}</p>
    </div>

    <template v-else>
      <section class="hero-grid">
        <div class="wishlist-summary surface-card">
          <div class="section-heading">
            <p class="section-kicker">{{ t('purchaseSuggestions.summary.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.summary.title') }}</h2>
          </div>
          <div class="summary-grid">
            <article class="summary-tile">
              <span class="summary-label">{{ t('purchaseSuggestions.summary.totalRequests') }}</span>
              <strong class="summary-value">{{ board?.totalRequests ?? 0 }}</strong>
            </article>
            <article class="summary-tile summary-tile--priority">
              <span class="summary-label">{{ t('purchaseSuggestions.summary.priorityPool') }}</span>
              <strong class="summary-value">{{ board?.priorityPoolCount ?? 0 }}</strong>
            </article>
            <article class="summary-tile summary-tile--planned">
              <span class="summary-label">{{ t('purchaseSuggestions.summary.planned') }}</span>
              <strong class="summary-value">{{ (board?.plannedCount ?? 0) + (board?.purchasingCount ?? 0) }}</strong>
            </article>
            <article class="summary-tile summary-tile--arrived">
              <span class="summary-label">{{ t('purchaseSuggestions.summary.arrived') }}</span>
              <strong class="summary-value">{{ board?.arrivedCount ?? 0 }}</strong>
            </article>
            <article class="summary-tile summary-tile--support">
              <span class="summary-label">{{ t('purchaseSuggestions.summary.totalSupport') }}</span>
              <strong class="summary-value">{{ board?.totalSupportCount ?? 0 }}</strong>
            </article>
          </div>
        </div>

        <div class="process-card surface-card">
          <div class=”section-heading”>
            <p class=”section-kicker”>{{ t('purchaseSuggestions.process.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.process.title') }}</h2>
          </div>
          <ol class=”process-list”>
            <li>{{ t('purchaseSuggestions.process.step1') }}</li>
            <li>{{ t('purchaseSuggestions.process.step2') }}</li>
            <li>{{ t('purchaseSuggestions.process.step3') }}</li>
            <li>{{ t('purchaseSuggestions.process.step4') }}</li>
          </ol>
        </div>
      </section>

      <section class="request-grid">
        <div class="request-form-card surface-card">
          <div class="section-heading">
            <p class="section-kicker">{{ t('purchaseSuggestions.form.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.form.title') }}</h2>
          </div>
          <form class="request-form" @submit.prevent="submitRequest">
            <label class="field">
              <span class="field-label">{{ t('purchaseSuggestions.form.bookTitle') }}</span>
              <input v-model.trim="requestForm.title" type="text" maxlength="200" :placeholder="t('purchaseSuggestions.form.bookTitlePlaceholder')" />
            </label>
            <label class="field">
              <span class="field-label">{{ t('purchaseSuggestions.form.author') }}</span>
              <input v-model.trim="requestForm.author" type="text" maxlength="100" :placeholder="t('purchaseSuggestions.form.authorPlaceholder')" />
            </label>
            <label class="field">
              <span class="field-label">{{ t('purchaseSuggestions.form.isbn') }}</span>
              <input v-model.trim="requestForm.isbn" type="text" maxlength="32" :placeholder="t('purchaseSuggestions.form.isbnPlaceholder')" />
            </label>
            <label class="field">
              <span class="field-label">{{ t('purchaseSuggestions.form.reason') }}</span>
              <textarea
                v-model.trim="requestForm.reason"
                rows="4"
                maxlength="500"
                :placeholder="t('purchaseSuggestions.form.reasonPlaceholder')"
              />
            </label>
            <button class="submit-btn" type="submit" :disabled="submitting">
              <span class="material-symbols-outlined">library_add</span>
              <span>{{ submitting ? t('purchaseSuggestions.form.submitting') : t('purchaseSuggestions.form.submit') }}</span>
            </button>
          </form>
        </div>

        <div class="request-tips-card surface-card">
          <div class="section-heading">
            <p class="section-kicker">{{ t('purchaseSuggestions.tips.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.tips.title') }}</h2>
          </div>
          <ul class="tips-list">
            <li>{{ t('purchaseSuggestions.tips.tip1') }}</li>
            <li>{{ t('purchaseSuggestions.tips.tip2') }}</li>
            <li>{{ t('purchaseSuggestions.tips.tip3') }}</li>
          </ul>
        </div>
      </section>

      <div v-if="feedback" :class="['feedback-banner', `feedback-banner--${feedback.tone}`]">
        <span class="material-symbols-outlined">{{ feedbackIcon(feedback.tone) }}</span>
        <span>{{ feedback.message }}</span>
      </div>

      <section class="request-list-card surface-card">
        <div class="section-heading section-heading--row">
          <div>
            <p class="section-kicker">{{ t('purchaseSuggestions.list.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.list.title') }}</h2>
          </div>
          <p class="section-note">{{ t('purchaseSuggestions.list.note') }}</p>
        </div>

        <div v-if="!board?.requests?.length" class="empty-state">
          <span class="material-symbols-outlined">menu_book</span>
          <p>{{ t('purchaseSuggestions.empty.title') }}</p>
          <p class="empty-state__desc">{{ t('purchaseSuggestions.empty.description') }}</p>
        </div>

        <div v-else class="request-list">
          <article
            v-for="request in board.requests"
            :id="`purchase-request-${request.id}`"
            :key="request.id"
            :class="['request-card', `request-card--${statusTone(request.status)}`, { 'request-card--highlight': highlightedRequestId === request.id }]"
          >
            <div class="request-card__header">
              <div>
                <div class="request-card__meta">
                  <span :class="['status-pill', `status-pill--${statusTone(request.status)}`]">{{ request.statusLabel }}</span>
                  <span class="meta-text">{{ t('purchaseSuggestions.card.proposedBy', { name: request.proposerName || t('purchaseSuggestions.card.anonymous') }) }}</span>
                  <span class="meta-text">{{ t('purchaseSuggestions.card.updatedAt') }} {{ formatTimestamp(request.updatedAt) }}</span>
                </div>
                <h3 class="request-card__title">{{ request.title }}</h3>
                <p class="request-card__subtitle">
                  {{ request.author }}
                  <span v-if="request.isbn"> · ISBN {{ request.isbn }}</span>
                </p>
              </div>
              <div class="support-badge">
                <span class="support-badge__value">{{ request.supportCount }}</span>
                <span class="support-badge__label">{{ t('purchaseSuggestions.card.supportCount') }}</span>
              </div>
            </div>

            <div class="progress-panel">
              <div class="progress-panel__copy">
                <span class="progress-label">{{ t('purchaseSuggestions.card.progress') }}</span>
                <strong>{{ request.progressLabel }}</strong>
              </div>
              <div class="progress-track" aria-hidden="true">
                <div class="progress-fill" :style="{ width: `${request.progressPercent}%` }"></div>
              </div>
              <span class="progress-percent">{{ request.progressPercent }}%</span>
            </div>

            <p class="request-reason">{{ request.reason || t('purchaseSuggestions.card.noReason') }}</p>

            <div v-if="request.statusNote" class="status-note">
              <span class="material-symbols-outlined">campaign</span>
              <span>{{ request.statusNote }}</span>
            </div>

            <div class="request-card__footer">
              <button
                class="vote-btn"
                :disabled="!request.canVote || isVoting(request.id)"
                @click="voteForRequest(request.id)"
              >
                <span class="material-symbols-outlined">how_to_vote</span>
                <span>{{ voteButtonText(request) }}</span>
              </button>
              <span class="footer-note">{{ t('purchaseSuggestions.card.createdAt') }} {{ formatFullDate(request.createdAt) }}</span>
            </div>

            <div v-if="isAdmin" class="admin-control">
              <div class="admin-control__fields">
                <label class="field">
                  <span class="field-label">{{ t('purchaseSuggestions.admin.status') }}</span>
                  <select v-model="statusDrafts[request.id].status">
                    <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span class="field-label">{{ t('purchaseSuggestions.admin.note') }}</span>
                  <input
                    v-model.trim="statusDrafts[request.id].note"
                    type="text"
                    maxlength="200"
                    :placeholder="t('purchaseSuggestions.admin.notePlaceholder')"
                  />
                </label>
              </div>
              <button
                class="admin-save-btn"
                :disabled="isSavingStatus(request.id)"
                @click="saveStatus(request.id)"
              >
                <span class="material-symbols-outlined">save</span>
                <span>{{ isSavingStatus(request.id) ? t('purchaseSuggestions.admin.saving') : t('purchaseSuggestions.admin.save') }}</span>
              </button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="isAdmin" class="admin-section surface-card">
        <div class="section-heading section-heading--row">
          <div>
            <p class="section-kicker">{{ t('purchaseSuggestions.adminSection.kicker') }}</p>
            <h2>{{ t('purchaseSuggestions.adminSection.title') }}</h2>
          </div>
          <p class="section-note">{{ t('purchaseSuggestions.adminSection.note') }}</p>
        </div>

        <div v-if="suggestionsLoading && !suggestionSummary" class="empty-state empty-state--compact">
          <span class="material-symbols-outlined">hourglass_top</span>
          <p>{{ t('purchaseSuggestions.adminSection.loading') }}</p>
        </div>

        <div v-else-if="suggestionsError" class="empty-state empty-state--compact">
          <span class="material-symbols-outlined">error</span>
          <p>{{ suggestionsError }}</p>
        </div>

        <template v-else-if="suggestionSummary">
          <div class="admin-summary-grid">
            <article class="admin-summary-card">
              <span class="summary-label">{{ t('purchaseSuggestions.adminSection.totalSuggestions') }}</span>
              <strong class="summary-value">{{ suggestionSummary.totalSuggestions }}</strong>
            </article>
            <article class="admin-summary-card admin-summary-card--high">
              <span class="summary-label">{{ t('purchaseSuggestions.adminSection.highPriority') }}</span>
              <strong class="summary-value">{{ suggestionSummary.highPriority }}</strong>
            </article>
            <article class="admin-summary-card admin-summary-card--medium">
              <span class="summary-label">{{ t('purchaseSuggestions.adminSection.mediumPriority') }}</span>
              <strong class="summary-value">{{ suggestionSummary.mediumPriority }}</strong>
            </article>
            <article class="admin-summary-card admin-summary-card--copies">
              <span class="summary-label">{{ t('purchaseSuggestions.adminSection.additionalCopies') }}</span>
              <strong class="summary-value">{{ suggestionSummary.totalAdditionalCopies }}</strong>
            </article>
            <article class="admin-summary-card admin-summary-card--budget">
              <span class="summary-label">{{ t('purchaseSuggestions.adminSection.estimatedBudget') }}</span>
              <strong class="summary-value">¥{{ suggestionSummary.estimatedBudget.toFixed(0) }}</strong>
            </article>
          </div>

          <div v-if="!suggestionSummary.suggestions.length" class="empty-state empty-state--compact">
            <span class="material-symbols-outlined">inventory_2</span>
            <p>{{ t('purchaseSuggestions.adminSection.noSuggestions') }}</p>
          </div>

          <div v-else class="suggestion-list">
            <article
              v-for="suggestion in suggestionSummary.suggestions"
              :key="suggestion.bookId"
              :class="['suggestion-card', `suggestion-card--${statusTone(suggestion.priority)}`]"
            >
              <div class="suggestion-card__header">
                <div>
                  <span :class="['status-pill', `status-pill--${statusTone(suggestion.priority)}`]">
                    {{ priorityText(suggestion.priority) }}
                  </span>
                  <h3 class="suggestion-title">{{ suggestion.title }}</h3>
                  <p class="suggestion-subtitle">
                    {{ suggestion.author }} · {{ suggestion.category }} · ISBN {{ suggestion.isbn }}
                  </p>
                </div>
                <div class="support-badge support-badge--admin">
                  <span class="support-badge__value">{{ suggestion.score }}</span>
                  <span class="support-badge__label">{{ t('purchaseSuggestions.adminSection.score') }}</span>
                </div>
              </div>

              <div class="suggestion-metrics">
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.currentCopies') }}</span><strong>{{ suggestion.currentCopies }}</strong></div>
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.suggestedCopies') }}</span><strong>{{ suggestion.suggestedCopies }}</strong></div>
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.pendingPurchase') }}</span><strong>+{{ suggestion.additionalCopies }}</strong></div>
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.reservationCount') }}</span><strong>{{ suggestion.reservationCount }}</strong></div>
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.borrowCount') }}</span><strong>{{ suggestion.borrowCount }}</strong></div>
                <div class="metric-item"><span class="metric-label">{{ t('purchaseSuggestions.adminSection.averageWait') }}</span><strong>{{ t('purchaseSuggestions.adminSection.averageWaitDays', { days: suggestion.averageWaitTime }) }}</strong></div>
              </div>

              <p class="suggestion-reason">{{ suggestion.reason }}</p>
            </article>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  purchaseRequestApi,
  type CreatePurchaseRequestPayload,
  type PurchaseRequestBoard,
  type PurchaseRequestItem,
  type PurchaseRequestStatus,
  type UpdatePurchaseRequestStatusPayload,
} from '../api/purchaseRequestApi'
import { statisticsApi, type PurchaseSuggestionSummary } from '../api/statisticsApi'
import PageHeader from '../components/layout/PageHeader.vue'
import { useUserStore } from '../stores/user'
import { sanitizeApiMessage } from '../utils/apiMessage'
import { logger } from '../utils/logger'

type FeedbackTone = 'success' | 'info' | 'warning' | 'error'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const boardLoading = ref(true)
const boardError = ref('')
const board = ref<PurchaseRequestBoard | null>(null)

const suggestionsLoading = ref(false)
const suggestionsError = ref('')
const suggestionSummary = ref<PurchaseSuggestionSummary | null>(null)

const submitting = ref(false)
const votingIds = ref<number[]>([])
const savingStatusIds = ref<number[]>([])
const feedback = ref<{ tone: FeedbackTone; message: string } | null>(null)
const highlightedRequestId = ref<number | null>(null)
const statusDrafts = reactive<Record<number, { status: PurchaseRequestStatus; note: string }>>({})

const requestForm = reactive<CreatePurchaseRequestPayload>({
  title: '',
  author: '',
  isbn: '',
  reason: '',
})

const isAdmin = computed(() => userStore.isAdmin)

const statusOptions = computed<Array<{ value: PurchaseRequestStatus; label: string }>>(() => [
  { value: 'PENDING_REVIEW', label: t('purchaseSuggestions.statusOptions.PENDING_REVIEW') },
  { value: 'PRIORITY_POOL', label: t('purchaseSuggestions.statusOptions.PRIORITY_POOL') },
  { value: 'PLANNED', label: t('purchaseSuggestions.statusOptions.PLANNED') },
  { value: 'PURCHASING', label: t('purchaseSuggestions.statusOptions.PURCHASING') },
  { value: 'ARRIVED', label: t('purchaseSuggestions.statusOptions.ARRIVED') },
  { value: 'REJECTED', label: t('purchaseSuggestions.statusOptions.REJECTED') },
])

let highlightTimer: number | null = null

onMounted(() => {
  loadPage()
})

onUnmounted(() => {
  if (highlightTimer !== null) window.clearTimeout(highlightTimer)
})

async function loadPage() {
  await Promise.all([loadBoard(), isAdmin.value ? loadSuggestions() : Promise.resolve()])
}

async function loadAdminData() {
  await Promise.all([loadBoard(), loadSuggestions()])
}

async function loadBoard() {
  boardLoading.value = true
  boardError.value = ''
  try {
    const response = await purchaseRequestApi.getBoard()
    if (response.data.success) {
      board.value = response.data.data
      syncStatusDrafts()
    } else {
      board.value = null
      boardError.value = response.data.message || t('purchaseSuggestions.errors.loadBoardFailed')
    }
  } catch (error: any) {
    board.value = null
    boardError.value = sanitizeApiMessage(error.response?.data?.message, t('purchaseSuggestions.errors.loadBoardFailed'))
    logger.error(t('purchaseSuggestions.errors.loadBoardFailed'), error)
  } finally {
    boardLoading.value = false
  }
}

async function loadSuggestions() {
  if (!isAdmin.value) {
    suggestionSummary.value = null
    return
  }

  suggestionsLoading.value = true
  suggestionsError.value = ''
  try {
    const response = await statisticsApi.getPurchaseSuggestions()
    if (response.data.success) {
      suggestionSummary.value = response.data.data
    } else {
      suggestionSummary.value = null
      suggestionsError.value = response.data.message || t('purchaseSuggestions.errors.loadSuggestionsFailed')
    }
  } catch (error: any) {
    suggestionSummary.value = null
    suggestionsError.value = sanitizeApiMessage(error.response?.data?.message, t('purchaseSuggestions.errors.loadSuggestionsFailed'))
    logger.error(t('purchaseSuggestions.errors.loadSuggestionsFailed'), error)
  } finally {
    suggestionsLoading.value = false
  }
}

async function submitRequest() {
  if (submitting.value) return
  submitting.value = true
  feedback.value = null

  try {
    const response = await purchaseRequestApi.createRequest({
      title: requestForm.title,
      author: requestForm.author,
      isbn: requestForm.isbn?.trim() || '',
      reason: requestForm.reason?.trim() || '',
    })

    const result = response.data.data
    const message = sanitizeApiMessage(response.data.message, t('purchaseSuggestions.errors.requestProcessed'))

    if (result.created) {
      feedback.value = { tone: 'success', message }
      resetRequestForm()
      await loadBoard()
      await focusRequest(result.request?.id ?? null)
      return
    }

    if (result.conflictType === 'EXISTING_BOOK' && result.existingBookId) {
      feedback.value = { tone: 'info', message }
      await router.push({ name: 'BookDetail', params: { id: result.existingBookId } })
      return
    }

    if (result.conflictType === 'DUPLICATE_REQUEST' && result.existingRequestId) {
      feedback.value = { tone: 'info', message }
      await loadBoard()
      await focusRequest(result.existingRequestId)
      return
    }

    feedback.value = { tone: 'warning', message }
  } catch (error: any) {
    feedback.value = { tone: 'error', message: sanitizeApiMessage(error.response?.data?.message, t('purchaseSuggestions.errors.submitFailed')) }
    logger.error(t('purchaseSuggestions.errors.submitFailed'), error)
  } finally {
    submitting.value = false
  }
}

async function voteForRequest(requestId: number) {
  markPending(votingIds, requestId, true)
  feedback.value = null
  try {
    const response = await purchaseRequestApi.voteRequest(requestId)
    feedback.value = {
      tone: response.data.data.alreadyVoted ? 'info' : 'success',
      message: sanitizeApiMessage(response.data.message, t('purchaseSuggestions.errors.voteProcessed')),
    }
    await loadBoard()
    await focusRequest(requestId)
  } catch (error: any) {
    feedback.value = { tone: 'error', message: sanitizeApiMessage(error.response?.data?.message, t('purchaseSuggestions.errors.voteFailed')) }
    logger.error(t('purchaseSuggestions.errors.voteFailed'), error)
  } finally {
    markPending(votingIds, requestId, false)
  }
}

async function saveStatus(requestId: number) {
  const draft = statusDrafts[requestId]
  if (!draft) return

  markPending(savingStatusIds, requestId, true)
  feedback.value = null
  try {
    const payload: UpdatePurchaseRequestStatusPayload = {
      status: draft.status,
      statusNote: draft.note,
    }
    const response = await purchaseRequestApi.updateStatus(requestId, payload)
    feedback.value = { tone: 'success', message: sanitizeApiMessage(response.data.message, t('purchaseSuggestions.errors.statusUpdated')) }
    await loadBoard()
    await focusRequest(requestId)
  } catch (error: any) {
    feedback.value = { tone: 'error', message: sanitizeApiMessage(error.response?.data?.message, t('purchaseSuggestions.errors.statusUpdateFailed')) }
    logger.error(t('purchaseSuggestions.errors.statusUpdateFailed'), error)
  } finally {
    markPending(savingStatusIds, requestId, false)
  }
}

function syncStatusDrafts() {
  const validIds = new Set<number>()
  for (const request of board.value?.requests ?? []) {
    validIds.add(request.id)
    statusDrafts[request.id] = { status: request.status, note: request.statusNote ?? '' }
  }
  for (const key of Object.keys(statusDrafts)) {
    const numericKey = Number(key)
    if (!validIds.has(numericKey)) delete statusDrafts[numericKey]
  }
}

function resetRequestForm() {
  requestForm.title = ''
  requestForm.author = ''
  requestForm.isbn = ''
  requestForm.reason = ''
}

async function focusRequest(requestId: number | null | undefined) {
  if (!requestId) return
  highlightedRequestId.value = requestId
  await nextTick()
  document.getElementById(`purchase-request-${requestId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  if (highlightTimer !== null) window.clearTimeout(highlightTimer)
  highlightTimer = window.setTimeout(() => {
    highlightedRequestId.value = null
  }, 1800)
}

function markPending(target: { value: number[] }, id: number, active: boolean) {
  if (active) {
    if (!target.value.includes(id)) target.value = [...target.value, id]
    return
  }
  target.value = target.value.filter((value) => value !== id)
}

function isVoting(requestId: number) {
  return votingIds.value.includes(requestId)
}

function isSavingStatus(requestId: number) {
  return savingStatusIds.value.includes(requestId)
}

function voteButtonText(request: PurchaseRequestItem) {
  if (request.votedByCurrentUser) return t('purchaseSuggestions.voteButton.voted')
  if (!request.canVote) {
    if (request.status === 'PURCHASING') return t('purchaseSuggestions.voteButton.purchasing')
    if (request.status === 'ARRIVED') return t('purchaseSuggestions.voteButton.arrived')
    if (request.status === 'REJECTED') return t('purchaseSuggestions.voteButton.ended')
    return t('purchaseSuggestions.voteButton.unavailable')
  }
  return isVoting(request.id) ? t('purchaseSuggestions.voteButton.submitting') : t('purchaseSuggestions.voteButton.support')
}

function statusTone(status: PurchaseRequestStatus | string) {
  switch (status) {
    case 'PRIORITY_POOL':
    case 'HIGH':
      return 'priority'
    case 'PLANNED':
    case 'PURCHASING':
      return 'planned'
    case 'ARRIVED':
    case 'LOW':
      return 'arrived'
    case 'REJECTED':
      return 'rejected'
    case 'MEDIUM':
      return 'medium'
    default:
      return 'pending'
  }
}

function priorityText(priority: string) {
  if (priority === 'HIGH') return t('purchaseSuggestions.priorityText.high')
  if (priority === 'MEDIUM') return t('purchaseSuggestions.priorityText.medium')
  if (priority === 'LOW') return t('purchaseSuggestions.priorityText.low')
  return priority
}

function feedbackIcon(tone: FeedbackTone) {
  if (tone === 'success') return 'check_circle'
  if (tone === 'info') return 'info'
  if (tone === 'warning') return 'warning'
  return 'error'
}

function formatTimestamp(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

function formatFullDate(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
}

function exportSuggestions() {
  if (!suggestionSummary.value?.suggestions?.length) return

  const escapeCSV = (value: unknown) => {
    if (value === null || value === undefined) return ''
    const content = String(value)
    if (content.includes(',') || content.includes('\n') || content.includes('"')) {
      return `"${content.replace(/"/g, '""')}"`
    }
    return content
  }

  const headers = [
    t('purchaseSuggestions.csvHeaders.title'),
    t('purchaseSuggestions.csvHeaders.author'),
    t('purchaseSuggestions.csvHeaders.isbn'),
    t('purchaseSuggestions.csvHeaders.category'),
    t('purchaseSuggestions.csvHeaders.currentCopies'),
    t('purchaseSuggestions.csvHeaders.suggestedCopies'),
    t('purchaseSuggestions.csvHeaders.additionalCopies'),
    t('purchaseSuggestions.csvHeaders.borrowCount'),
    t('purchaseSuggestions.csvHeaders.reservationCount'),
    t('purchaseSuggestions.csvHeaders.priority'),
    t('purchaseSuggestions.csvHeaders.reason'),
  ]
  const rows = suggestionSummary.value.suggestions.map((item) => [
    escapeCSV(item.title),
    escapeCSV(item.author),
    escapeCSV(item.isbn),
    escapeCSV(item.category),
    escapeCSV(item.currentCopies),
    escapeCSV(item.suggestedCopies),
    escapeCSV(item.additionalCopies),
    escapeCSV(item.borrowCount),
    escapeCSV(item.reservationCount),
    escapeCSV(priorityText(item.priority)),
    escapeCSV(item.reason),
  ])

  const csvContent = [headers, ...rows].map((row) => row.join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${t('purchaseSuggestions.export.fileName')}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}
</script>

<style scoped>
.purchase-hub {
  --hub-ink: #21312b;
  --hub-muted: rgba(50, 65, 58, 0.72);
  --hub-line: rgba(92, 106, 97, 0.18);
  --hub-cream: linear-gradient(180deg, rgba(254, 250, 238, 0.98), rgba(248, 244, 232, 0.96));
  --hub-shadow: 0 18px 40px rgba(47, 55, 50, 0.08);
  --hub-priority: #b85c38;
  --hub-planned: #3c6e71;
  --hub-arrived: #557c55;
  --hub-rejected: #7c4f4f;
  --hub-pending: #8b6f47;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.hero-grid,
.request-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
  gap: var(--space-5);
}

.wishlist-summary,
.process-card,
.request-form-card,
.request-tips-card,
.request-list-card,
.admin-section,
.loading-card {
  background: var(--hub-cream);
  border: 1px solid var(--hub-line);
  box-shadow: var(--hub-shadow);
}

.loading-card {
  padding: var(--space-8);
  text-align: center;
  color: var(--hub-muted);
}

.section-heading {
  margin-bottom: var(--space-5);
}

.section-heading--row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-kicker {
  margin: 0 0 var(--space-2);
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(123, 92, 52, 0.84);
}

.section-heading h2 {
  margin: 0;
  font-size: 1.55rem;
  font-weight: 800;
  color: var(--hub-ink);
}

.section-note {
  margin: 0;
  font-size: 0.92rem;
  color: var(--hub-muted);
}

.summary-grid,
.admin-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: var(--space-4);
}

.summary-tile,
.admin-summary-card {
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(100, 115, 108, 0.14);
}

.summary-tile--priority,
.admin-summary-card--high {
  border-left: 4px solid var(--hub-priority);
}

.summary-tile--planned,
.admin-summary-card--copies {
  border-left: 4px solid var(--hub-planned);
}

.summary-tile--arrived,
.admin-summary-card--budget {
  border-left: 4px solid var(--hub-arrived);
}

.summary-tile--support,
.admin-summary-card--medium {
  border-left: 4px solid var(--hub-pending);
}

.summary-label {
  display: block;
  font-size: 0.82rem;
  color: var(--hub-muted);
}

.summary-value {
  display: block;
  margin-top: var(--space-2);
  font-size: 1.85rem;
  line-height: 1;
  color: var(--hub-ink);
}

.process-list,
.tips-list {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--hub-ink);
  line-height: 1.8;
}

.request-form-card,
.request-tips-card,
.request-list-card,
.admin-section {
  padding: var(--space-6);
}

.request-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.field-label {
  font-size: 0.83rem;
  font-weight: 700;
  color: var(--hub-muted);
}

.field input,
.field textarea,
.field select {
  width: 100%;
  border: 1px solid rgba(95, 110, 101, 0.18);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.94);
  padding: 0.85rem 0.95rem;
  font: inherit;
  color: var(--hub-ink);
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  outline: none;
  border-color: rgba(60, 110, 113, 0.46);
  box-shadow: 0 0 0 4px rgba(60, 110, 113, 0.12);
}

.submit-btn,
.vote-btn,
.admin-save-btn,
.page-action-btn {
  border: none;
  border-radius: 999px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease, box-shadow 0.18s ease;
}

.submit-btn,
.admin-save-btn,
.page-action-btn--primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  background: linear-gradient(135deg, #335c67, #537072);
  color: #fffdf5;
  padding: 0.9rem 1.2rem;
  box-shadow: 0 12px 26px rgba(51, 92, 103, 0.22);
}

.vote-btn,
.page-action-btn--secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  background: rgba(255, 255, 255, 0.9);
  color: var(--hub-ink);
  padding: 0.85rem 1.15rem;
  border: 1px solid rgba(97, 113, 104, 0.18);
}

.submit-btn:disabled,
.vote-btn:disabled,
.admin-save-btn:disabled,
.page-action-btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.submit-btn:not(:disabled):hover,
.vote-btn:not(:disabled):hover,
.admin-save-btn:not(:disabled):hover,
.page-action-btn:not(:disabled):hover {
  transform: translateY(-1px);
}

.page-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.feedback-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.95rem 1.1rem;
  border-radius: var(--radius-lg);
  font-weight: 600;
}

.feedback-banner--success {
  background: rgba(85, 124, 85, 0.12);
  color: #2f5d32;
}

.feedback-banner--info {
  background: rgba(60, 110, 113, 0.12);
  color: #315f63;
}

.feedback-banner--warning {
  background: rgba(184, 92, 56, 0.12);
  color: #8b482f;
}

.feedback-banner--error {
  background: rgba(124, 79, 79, 0.12);
  color: #7c4f4f;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.request-card,
.suggestion-card {
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(96, 111, 104, 0.16);
  padding: var(--space-5);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.request-card:hover,
.suggestion-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(38, 47, 42, 0.08);
}

.request-card--highlight {
  box-shadow: 0 0 0 3px rgba(60, 110, 113, 0.18), 0 16px 34px rgba(38, 47, 42, 0.08);
}

.request-card--priority,
.suggestion-card--priority {
  border-left: 4px solid var(--hub-priority);
}

.request-card--planned,
.suggestion-card--medium {
  border-left: 4px solid var(--hub-planned);
}

.request-card--arrived,
.suggestion-card--arrived {
  border-left: 4px solid var(--hub-arrived);
}

.request-card--rejected {
  border-left: 4px solid var(--hub-rejected);
  opacity: 0.9;
}

.request-card--pending {
  border-left: 4px solid var(--hub-pending);
}

.request-card__header,
.suggestion-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.request-card__meta {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  flex-wrap: wrap;
  margin-bottom: var(--space-2);
}

.meta-text,
.footer-note,
.suggestion-subtitle {
  color: var(--hub-muted);
  font-size: 0.9rem;
}

.request-card__title,
.suggestion-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--hub-ink);
}

.request-card__subtitle {
  margin: var(--space-2) 0 0;
  color: var(--hub-muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.72rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
}

.status-pill--pending {
  background: rgba(139, 111, 71, 0.14);
  color: #7c633e;
}

.status-pill--priority {
  background: rgba(184, 92, 56, 0.14);
  color: #a14d2e;
}

.status-pill--planned,
.status-pill--medium {
  background: rgba(60, 110, 113, 0.14);
  color: #315f63;
}

.status-pill--arrived {
  background: rgba(85, 124, 85, 0.14);
  color: #456846;
}

.status-pill--rejected {
  background: rgba(124, 79, 79, 0.14);
  color: #7c4f4f;
}

.support-badge {
  min-width: 92px;
  padding: 0.9rem 0.75rem;
  border-radius: 1.1rem;
  background: linear-gradient(135deg, rgba(250, 246, 235, 0.98), rgba(242, 237, 223, 0.92));
  border: 1px solid rgba(121, 102, 66, 0.18);
  text-align: center;
}

.support-badge--admin {
  min-width: 108px;
}

.support-badge__value {
  display: block;
  font-size: 1.45rem;
  font-weight: 800;
  color: var(--hub-ink);
}

.support-badge__label {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--hub-muted);
}

.progress-panel {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--space-3);
  align-items: center;
  margin-top: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: rgba(247, 245, 238, 0.92);
}

.progress-panel__copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.progress-label {
  font-size: 0.78rem;
  color: var(--hub-muted);
}

.progress-track {
  width: 100%;
  height: 0.6rem;
  border-radius: 999px;
  background: rgba(116, 129, 120, 0.14);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ad7a42, #537072);
}

.progress-percent {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--hub-muted);
}

.request-reason,
.suggestion-reason {
  margin: var(--space-4) 0 0;
  color: var(--hub-ink);
  line-height: 1.8;
}

.status-note {
  margin-top: var(--space-4);
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.7rem 0.9rem;
  border-radius: var(--radius-md);
  background: rgba(60, 110, 113, 0.08);
  color: #315f63;
}

.request-card__footer {
  margin-top: var(--space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.vote-btn {
  min-width: 10rem;
}

.admin-control {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px dashed rgba(94, 108, 100, 0.2);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
}

.admin-control__fields {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: var(--space-3);
  flex: 1;
}

.admin-save-btn {
  min-width: 8.8rem;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.suggestion-metrics {
  margin-top: var(--space-4);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(124px, 1fr));
  gap: var(--space-3);
}

.metric-item {
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-md);
  background: rgba(247, 245, 238, 0.92);
}

.metric-label {
  display: block;
  font-size: 0.76rem;
  color: var(--hub-muted);
}

.metric-item strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--hub-ink);
}

.empty-state {
  padding: var(--space-8);
  text-align: center;
  color: var(--hub-muted);
}

.empty-state--compact {
  padding: var(--space-6);
}

.empty-state .material-symbols-outlined {
  font-size: 2.4rem;
  color: rgba(92, 110, 101, 0.36);
  margin-bottom: var(--space-3);
}

.empty-state p {
  margin: 0;
}

.empty-state__desc {
  margin-top: var(--space-2) !important;
}

@media (max-width: 1024px) {
  .hero-grid,
  .request-grid {
    grid-template-columns: 1fr;
  }

  .admin-control {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-control__fields {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .section-heading--row,
  .request-card__header,
  .suggestion-card__header,
  .request-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .progress-panel {
    grid-template-columns: 1fr;
  }

  .support-badge,
  .support-badge--admin {
    min-width: 0;
    width: 100%;
  }

  .page-actions {
    width: 100%;
  }

  .page-action-btn {
    width: 100%;
  }
}
</style>
