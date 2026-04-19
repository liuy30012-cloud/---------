<template>
  <div class="my-reservations page-stack">
    <PageHeader
      :title="t('myReservations.title')"
      eyebrow="Reservation Queue"
      :description="t('myReservations.description')"
    />

    <div v-if="reservations.length === 0" class="empty-state">{{ t('myReservations.empty') }}</div>

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
            <p>{{ reservation.location || t('myReservations.location') }}</p>
          </div>
          <span class="status-chip" :class="`status-chip--${reservation.status.toLowerCase()}`">{{ statusText(reservation.status) }}</span>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span>{{ t('myReservations.detail.queueAhead') }}</span>
            <strong>{{ reservation.queueAhead ?? Math.max((reservation.queuePosition || 1) - 1, 0) }}</strong>
          </div>
          <div class="detail-item">
            <span>{{ t('myReservations.detail.estimatedWait') }}</span>
            <strong>{{ reservation.estimatedWaitDays ? t('myReservations.detail.daysUnit', { days: reservation.estimatedWaitDays }) : '-' }}</strong>
          </div>
          <div class="detail-item">
            <span>{{ t('myReservations.detail.notifyDate') }}</span>
            <strong>{{ formatDate(reservation.notifyDate) }}</strong>
          </div>
          <div class="detail-item">
            <span>{{ t('myReservations.detail.pickupDeadline') }}</span>
            <strong>{{ formatDate(reservation.pickupDeadline || reservation.expireDate) }}</strong>
          </div>
          <div v-if="reservation.status === 'AVAILABLE' && reservation.daysUntilExpiry !== undefined" class="detail-item">
            <span>{{ t('myReservations.detail.daysUntilExpiry') }}</span>
            <strong :class="{ 'text-warning': reservation.isExpiringSoon }">
              {{ reservation.daysUntilExpiry }} 天
            </strong>
          </div>
          <div v-if="reservation.extensionCount && reservation.extensionCount > 0" class="detail-item">
            <span>{{ t('myReservations.detail.extensionsCount') }}</span>
            <strong>{{ reservation.extensionCount }} 次</strong>
          </div>
          <div class="detail-item detail-item--wide">
            <span>{{ t('myReservations.detail.statusHint') }}</span>
            <strong>{{ reservation.statusHint || t('myReservations.detail.waitingUpdate') }}</strong>
          </div>
        </div>

        <div v-if="reservation.isExpiringSoon" class="expiry-warning">
          <span class="warning-icon">⚠️</span>
          <span>{{ t('myReservations.expiryWarning') }}</span>
        </div>

        <div class="action-row">
          <LibraryButton
            v-if="reservation.status === 'WAITING'"
            type="secondary"
            :disabled="isSubmitting"
            @click="openDialog('cancel', reservation)"
          >
            {{ t('myReservations.buttons.cancel') }}
          </LibraryButton>
          <LibraryButton
            v-if="reservation.status === 'AVAILABLE'"
            type="primary"
            :disabled="isSubmitting"
            @click="openDialog('pickup', reservation)"
          >
            {{ t('myReservations.buttons.pickup') }}
          </LibraryButton>
          <LibraryButton
            v-if="reservation.status === 'AVAILABLE' && reservation.canExtend"
            type="secondary"
            :disabled="isSubmitting"
            @click="openDialog('extend', reservation)"
          >
            {{ t('myReservations.buttons.extend') }}
          </LibraryButton>
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
import { useI18n } from 'vue-i18n'
import { reservationApi, type ReservationRecord } from '../api/borrowApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import LibraryButton from '@/components/common/LibraryButton.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'
import { useToast } from '../composables/useToast'

const router = useRouter()
const { t } = useI18n()
const { toast, showToast } = useToast()

const reservations = ref<ReservationRecord[]>([])
const isSubmitting = ref(false)

const dialog = reactive({
  open: false,
  action: '' as 'cancel' | 'pickup' | 'extend' | '',
  reservation: null as ReservationRecord | null,
  eyebrow: '',
  title: '',
  message: '',
  confirmText: '',
  cancelText: '',
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
    dialog.title = t('myReservations.dialog.extendTitle')
    dialog.message = t('myReservations.dialog.extendMsg')
    dialog.confirmText = t('myReservations.dialog.confirmExtend')
    dialog.cancelText = t('myReservations.dialog.cancel')
  } else {
    dialog.eyebrow = action === 'pickup' ? 'Pickup' : 'Cancel'
    dialog.title = action === 'pickup' ? t('myReservations.dialog.pickupTitle') : t('myReservations.dialog.cancelTitle')
    dialog.message = action === 'pickup'
      ? t('myReservations.dialog.pickupMsg')
      : t('myReservations.dialog.cancelMsg')
    dialog.confirmText = action === 'pickup' ? t('myReservations.dialog.confirmPickup') : t('myReservations.dialog.confirmCancel')
    dialog.cancelText = t('myReservations.dialog.back')
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
      showToast(response.data.message || t('myReservations.toast.cancelled'), 'success')
      await loadReservations()
    } else if (dialog.action === 'pickup') {
      const response = await reservationApi.pickupReservation(dialog.reservation.id)
      showToast(response.data.message || t('myReservations.toast.convertedToBorrow'), 'success')
      await router.push({ name: 'MyBorrows' })
    } else if (dialog.action === 'extend') {
      const response = await reservationApi.extendReservation(dialog.reservation.id)
      showToast(response.data.message || t('myReservations.toast.extended'), 'success')
      await loadReservations()
    }
    closeDialog()
  } catch (error: any) {
    showToast(error.response?.data?.message || t('myReservations.toast.operationFailed'), 'error')
  } finally {
    isSubmitting.value = false
  }
}

function statusText(status: string) {
  return t(`myReservations.status.${status}`) || status
}

function formatDate(dateStr?: string | null) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
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
