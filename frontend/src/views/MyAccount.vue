<template>
  <div class="my-account page-stack">
    <PageHeader
      :title="t('myAccount.title')"
      eyebrow="Account"
      :description="t('myAccount.description')"
    />

    <section class="profile-grid">
      <article v-reveal="{ preset: 'card', delay: 0.02, once: true }" class="surface-card profile-card">
        <div class="profile-head">
          <div class="profile-avatar">{{ userStore.user?.username?.charAt(0) || 'U' }}</div>
          <div>
            <h2>{{ userStore.user?.username }}</h2>
            <p>{{ userStore.user?.studentId }}</p>
          </div>
        </div>
        <div class="profile-meta">
          <div>
            <span>{{ t('myAccount.profile.email') }}</span>
            <strong>{{ userStore.user?.email || t('myAccount.profile.notFilled') }}</strong>
          </div>
          <div>
            <span>{{ t('myAccount.profile.phone') }}</span>
            <strong>{{ userStore.user?.phone || t('myAccount.profile.notFilled') }}</strong>
          </div>
          <div>
            <span>{{ t('myAccount.profile.role') }}</span>
            <strong>{{ userStore.user?.role || 'STUDENT' }}</strong>
          </div>
        </div>
      </article>

      <article v-reveal="{ preset: 'card', delay: 0.1, once: true }" class="surface-card profile-card">
        <h2>{{ t('myAccount.borrowProfile.title') }}</h2>
        <div v-if="profile" class="profile-stats">
          <div class="stat-box">
            <span>{{ t('myAccount.borrowProfile.totalBorrows') }}</span>
            <strong>{{ profile.totalBorrows }}</strong>
          </div>
          <div class="stat-box">
            <span>{{ t('myAccount.borrowProfile.currentBorrows') }}</span>
            <strong>{{ profile.currentBorrows }}</strong>
          </div>
          <div class="stat-box">
            <span>{{ t('myAccount.borrowProfile.favoriteCategory') }}</span>
            <strong>{{ profile.favoriteCategory || t('myAccount.borrowProfile.pending') }}</strong>
          </div>
          <div class="stat-box">
            <span>{{ t('myAccount.borrowProfile.averageBorrowDays') }}</span>
            <strong>{{ profile.averageBorrowDays?.toFixed(1) || '0.0' }}</strong>
          </div>
        </div>
        <p v-else class="muted-copy">{{ t('myAccount.borrowProfile.loadFailed') }}</p>
      </article>
    </section>

    <section class="account-grid">
      <article v-reveal="{ preset: 'section', delay: 0.06, once: true }" class="surface-card">
        <h2>{{ t('myAccount.password.title') }}</h2>
        <div class="form-stack">
          <label>
            <span>{{ t('myAccount.password.oldPassword') }}</span>
            <input v-model="passwordForm.oldPassword" type="password" />
          </label>
          <label>
            <span>{{ t('myAccount.password.newPassword') }}</span>
            <input v-model="passwordForm.newPassword" type="password" />
          </label>
          <label>
            <span>{{ t('myAccount.password.confirmPassword') }}</span>
            <input v-model="passwordForm.confirmPassword" type="password" />
          </label>
          <LibraryButton type="primary" :loading="isSubmitting" @click="changePassword">
            {{ t('myAccount.password.update') }}
          </LibraryButton>
        </div>
      </article>

      <article v-reveal="{ preset: 'section', delay: 0.14, once: true }" class="surface-card">
        <h2>{{ t('myAccount.preferences.title') }}</h2>
        <div class="preference-list">
          <label class="preference-item">
            <div>
              <strong>{{ t('myAccount.preferences.borrowUpdates') }}</strong>
              <p>{{ t('myAccount.preferences.borrowUpdatesDesc') }}</p>
            </div>
            <input v-model="preferences.borrowUpdates" type="checkbox" />
          </label>
          <label class="preference-item">
            <div>
              <strong>{{ t('myAccount.preferences.reservationUpdates') }}</strong>
              <p>{{ t('myAccount.preferences.reservationUpdatesDesc') }}</p>
            </div>
            <input v-model="preferences.reservationUpdates" type="checkbox" />
          </label>
          <label class="preference-item">
            <div>
              <strong>{{ t('myAccount.preferences.dueReminders') }}</strong>
              <p>{{ t('myAccount.preferences.dueRemindersDesc') }}</p>
            </div>
            <input v-model="preferences.dueReminders" type="checkbox" />
          </label>
          <LibraryButton type="secondary" @click="savePreferences">
            {{ t('myAccount.preferences.save') }}
          </LibraryButton>
        </div>
      </article>
    </section>

    <section class="account-grid">
      <article v-reveal="{ preset: 'section', delay: 0.22, once: true }" class="surface-card">
        <h2>{{ t('myAccount.export.title') }}</h2>
        <p class="muted-copy">{{ t('myAccount.export.description') }}</p>
        <div class="export-actions">
          <LibraryButton type="secondary" :loading="isExporting" @click="exportBorrowHistory('excel')">
            {{ t('myAccount.export.borrowHistoryExcel') }}
          </LibraryButton>
          <LibraryButton type="secondary" :loading="isExporting" @click="exportBorrowHistory('json')">
            {{ t('myAccount.export.borrowHistoryJson') }}
          </LibraryButton>
          <LibraryButton type="secondary" :loading="isExporting" @click="exportBookReviews">
            {{ t('myAccount.export.reviews') }}
          </LibraryButton>
          <LibraryButton type="primary" :loading="isExporting" @click="exportAllData">
            {{ t('myAccount.export.allData') }}
          </LibraryButton>
        </div>
      </article>
    </section>

    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LibraryButton from '@/components/common/LibraryButton.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { statisticsApi, type UserProfile } from '../api/statisticsApi'
import { exportApi } from '../api/exportApi'
import { useUserStore } from '../stores/user'
import { logger } from '../utils/logger'
import { useToast } from '../composables/useToast'
import { downloadFile, getTimestamp } from '../utils/downloadHelpers'

const { t } = useI18n()
const userStore = useUserStore()
const profile = ref<UserProfile | null>(null)
const isSubmitting = ref(false)
const isExporting = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const preferences = reactive(loadPreferences())
const { toast, showToast } = useToast()

onMounted(() => {
  void loadProfile()
})

async function loadProfile() {
  try {
    const response = await statisticsApi.getUserProfile()
    profile.value = response.data.success ? response.data.data : null
  } catch (error) {
    logger.error('Failed to load user profile:', error)
    profile.value = null
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    showToast(t('myAccount.toast.passwordIncomplete'), 'error')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showToast(t('myAccount.toast.passwordMismatch'), 'error')
    return
  }

  isSubmitting.value = true
  try {
    const result = await userStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    if (result.success) {
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      showToast(result.message, 'success')
    } else {
      showToast(result.message, 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}

function savePreferences() {
  window.localStorage.setItem('reader-account-preferences', JSON.stringify(preferences))
  showToast(t('myAccount.toast.preferencesSaved'), 'success')
}

function loadPreferences() {
  const fallback = {
    borrowUpdates: true,
    reservationUpdates: true,
    dueReminders: true,
  }

  try {
    const raw = window.localStorage.getItem('reader-account-preferences')
    return raw ? { ...fallback, ...JSON.parse(raw) } : fallback
  } catch {
    return fallback
  }
}

async function exportBorrowHistory(format: 'excel' | 'json') {
  isExporting.value = true
  try {
    const response = await exportApi.exportBorrowHistory(format)
    downloadFile(response.data, `${t('myAccount.export.filenameBorrowHistory')}_${getTimestamp()}.${format === 'json' ? 'json' : 'xlsx'}`)
    showToast(t('myAccount.toast.exportSuccess'), 'success')
  } catch (error) {
    logger.error('Failed to export borrow history:', error)
    showToast(t('myAccount.toast.exportFailed'), 'error')
  } finally {
    isExporting.value = false
  }
}

async function exportBookReviews() {
  isExporting.value = true
  try {
    const response = await exportApi.exportBookReviews()
    downloadFile(response.data, `${t('myAccount.export.filenameReviews')}_${getTimestamp()}.xlsx`)
    showToast(t('myAccount.toast.exportSuccess'), 'success')
  } catch (error) {
    logger.error('Failed to export book reviews:', error)
    showToast(t('myAccount.toast.exportFailed'), 'error')
  } finally {
    isExporting.value = false
  }
}

async function exportAllData() {
  isExporting.value = true
  try {
    const response = await exportApi.exportAllData()
    downloadFile(response.data, `${t('myAccount.export.filenameAllData')}_${getTimestamp()}.xlsx`)
    showToast(t('myAccount.toast.exportSuccess'), 'success')
  } catch (error) {
    logger.error('Failed to export all data:', error)
    showToast(t('myAccount.toast.exportFailed'), 'error')
  } finally {
    isExporting.value = false
  }
}
</script>

<style scoped>
.profile-grid,
.account-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.profile-card h2,
.account-grid h2 {
  margin: 0 0 1rem;
  font-family: var(--font-headline);
  font-size: 1.4rem;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.profile-avatar {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dim) 100%);
  color: white;
  font-size: 1.4rem;
  font-weight: 700;
}

.profile-head h2,
.profile-head p {
  margin: 0;
}

.profile-head p,
.muted-copy {
  color: var(--on-surface-variant);
}

.profile-meta,
.profile-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
  margin-top: 1rem;
}

.profile-meta div,
.stat-box {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.95rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.7);
}

.profile-meta span,
.stat-box span,
.form-stack span {
  font-size: 0.74rem;
  font-family: var(--font-label);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--outline);
}

.stat-box strong {
  font-size: 1.4rem;
  font-family: var(--font-headline);
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.form-stack label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-stack input {
  min-height: 3rem;
  padding: 0.8rem 0.95rem;
  border-radius: 1rem;
  border: 1px solid rgba(133, 122, 107, 0.16);
  background: rgba(255, 255, 255, 0.92);
}

.preference-list {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.preference-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.7);
}

.preference-item strong,
.preference-item p {
  margin: 0;
}

.preference-item p {
  margin-top: 0.35rem;
  color: var(--on-surface-variant);
}

.preference-item input {
  width: 1rem;
  height: 1rem;
}

@media (max-width: 1024px) {
  .profile-grid,
  .account-grid {
    grid-template-columns: 1fr;
  }
}

.export-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

@media (max-width: 767px) {
  .profile-meta,
  .profile-stats {
    grid-template-columns: 1fr;
  }

  .preference-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
