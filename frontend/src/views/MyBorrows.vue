<template>
  <div class="my-borrows page-stack">
    <PageHeader
      title="我的借阅"
      eyebrow="Reading Ledger"
      description="待审批、待取书、借阅中、逾期和历史记录都放在同一页，读者可以直接看到为什么卡住、下一步做什么。"
    >
      <template #actions>
        <div class="tabs">
          <button :class="{ active: activeTab === 'current' }" type="button" @click="activeTab = 'current'">当前借阅</button>
          <button :class="{ active: activeTab === 'history' }" type="button" @click="activeTab = 'history'">历史记录</button>
        </div>
      </template>
    </PageHeader>

    <section v-if="activeTab === 'current'" class="borrow-list">
      <div v-if="currentBorrows.length === 0" class="empty-state">当前没有进行中的借阅记录。</div>
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
              <p class="sub-copy">{{ borrow.location || '馆藏位置待补充' }}</p>
            </div>
            <span class="status-chip" :class="`status-chip--${borrow.status.toLowerCase()}`">{{ statusText(borrow.status) }}</span>
          </div>

          <div class="detail-grid">
            <div class="detail-item">
              <span>下一步</span>
              <strong>{{ nextActionText(borrow.nextAction) }}</strong>
            </div>
            <div class="detail-item">
              <span>状态说明</span>
              <strong>{{ borrow.statusHint || '等待状态更新' }}</strong>
            </div>
            <div class="detail-item">
              <span>审批时间</span>
              <strong>{{ formatDate(borrow.approvedAt) }}</strong>
            </div>
            <div class="detail-item">
              <span>取书时限</span>
              <strong>{{ formatDate(borrow.pickupDeadline) }}</strong>
            </div>
            <div class="detail-item">
              <span>到期时间</span>
              <strong :class="{ overdue: isOverdue(borrow.dueDate) }">{{ formatDate(borrow.dueDate) }}</strong>
            </div>
            <div class="detail-item">
              <span>拒绝原因</span>
              <strong>{{ borrow.rejectReason || '-' }}</strong>
            </div>
          </div>

          <div class="action-row">
            <button
              v-if="borrow.nextAction === 'PICKUP'"
              class="page-action-btn page-action-btn--primary"
              type="button"
              :disabled="isSubmitting"
              @click="confirmAction('pickup', borrow)"
            >
              确认取书
            </button>
            <button
              v-if="borrow.nextAction === 'RETURN_OR_RENEW'"
              class="page-action-btn page-action-btn--secondary"
              type="button"
              :disabled="isSubmitting || !canRenew(borrow)"
              @click="confirmAction('renew', borrow)"
            >
              续借
            </button>
            <button
              v-if="borrow.nextAction === 'RETURN_NOW' || borrow.nextAction === 'RETURN_OR_RENEW'"
              class="page-action-btn page-action-btn--primary"
              type="button"
              :disabled="isSubmitting"
              @click="confirmAction('return', borrow)"
            >
              归还图书
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else v-reveal="{ preset: 'section', once: true }" class="surface-card history-table">
      <div v-if="borrowHistory.length === 0" class="empty-state empty-state--plain">还没有历史借阅记录。</div>
      <table v-else>
        <thead>
          <tr>
            <th>图书</th>
            <th>借出</th>
            <th>到期</th>
            <th>归还</th>
            <th>状态</th>
            <th>费用</th>
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
    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { borrowApi, type BorrowRecord } from '../api/borrowApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'

const activeTab = ref<'current' | 'history'>('current')
const currentBorrows = ref<BorrowRecord[]>([])
const borrowHistory = ref<BorrowRecord[]>([])
const isSubmitting = ref(false)

const dialog = reactive({
  open: false,
  action: '' as 'pickup' | 'renew' | 'return' | '',
  record: null as BorrowRecord | null,
  eyebrow: '',
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
})

const toast = reactive<{ message: string; type: 'success' | 'error' | 'info' }>({
  message: '',
  type: 'info',
})

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
  dialog.open = true
  dialog.action = action
  dialog.record = record
  dialog.eyebrow = action.toUpperCase()
  dialog.title = action === 'pickup' ? '确认取书' : action === 'renew' ? '确认续借' : '确认归还'
  dialog.message = action === 'pickup'
    ? '确认后，这本书会进入正式借阅状态，并开始计算到期时间。'
    : action === 'renew'
      ? '续借会直接更新到期时间。如果后面有人排队，这里会阻止续借。'
      : '归还后，系统会释放副本并继续推动预约队列。'
  dialog.confirmText = action === 'pickup' ? '开始借阅' : action === 'renew' ? '确认续借' : '确认归还'
  dialog.cancelText = '取消'
}

function closeDialog() {
  dialog.open = false
  dialog.action = ''
  dialog.record = null
}

async function runAction() {
  if (!dialog.record || !dialog.action) return
  isSubmitting.value = true
  try {
    if (dialog.action === 'pickup') {
      const response = await borrowApi.pickupBorrow(dialog.record.id)
      showToast(response.data.message || '已确认取书。', 'success')
    } else if (dialog.action === 'renew') {
      const response = await borrowApi.renewBorrow(dialog.record.id)
      showToast(response.data.message || '已完成续借。', 'success')
    } else {
      const response = await borrowApi.returnBook(dialog.record.id)
      showToast(response.data.message || '已归还图书。', 'success')
    }
    closeDialog()
    await Promise.all([loadCurrentBorrows(), loadBorrowHistory()])
  } catch (error: any) {
    showToast(error.response?.data?.message || '操作失败。', 'error')
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

function formatDate(dateStr?: string | null) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function statusText(status: string) {
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

function nextActionText(action?: string) {
  const map: Record<string, string> = {
    WAIT_APPROVAL: '等待管理员审核',
    PICKUP: '到馆取书',
    RETURN_OR_RENEW: '可续借或归还',
    RETURN_NOW: '尽快归还',
    CONTACT_LIBRARY: '联系馆员',
    BROWSE_MORE: '可以继续借阅其它图书',
  }
  return map[action || ''] || '-'
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
