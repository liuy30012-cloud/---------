<template>
  <div class="my-reservations page-stack">
    <PageHeader
      title="我的预约"
      eyebrow="Reservation Queue"
      description="排队人数、预计等待时间、到馆通知与取书时限都对读者透明展示，不再只剩一个模糊状态。"
    />

    <div v-if="reservations.length === 0" class="empty-state">当前没有预约记录。</div>

    <div v-else class="reservation-list">
      <article
        v-for="(reservation, index) in reservations"
        :key="reservation.id"
        v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
        class="reservation-card"
      >
        <div class="card-head">
          <div>
            <h3>{{ reservation.bookTitle }}</h3>
            <p>{{ reservation.location || '馆藏位置待补充' }}</p>
          </div>
          <span class="status-chip" :class="`status-chip--${reservation.status.toLowerCase()}`">{{ statusText(reservation.status) }}</span>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span>队列前方</span>
            <strong>{{ reservation.queueAhead ?? Math.max((reservation.queuePosition || 1) - 1, 0) }}</strong>
          </div>
          <div class="detail-item">
            <span>预计等待</span>
            <strong>{{ reservation.estimatedWaitDays ? `${reservation.estimatedWaitDays} 天` : '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>通知时间</span>
            <strong>{{ formatDate(reservation.notifyDate) }}</strong>
          </div>
          <div class="detail-item">
            <span>取书时限</span>
            <strong>{{ formatDate(reservation.pickupDeadline || reservation.expireDate) }}</strong>
          </div>
          <div v-if="reservation.status === 'AVAILABLE' && reservation.daysUntilExpiry !== undefined" class="detail-item">
            <span>距离过期</span>
            <strong :class="{ 'text-warning': reservation.isExpiringSoon }">
              {{ reservation.daysUntilExpiry }} 天
            </strong>
          </div>
          <div v-if="reservation.extensionCount && reservation.extensionCount > 0" class="detail-item">
            <span>已延期</span>
            <strong>{{ reservation.extensionCount }} 次</strong>
          </div>
          <div class="detail-item detail-item--wide">
            <span>状态说明</span>
            <strong>{{ reservation.statusHint || '等待系统更新' }}</strong>
          </div>
        </div>

        <div v-if="reservation.isExpiringSoon" class="expiry-warning">
          <span class="warning-icon">⚠️</span>
          <span>取书时限即将到期,请尽快取书或申请延期!</span>
        </div>

        <div class="action-row">
          <button
            v-if="reservation.status === 'WAITING'"
            class="page-action-btn page-action-btn--secondary"
            type="button"
            :disabled="isSubmitting"
            @click="openDialog('cancel', reservation)"
          >
            取消预约
          </button>
          <button
            v-if="reservation.status === 'AVAILABLE'"
            class="page-action-btn page-action-btn--primary"
            type="button"
            :disabled="isSubmitting"
            @click="openDialog('pickup', reservation)"
          >
            现在取书
          </button>
          <button
            v-if="reservation.status === 'AVAILABLE' && reservation.canExtend"
            class="page-action-btn page-action-btn--secondary"
            type="button"
            :disabled="isSubmitting"
            @click="openDialog('extend', reservation)"
          >
            延长时限
          </button>
        </div>
      </article>
    </div>

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
import { useRouter } from 'vue-router'
import { reservationApi, type ReservationRecord } from '../api/borrowApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'

const router = useRouter()
const reservations = ref<ReservationRecord[]>([])
const isSubmitting = ref(false)

const dialog = reactive({
  open: false,
  action: '' as 'cancel' | 'pickup' | 'extend' | '',
  reservation: null as ReservationRecord | null,
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
  void loadReservations()
})

async function loadReservations() {
  try {
    const response = await reservationApi.getReservations()
    reservations.value = response.data.success ? response.data.data : []
  } catch (error) {
    logger.error('Failed to load reservations:', error)
    reservations.value = []
  }
}

function openDialog(action: 'cancel' | 'pickup' | 'extend', reservation: ReservationRecord) {
  dialog.open = true
  dialog.action = action
  dialog.reservation = reservation

  if (action === 'extend') {
    dialog.eyebrow = 'Extend'
    dialog.title = '延长取书时限'
    dialog.message = '延长后，取书时限将增加2天。每个预约只能延长1次。'
    dialog.confirmText = '确认延长'
    dialog.cancelText = '取消'
  } else {
    dialog.eyebrow = action === 'pickup' ? 'Pickup' : 'Cancel'
    dialog.title = action === 'pickup' ? '确认取走预约图书' : '确认取消预约'
    dialog.message = action === 'pickup'
      ? '确认后，这条预约会转为正式借阅记录，并在”我的借阅”里继续跟踪。'
      : '取消后，系统会立即把队列位置让给下一位读者。'
    dialog.confirmText = action === 'pickup' ? '确认取书' : '确认取消'
    dialog.cancelText = '返回'
  }
}

function closeDialog() {
  dialog.open = false
  dialog.action = ''
  dialog.reservation = null
}

async function runAction() {
  if (!dialog.reservation || !dialog.action) return
  isSubmitting.value = true
  try {
    if (dialog.action === 'cancel') {
      const response = await reservationApi.cancelReservation(dialog.reservation.id)
      showToast(response.data.message || '预约已取消。', 'success')
      await loadReservations()
    } else if (dialog.action === 'pickup') {
      const response = await reservationApi.pickupReservation(dialog.reservation.id)
      showToast(response.data.message || '预约已转为借阅。', 'success')
      await router.push({ name: 'MyBorrows' })
    } else if (dialog.action === 'extend') {
      const response = await reservationApi.extendReservation(dialog.reservation.id)
      showToast(response.data.message || '取书时限已延长。', 'success')
      await loadReservations()
    }
    closeDialog()
  } catch (error: any) {
    showToast(error.response?.data?.message || '操作失败。', 'error')
  } finally {
    isSubmitting.value = false
  }
}

function statusText(status: string) {
  const map: Record<string, string> = {
    WAITING: '排队中',
    AVAILABLE: '可取书',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    EXPIRED: '已过期',
  }
  return map[status] || status
}

function formatDate(dateStr?: string | null) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
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
.empty-state {
  padding: 3.2rem 1.5rem;
  text-align: center;
  border-radius: 1.25rem;
  color: var(--on-surface-variant);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-soft);
}

.reservation-list {
  display: grid;
  gap: 1rem;
}

.reservation-card {
  padding: 1.35rem;
  border-radius: 1.35rem;
  background: linear-gradient(180deg, rgba(255, 254, 249, 0.98) 0%, rgba(245, 241, 232, 0.95) 100%);
  border: 1px solid rgba(133, 122, 107, 0.12);
  box-shadow: 0 18px 40px rgba(47, 48, 45, 0.08);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.card-head h3 {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.35rem;
}

.card-head p {
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

.status-chip--waiting { background: rgba(217, 119, 6, 0.12); color: #b45309; }
.status-chip--available { background: rgba(34, 84, 61, 0.12); color: #22543d; }
.status-chip--completed { background: rgba(0, 83, 219, 0.12); color: var(--primary); }
.status-chip--cancelled,
.status-chip--expired { background: rgba(115, 124, 129, 0.12); color: var(--outline); }

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.detail-item--wide {
  grid-column: span 2;
}

.detail-item span {
  font-size: 0.75rem;
  font-family: var(--font-label);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--outline);
}

.action-row {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}

.text-warning {
  color: #d97706;
}

.expiry-warning {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-top: 0.75rem;
  border-radius: 0.75rem;
  background: rgba(217, 119, 6, 0.1);
  border: 1px solid rgba(217, 119, 6, 0.3);
  color: #b45309;
  font-size: 0.875rem;
}

.warning-icon {
  font-size: 1.25rem;
}

@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-item--wide {
    grid-column: span 2;
  }
}

@media (max-width: 767px) {
  .card-head,
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item--wide {
    grid-column: span 1;
  }
}
</style>
