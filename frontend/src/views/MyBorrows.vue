<template>
  <div class="my-borrows page-stack">
    <PageHeader
      :title="t('myBorrows.title')"
      eyebrow="Reading Ledger"
      :description="t('myBorrows.description')"
    >
      <template #actions>
        <div class="tabs">
          <button :class="{ active: activeTab === 'current' }" type="button" @click="activeTab = 'current'">{{ t('myBorrows.tabs.current') }}</button>
          <button :class="{ active: activeTab === 'history' }" type="button" @click="activeTab = 'history'">{{ t('myBorrows.tabs.history') }}</button>
        </div>
      </template>
    </PageHeader>

    <section v-if="activeTab === 'current'" class="borrow-list">
      <div v-if="currentBorrows.length === 0" class="empty-state">{{ t('myBorrows.empty.current') }}</div>
      <div v-else class="borrow-cards">
        <article
          v-for="(borrow, index) in currentBorrows"
          :key="borrow.id"
          v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
          class="borrow-card"
        >
          <div class="borrow-card-head">
            <div>
              <h3>{{ borrow.bookTitle }}</h3>
              <p class="sub-copy">{{ borrow.location || t('myBorrows.empty.location') }}</p>
            </div>
            <span class="status-chip" :class="`status-chip--${borrow.status.toLowerCase()}`">{{ statusText(borrow.status) }}</span>
          </div>

          <div class="detail-grid">
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.nextStep') }}</span>
              <strong>{{ nextActionText(borrow.nextAction) }}</strong>
            </div>
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.statusHint') }}</span>
              <strong>{{ borrow.statusHint || t('myBorrows.detail.waitingUpdate') }}</strong>
            </div>
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.approvedAt') }}</span>
              <strong>{{ formatDate(borrow.approvedAt) }}</strong>
            </div>
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.pickupDeadline') }}</span>
              <strong>{{ formatDate(borrow.pickupDeadline) }}</strong>
            </div>
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.dueDate') }}</span>
              <strong :class="{ overdue: isOverdue(borrow.dueDate) }">{{ formatDate(borrow.dueDate) }}</strong>
            </div>
            <div class="detail-item">
              <span>{{ t('myBorrows.detail.rejectReason') }}</span>
              <strong>{{ borrow.rejectReason || '-' }}</strong>
            </div>
          </div>

          <div class="action-row">
            <LibraryButton
              v-if="borrow.nextAction === 'PICKUP'"
              type="primary"
              :disabled="isSubmitting"
              @click="confirmAction('pickup', borrow)"
            >
              {{ t('myBorrows.buttons.confirmPickup') }}
            </LibraryButton>
            <LibraryButton
              v-if="borrow.nextAction === 'RETURN_OR_RENEW'"
              type="secondary"
              :disabled="isSubmitting || !canRenew(borrow)"
              @click="confirmAction('renew', borrow)"
            >
              {{ t('myBorrows.buttons.renew') }}
            </LibraryButton>
            <LibraryButton
              v-if="borrow.nextAction === 'RETURN_NOW' || borrow.nextAction === 'RETURN_OR_RENEW'"
              type="primary"
              :disabled="isSubmitting"
              @click="confirmAction('return', borrow)"
            >
              {{ t('myBorrows.buttons.returnBook') }}
            </LibraryButton>
          </div>
        </article>
      </div>
    </section>

    <section v-else v-reveal="{ preset: 'section', once: true }" class="surface-card history-table">
      <div v-if="borrowHistory.length === 0" class="empty-state empty-state--plain">{{ t('myBorrows.empty.history') }}</div>
      <table v-else>
        <thead>
          <tr>
            <th>{{ t('myBorrows.historyTable.book') }}</th>
            <th>{{ t('myBorrows.historyTable.borrowDate') }}</th>
            <th>{{ t('myBorrows.historyTable.dueDate') }}</th>
            <th>{{ t('myBorrows.historyTable.returnDate') }}</th>
            <th>{{ t('myBorrows.historyTable.status') }}</th>
            <th>{{ t('myBorrows.historyTable.fee') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="borrow in borrowHistory" :key="borrow.id">
            <td>{{ borrow.bookTitle }}</td>
            <td>{{ formatDate(borrow.borrowDate) }}</td>
            <td>{{ formatDate(borrow.dueDate) }}</td>
            <td>{{ formatDate(borrow.returnDate) }}</td>
            <td>{{ statusText(borrow.status) }}</td>
            <td>{{ borrow.fineAmount > 0 ? `¥${Number(borrow.fineAmount).toFixed(2)}` : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <ConfirmDialog
      :open="dialog.open"
      :eyebrow="dialog.eyebrow"
      :title="dialog.title"
      :message="dialog.message"
      :confirm-text="dialog.confirmText"
      :cancel-text="dialog.cancelText"
      @cancel="closeDialog"
      @confirm="runAction"
    />
    <FeedbackToast v-if="toast.message" :toast="toast" @close="toast.message = ''" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { borrowApi, type BorrowRecord } from '../api/borrowApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import LibraryButton from '@/components/common/LibraryButton.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { formatLocalDate as formatDate } from '../utils/timeHelpers'
import { useOffline } from '../composables/useOffline'
import { pickupBorrowOfflineAware, returnBookOfflineAware, renewBorrowOfflineAware } from '../composables/useBorrowOffline'

const { t } = useI18n()
const { isOnline, enqueueOperation } = useOffline()

const activeTab = ref<'current' | 'history'>('current')
const currentBorrows = ref<BorrowRecord[]>([])
const borrowHistory = ref<BorrowRecord[]>([])
const isSubmitting = ref(false)

const pendingRecord = ref<BorrowRecord | null>(null)
const { dialog, openDialog, closeDialog: closeDialogBase } = useConfirmDialog()
const { toast, showToast } = useToast()

onMounted(() => {
  void Promise.all([loadCurrentBorrows(), loadBorrowHistory()])
})

async function loadCurrentBorrows() {
  try {
    const response = await borrowApi.getCurrentBorrows()
    currentBorrows.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load current borrows:', error)
    currentBorrows.value = []
  }
}

async function loadBorrowHistory() {
  try {
    const response = await borrowApi.getBorrowHistory()
    borrowHistory.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load borrow history:', error)
    borrowHistory.value = []
  }
}

function confirmAction(action: 'pickup' | 'renew' | 'return', record: BorrowRecord) {
  pendingRecord.value = record
  openDialog({
    eyebrow: action.toUpperCase(),
    title: action === 'pickup' ? t('myBorrows.dialog.confirmPickup') : action === 'renew' ? t('myBorrows.dialog.confirmRenew') : t('myBorrows.dialog.confirmReturn'),
    message: action === 'pickup'
      ? t('myBorrows.dialog.pickupMsg')
      : action === 'renew'
        ? t('myBorrows.dialog.renewMsg')
        : t('myBorrows.dialog.returnMsg'),
    confirmText: action === 'pickup' ? t('myBorrows.dialog.startBorrow') : action === 'renew' ? t('myBorrows.dialog.confirmRenewText') : t('myBorrows.dialog.confirmReturnText'),
    cancelText: t('myBorrows.dialog.cancel'),
    action,
  })
}

function closeDialog() {
  closeDialogBase()
  pendingRecord.value = null
}

async function runAction() {
  if (!pendingRecord.value || !dialog.action) return
  isSubmitting.value = true
  try {
    if (dialog.action === 'pickup') {
      await pickupBorrowOfflineAware({
        recordId: pendingRecord.value.id,
        isOnline,
        enqueueOperation,
      })
      showToast(t('myBorrows.toast.pickupConfirmed'), 'success')
    } else if (dialog.action === 'renew') {
      await renewBorrowOfflineAware({
        recordId: pendingRecord.value.id,
        isOnline,
        enqueueOperation,
      })
      showToast(t('myBorrows.toast.renewed'), 'success')
    } else {
      await returnBookOfflineAware({
        recordId: pendingRecord.value.id,
        isOnline,
        enqueueOperation,
      })
      showToast(t('myBorrows.toast.returned'), 'success')
    }
    closeDialog()
    await Promise.all([loadCurrentBorrows(), loadBorrowHistory()])
  } catch (error: any) {
    showToast(error.response?.data?.message || t('myBorrows.toast.operationFailed'), 'error')
  } finally {
    isSubmitting.value = false
  }
}

function canRenew(borrow: BorrowRecord) {
  return borrow.status === 'BORROWED' && (borrow.renewCount ?? 0) < 1 && !isOverdue(borrow.dueDate)
}

function isOverdue(dueDate?: string | null) {
  if (!dueDate) return false
  const due = new Date(dueDate)
  return !Number.isNaN(due.getTime()) && Date.now() > due.getTime()
}

function statusText(status: string) {
  return t(`myBorrows.status.${status}`) || status
}

function nextActionText(action?: string) {
  return t(`myBorrows.nextAction.${action || ''}`) || '-'
}
</script>

<style scoped>
.tabs {
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.35rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(115, 124, 129, 0.12);
}

.tabs button {
  min-height: 2.7rem;
  padding: 0 1rem;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--outline);
  font-weight: 700;
  cursor: pointer;
}

.tabs button.active {
  color: var(--primary);
  background: rgba(0, 83, 219, 0.1);
}

.borrow-cards {
  display: grid;
  gap: 1rem;
}

.borrow-card {
  padding: 1.35rem;
  border-radius: 1.35rem;
  background: linear-gradient(180deg, rgba(255, 254, 249, 0.98) 0%, rgba(245, 241, 232, 0.95) 100%);
  border: 1px solid rgba(133, 122, 107, 0.12);
  box-shadow: 0 18px 40px rgba(47, 48, 45, 0.08);
}

.borrow-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.borrow-card h3 {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.35rem;
}

.sub-copy {
  margin: 0.35rem 0 0;
  color: var(--on-surface-variant);
}

.status-chip {
  padding: 0.42rem 0.8rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.status-chip--pending { background: rgba(217, 119, 6, 0.12); color: #b45309; }
.status-chip--approved { background: rgba(0, 83, 219, 0.12); color: var(--primary); }
.status-chip--borrowed { background: rgba(34, 84, 61, 0.12); color: #22543d; }
.status-chip--overdue { background: rgba(127, 29, 29, 0.12); color: #991b1b; }
.status-chip--rejected,
.status-chip--returned { background: rgba(115, 124, 129, 0.12); color: var(--outline); }

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.95rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.7);
}

.detail-item span {
  font-size: 0.75rem;
  font-family: var(--font-label);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--outline);
}

.detail-item strong {
  line-height: 1.55;
}

.detail-item strong.overdue {
  color: #991b1b;
}

.action-row {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}

.empty-state {
  padding: 3.2rem 1.5rem;
  text-align: center;
  border-radius: 1.25rem;
  color: var(--on-surface-variant);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-soft);
}

.empty-state--plain {
  padding: 2rem;
  background: transparent;
  box-shadow: none;
}

.history-table table {
  width: 100%;
  border-collapse: collapse;
}

.history-table th,
.history-table td {
  padding: 0.95rem 0.8rem;
  text-align: left;
  border-top: 1px solid rgba(133, 122, 107, 0.12);
}

.history-table th {
  border-top: none;
}

@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .borrow-card-head,
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .history-table {
    overflow-x: auto;
  }
}
</style>
