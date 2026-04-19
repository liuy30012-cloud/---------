<template>
  <div class="user-management page-stack">
    <PageHeader
      :title="t('userManagement.title')"
      :eyebrow="t('userManagement.eyebrow')"
      :description="t('userManagement.description')"
    >
      <template #actions>
        <div class="page-actions">
          <LibraryButton type="secondary" @click="resetFilters">
            {{ t('userManagement.actions.reset') }}
          </LibraryButton>
          <LibraryButton type="primary" @click="refreshAll">
            {{ t('userManagement.actions.refresh') }}
          </LibraryButton>
        </div>
      </template>
    </PageHeader>

    <section class="stats-grid">
      <article v-reveal="{ preset: 'card', delay: 0, once: true }" class="stat-card stat-card--total">
        <span class="stat-kicker">{{ t('userManagement.stats.totalUsers') }}</span>
        <strong class="stat-value">{{ statistics?.totalUsers ?? 0 }}</strong>
      </article>
      <article v-reveal="{ preset: 'card', delay: 0.05, once: true }" class="stat-card stat-card--active">
        <span class="stat-kicker">{{ t('userManagement.stats.activeUsers') }}</span>
        <strong class="stat-value">{{ statistics?.activeUsers ?? 0 }}</strong>
      </article>
      <article v-reveal="{ preset: 'card', delay: 0.1, once: true }" class="stat-card stat-card--disabled">
        <span class="stat-kicker">{{ t('userManagement.stats.disabledUsers') }}</span>
        <strong class="stat-value">{{ statistics?.disabledUsers ?? 0 }}</strong>
      </article>
      <article v-reveal="{ preset: 'card', delay: 0.15, once: true }" class="stat-card stat-card--admin">
        <span class="stat-kicker">{{ t('userManagement.stats.activeAdmins') }}</span>
        <strong class="stat-value">{{ statistics?.activeAdmins ?? 0 }}</strong>
      </article>
    </section>

    <section v-reveal="{ preset: 'section', delay: 0.05, once: true }" class="surface-card filter-panel">
      <div class="filter-head">
        <div>
          <p class="section-kicker">{{ t('userManagement.filters.title') }}</p>
          <h2>{{ t('userManagement.notices.maskedEmail') }}</h2>
        </div>
        <p class="filter-note">
          {{ activeAdminWarning ? t('userManagement.notices.lastAdminProtected') : t('userManagement.notices.selfProtected') }}
        </p>
      </div>

      <div class="filter-grid">
        <label class="field field--keyword">
          <span class="field-label">{{ t('userManagement.filters.keyword') }}</span>
          <input
            v-model.trim="filters.keyword"
            class="field-input"
            type="text"
            :placeholder="t('userManagement.filters.keywordPlaceholder')"
            @keyup.enter="applyFilters"
          />
        </label>

        <label class="field">
          <span class="field-label">{{ t('userManagement.filters.role') }}</span>
          <select v-model="filters.role" class="field-input">
            <option value="">{{ t('userManagement.filters.allRoles') }}</option>
            <option value="STUDENT">{{ t('userManagement.roleOptions.STUDENT') }}</option>
            <option value="TEACHER">{{ t('userManagement.roleOptions.TEACHER') }}</option>
            <option value="ADMIN">{{ t('userManagement.roleOptions.ADMIN') }}</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">{{ t('userManagement.filters.status') }}</span>
          <select v-model="filters.status" class="field-input">
            <option value="">{{ t('userManagement.filters.allStatuses') }}</option>
            <option :value="'1'">{{ t('userManagement.statusOptions.1') }}</option>
            <option :value="'0'">{{ t('userManagement.statusOptions.0') }}</option>
          </select>
        </label>

        <div class="filter-actions">
          <LibraryButton type="primary" @click="applyFilters">
            {{ t('userManagement.actions.apply') }}
          </LibraryButton>
        </div>
      </div>
    </section>

    <section v-reveal="{ preset: 'section', delay: 0.1, once: true }" class="surface-card board-panel">
      <div class="board-toolbar">
        <p class="board-summary">
          {{ t('userManagement.pagination.summary', { total, page: page + 1, totalPages: Math.max(totalPages, 1) }) }}
        </p>
      </div>

      <div v-if="loading" class="board-state">
        <p>{{ t('userManagement.actions.refresh') }}...</p>
      </div>

      <div v-else-if="users.length === 0" class="board-state board-state--empty">
        <h3>{{ t('userManagement.empty.title') }}</h3>
        <p>{{ t('userManagement.empty.description') }}</p>
      </div>

      <template v-else>
        <div class="table-shell">
          <table class="user-table">
            <thead>
              <tr>
                <th>{{ t('userManagement.table.studentId') }}</th>
                <th>{{ t('userManagement.table.username') }}</th>
                <th>{{ t('userManagement.table.email') }}</th>
                <th>{{ t('userManagement.table.role') }}</th>
                <th>{{ t('userManagement.table.status') }}</th>
                <th>{{ t('userManagement.table.loginCount') }}</th>
                <th>{{ t('userManagement.table.lastLoginTime') }}</th>
                <th>{{ t('userManagement.table.createdAt') }}</th>
                <th>{{ t('userManagement.table.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.studentId }}</td>
                <td>
                  <div class="user-identity">
                    <span class="user-name">{{ user.username }}</span>
                    <span v-if="isCurrentUser(user)" class="self-badge">YOU</span>
                  </div>
                </td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <select v-model="roleDrafts[user.id]" class="role-select" :disabled="isRoleActionDisabled(user)">
                    <option value="STUDENT">{{ t('userManagement.roleOptions.STUDENT') }}</option>
                    <option value="TEACHER">{{ t('userManagement.roleOptions.TEACHER') }}</option>
                    <option value="ADMIN">{{ t('userManagement.roleOptions.ADMIN') }}</option>
                  </select>
                </td>
                <td>
                  <span class="status-pill" :class="user.status === 1 ? 'status-pill--active' : 'status-pill--disabled'">
                    {{ user.status === 1 ? t('userManagement.statusOptions.1') : t('userManagement.statusOptions.0') }}
                  </span>
                </td>
                <td>{{ user.loginCount }}</td>
                <td>{{ formatDate(user.lastLoginTime) }}</td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>
                  <div class="row-actions">
                    <button
                      class="table-action table-action--save"
                      type="button"
                      :disabled="!canSaveRole(user)"
                      @click="saveRole(user)"
                    >
                      {{ savingRoleId === user.id ? t('userManagement.actions.saving') : t('userManagement.actions.saveRole') }}
                    </button>
                    <button
                      class="table-action"
                      :class="user.status === 1 ? 'table-action--warn' : 'table-action--ok'"
                      type="button"
                      :disabled="isStatusActionDisabled(user)"
                      @click="openStatusDialog(user)"
                    >
                      {{ statusBusyId === user.id ? (user.status === 1 ? t('userManagement.actions.disabling') : t('userManagement.actions.enabling')) : (user.status === 1 ? t('userManagement.actions.disable') : t('userManagement.actions.enable')) }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mobile-cards">
          <article v-for="user in users" :key="user.id" class="mobile-card">
            <div class="mobile-card__header">
              <div>
                <p class="mobile-card__student">{{ user.studentId }}</p>
                <h3>{{ user.username }}</h3>
              </div>
              <span class="status-pill" :class="user.status === 1 ? 'status-pill--active' : 'status-pill--disabled'">
                {{ user.status === 1 ? t('userManagement.statusOptions.1') : t('userManagement.statusOptions.0') }}
              </span>
            </div>

            <dl class="mobile-card__meta">
              <div>
                <dt>{{ t('userManagement.cards.contact') }}</dt>
                <dd>{{ user.email || '-' }}</dd>
              </div>
              <div>
                <dt>{{ t('userManagement.table.role') }}</dt>
                <dd>{{ t(`userManagement.roleOptions.${user.role}`) }}</dd>
              </div>
              <div>
                <dt>{{ t('userManagement.cards.loginCount') }}</dt>
                <dd>{{ user.loginCount }}</dd>
              </div>
              <div>
                <dt>{{ t('userManagement.cards.lastLoginTime') }}</dt>
                <dd>{{ formatDate(user.lastLoginTime) }}</dd>
              </div>
              <div>
                <dt>{{ t('userManagement.cards.createdAt') }}</dt>
                <dd>{{ formatDate(user.createdAt) }}</dd>
              </div>
            </dl>

            <div class="mobile-card__actions">
              <select v-model="roleDrafts[user.id]" class="role-select" :disabled="isRoleActionDisabled(user)">
                <option value="STUDENT">{{ t('userManagement.roleOptions.STUDENT') }}</option>
                <option value="TEACHER">{{ t('userManagement.roleOptions.TEACHER') }}</option>
                <option value="ADMIN">{{ t('userManagement.roleOptions.ADMIN') }}</option>
              </select>

              <div class="mobile-card__buttons">
                <button
                  class="table-action table-action--save"
                  type="button"
                  :disabled="!canSaveRole(user)"
                  @click="saveRole(user)"
                >
                  {{ savingRoleId === user.id ? t('userManagement.actions.saving') : t('userManagement.actions.saveRole') }}
                </button>
                <button
                  class="table-action"
                  :class="user.status === 1 ? 'table-action--warn' : 'table-action--ok'"
                  type="button"
                  :disabled="isStatusActionDisabled(user)"
                  @click="openStatusDialog(user)"
                >
                  {{ user.status === 1 ? t('userManagement.actions.disable') : t('userManagement.actions.enable') }}
                </button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <el-pagination
            :current-page="page + 1"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </section>

    <ConfirmDialog
      :open="dialog.open"
      :eyebrow="dialog.eyebrow"
      :title="dialog.title"
      :message="dialog.message"
      :confirm-text="dialog.confirmText"
      :cancel-text="dialog.cancelText"
      @confirm="confirmStatusChange"
      @cancel="closeDialog"
    />

    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { userManagementApi, type ManagedUser, type ManagedUserRole, type UserManagementStatistics } from '../api/userManagementApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import LibraryButton from '../components/common/LibraryButton.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { useToast } from '../composables/useToast'
import { useUserStore } from '../stores/user'
import { sanitizeApiMessage } from '../utils/apiMessage'
import { logger } from '../utils/logger'
import { formatLocalDate as formatDate } from '../utils/timeHelpers'

type ManagedStatus = 0 | 1
const USER_MANAGEMENT_MESSAGE_KEY_MAP: Record<string, string> = {
  'admin.user.error.role_required': 'userManagement.toast.roleRequired',
  'admin.user.error.role_invalid': 'userManagement.toast.roleInvalid',
  'admin.user.error.status_required': 'userManagement.toast.statusRequired',
  'admin.user.error.status_invalid': 'userManagement.toast.statusInvalid',
  'admin.user.error.self_role_change_forbidden': 'userManagement.toast.selfProtected',
  'admin.user.error.self_status_change_forbidden': 'userManagement.toast.selfProtected',
  'admin.user.error.must_keep_active_admin': 'userManagement.toast.lastAdminProtected',
  'admin.user.error.user_not_found': 'userManagement.toast.userNotFound',
  'admin.user.error.auth_token_missing': 'userManagement.toast.authInvalid',
  'admin.user.error.auth_invalid': 'userManagement.toast.authInvalid',
}

const { t, locale } = useI18n()
const userStore = useUserStore()
const { dialog, openDialog, closeDialog } = useConfirmDialog()
const { toast, showToast } = useToast()

const loading = ref(false)
const users = ref<ManagedUser[]>([])
const statistics = ref<UserManagementStatistics | null>(null)
const page = ref(0)
const size = ref(10)
const total = ref(0)
const totalPages = ref(1)
const savingRoleId = ref<number | null>(null)
const statusBusyId = ref<number | null>(null)
const pendingStatusUser = ref<ManagedUser | null>(null)
const pendingStatusValue = ref<ManagedStatus | null>(null)

const filters = reactive({
  keyword: '',
  role: '',
  status: '',
})

const roleDrafts = reactive<Record<number, ManagedUserRole>>({})

const activeAdminCount = computed(() => (
  typeof statistics.value?.activeAdmins === 'number' ? statistics.value.activeAdmins : null
))
const activeAdminWarning = computed(() => activeAdminCount.value !== null && activeAdminCount.value <= 1)

onMounted(() => {
  void refreshAll()
})

async function refreshAll() {
  await Promise.all([loadUsers(), loadStatistics()])
}

async function loadUsers() {
  loading.value = true
  try {
    const response = await userManagementApi.getUsers({
      keyword: filters.keyword || undefined,
      role: (filters.role || undefined) as ManagedUserRole | undefined,
      status: filters.status === '' ? undefined : Number(filters.status) as ManagedStatus,
      page: page.value,
      size: size.value,
    })

    users.value = response.data.data
    total.value = response.data.total || 0
    totalPages.value = response.data.totalPages || 1

    const nextDrafts: Record<number, ManagedUserRole> = {}
    for (const user of users.value) {
      nextDrafts[user.id] = user.role
    }

    Object.keys(roleDrafts).forEach((key) => {
      delete roleDrafts[Number(key)]
    })
    Object.assign(roleDrafts, nextDrafts)
  } catch (error: any) {
    logger.error('Failed to load managed users:', error)
    showToast(resolveApiMessage(error?.response?.data?.message, 'userManagement.toast.loadFailed'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const response = await userManagementApi.getStatistics()
    statistics.value = response.data.data
  } catch (error) {
    logger.error('Failed to load user management statistics:', error)
  }
}

function applyFilters() {
  page.value = 0
  void refreshAll()
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  page.value = 0
  void refreshAll()
}

function goToPage(nextPage: number) {
  page.value = nextPage
  void loadUsers()
}

function handlePageChange(newPage: number) {
  goToPage(newPage - 1)
}

function isCurrentUser(user: ManagedUser) {
  return userStore.user?.id === user.id
}

function isLastActiveAdmin(user: ManagedUser) {
  return activeAdminCount.value !== null
    && user.role === 'ADMIN'
    && user.status === 1
    && activeAdminCount.value <= 1
}

function isRoleActionDisabled(user: ManagedUser) {
  return isCurrentUser(user) || savingRoleId.value === user.id || statusBusyId.value === user.id
}

function isStatusActionDisabled(user: ManagedUser) {
  return isCurrentUser(user) || isLastActiveAdmin(user) || statusBusyId.value === user.id
}

function canSaveRole(user: ManagedUser) {
  if (isCurrentUser(user) || savingRoleId.value === user.id || statusBusyId.value === user.id) {
    return false
  }

  const nextRole = roleDrafts[user.id]
  if (!nextRole || nextRole === user.role) {
    return false
  }

  if (isLastActiveAdmin(user) && nextRole !== 'ADMIN') {
    return false
  }

  return true
}

async function saveRole(user: ManagedUser) {
  if (isCurrentUser(user)) {
    showToast(t('userManagement.toast.selfProtected'), 'info')
    return
  }

  const nextRole = roleDrafts[user.id]
  if (!nextRole || nextRole === user.role) {
    return
  }

  if (isLastActiveAdmin(user) && nextRole !== 'ADMIN') {
    showToast(t('userManagement.toast.lastAdminProtected'), 'error')
    return
  }

  savingRoleId.value = user.id
  try {
    await userManagementApi.updateRole(user.id, nextRole)
    showToast(t('userManagement.toast.roleUpdated'), 'success')
    await refreshAll()
  } catch (error: any) {
    logger.error('Failed to update user role:', error)
    roleDrafts[user.id] = user.role
    showToast(resolveApiMessage(error?.response?.data?.message, 'userManagement.toast.roleUpdateFailed'), 'error')
  } finally {
    savingRoleId.value = null
  }
}

function openStatusDialog(user: ManagedUser) {
  if (isCurrentUser(user)) {
    showToast(t('userManagement.toast.selfProtected'), 'info')
    return
  }

  if (isLastActiveAdmin(user)) {
    showToast(t('userManagement.toast.lastAdminProtected'), 'error')
    return
  }

  pendingStatusUser.value = user
  pendingStatusValue.value = user.status === 1 ? 0 : 1

  const enabling = pendingStatusValue.value === 1
  openDialog({
    eyebrow: t(enabling ? 'userManagement.dialog.enableEyebrow' : 'userManagement.dialog.disableEyebrow'),
    title: t(enabling ? 'userManagement.dialog.enableTitle' : 'userManagement.dialog.disableTitle'),
    message: t(enabling ? 'userManagement.dialog.enableMessage' : 'userManagement.dialog.disableMessage'),
    confirmText: t(enabling ? 'userManagement.dialog.confirmEnable' : 'userManagement.dialog.confirmDisable'),
    cancelText: t('userManagement.dialog.cancel'),
    action: enabling ? 'enable' : 'disable',
  })
}

async function confirmStatusChange() {
  if (!pendingStatusUser.value || pendingStatusValue.value === null) {
    closeDialog()
    return
  }

  statusBusyId.value = pendingStatusUser.value.id
  try {
    await userManagementApi.updateStatus(pendingStatusUser.value.id, pendingStatusValue.value)
    showToast(t('userManagement.toast.statusUpdated'), 'success')
    await refreshAll()
  } catch (error: any) {
    logger.error('Failed to update user status:', error)
    showToast(resolveApiMessage(error?.response?.data?.message, 'userManagement.toast.statusUpdateFailed'), 'error')
  } finally {
    statusBusyId.value = null
    pendingStatusUser.value = null
    pendingStatusValue.value = null
    closeDialog()
  }
}

function resolveApiMessage(message: unknown, fallbackKey: string) {
  const fallback = t(fallbackKey)
  if (typeof message !== 'string' || !message.trim()) {
    return fallback
  }

  const normalized = message.trim()
  const mappedKey = USER_MANAGEMENT_MESSAGE_KEY_MAP[normalized]
  if (mappedKey) {
    return t(mappedKey)
  }
  if (locale.value === 'en' && /[\u3400-\u9FFF]/u.test(normalized)) {
    return fallback
  }

  return sanitizeApiMessage(normalized, fallback)
}
</script>

<style scoped>
.user-management {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.page-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 1.35rem 1.4rem;
  border-radius: 1.25rem;
  background: linear-gradient(180deg, rgba(255, 251, 243, 0.96), rgba(242, 236, 226, 0.92));
  border: 1px solid rgba(120, 104, 79, 0.12);
  box-shadow: var(--shadow-soft);
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: auto -10% -20% auto;
  width: 8rem;
  height: 8rem;
  border-radius: 50%;
  opacity: 0.18;
  filter: blur(4px);
}

.stat-card--total::after { background: rgba(188, 148, 93, 0.5); }
.stat-card--active::after { background: rgba(67, 124, 94, 0.45); }
.stat-card--disabled::after { background: rgba(153, 84, 71, 0.45); }
.stat-card--admin::after { background: rgba(64, 95, 145, 0.45); }

.stat-kicker {
  display: block;
  margin-bottom: 0.45rem;
  font-size: 0.75rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(115, 88, 55, 0.72);
}

.stat-value {
  position: relative;
  z-index: 1;
  font-size: clamp(1.9rem, 4vw, 2.6rem);
  line-height: 1;
  color: var(--home-ink);
}

.filter-panel,
.board-panel {
  padding: 1.35rem 1.45rem;
}

.filter-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.section-kicker {
  margin: 0 0 0.35rem;
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(115, 88, 55, 0.72);
}

.filter-head h2 {
  margin: 0;
  font-size: 1.05rem;
  color: var(--home-ink);
}

.filter-note,
.board-summary {
  margin: 0;
  color: var(--on-surface-variant);
  line-height: 1.6;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 2.3fr) repeat(2, minmax(0, 1fr)) auto;
  gap: 0.9rem;
  align-items: end;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.field-label {
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--outline);
}

.field-input,
.role-select {
  min-height: 3rem;
  padding: 0.78rem 0.95rem;
  border-radius: 1rem;
  border: 1px solid rgba(135, 120, 99, 0.16);
  background: rgba(255, 255, 255, 0.88);
  color: var(--home-ink);
  font: inherit;
}

.board-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
}

.board-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1rem;
  text-align: center;
  color: var(--on-surface-variant);
}

.board-state--empty h3 {
  margin: 0 0 0.5rem;
  color: var(--home-ink);
}

.table-shell {
  overflow: hidden;
  border-radius: 1.15rem;
  border: 1px solid rgba(113, 124, 105, 0.12);
  background: rgba(255, 255, 255, 0.78);
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 0.9rem 0.85rem;
  border-bottom: 1px solid rgba(113, 124, 105, 0.1);
  text-align: left;
  vertical-align: middle;
}

.user-table th {
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(87, 95, 93, 0.76);
  background: rgba(247, 242, 233, 0.96);
}

.user-table tbody tr:hover {
  background: rgba(255, 250, 242, 0.72);
}

.user-table tbody tr:last-child td {
  border-bottom: none;
}

.user-identity {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.user-name {
  font-weight: 700;
  color: var(--home-ink);
}

.self-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.16rem 0.48rem;
  border-radius: 999px;
  background: rgba(65, 98, 148, 0.12);
  color: #39537c;
  font-size: 0.68rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.32rem 0.75rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-pill--active {
  background: rgba(82, 135, 101, 0.14);
  color: #356445;
}

.status-pill--disabled {
  background: rgba(163, 82, 70, 0.14);
  color: #8f4a3d;
}

.row-actions,
.mobile-card__buttons {
  display: flex;
  gap: 0.55rem;
  align-items: center;
}

.table-action {
  min-height: 2.6rem;
  padding: 0.65rem 0.9rem;
  border: 1px solid rgba(113, 124, 105, 0.14);
  border-radius: 0.95rem;
  background: rgba(255, 255, 255, 0.9);
  color: var(--home-ink);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.table-action:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.table-action--save {
  background: linear-gradient(135deg, rgba(192, 160, 110, 0.2), rgba(221, 196, 155, 0.28));
}

.table-action--warn {
  color: #8f4a3d;
  border-color: rgba(163, 82, 70, 0.18);
  background: rgba(253, 244, 241, 0.9);
}

.table-action--ok {
  color: #356445;
  border-color: rgba(82, 135, 101, 0.18);
  background: rgba(244, 252, 247, 0.92);
}

.mobile-cards {
  display: none;
  gap: 0.9rem;
}

.mobile-card {
  border-radius: 1rem;
  border: 1px solid rgba(113, 124, 105, 0.12);
  background: rgba(255, 255, 255, 0.82);
  padding: 1rem;
}

.mobile-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.mobile-card__header h3 {
  margin: 0.2rem 0 0;
  color: var(--home-ink);
}

.mobile-card__student {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--outline);
}

.mobile-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
  margin: 1rem 0;
}

.mobile-card__meta dt {
  margin-bottom: 0.28rem;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--outline);
}

.mobile-card__meta dd {
  margin: 0;
  color: var(--home-ink);
}

.mobile-card__actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.25rem;
}

:deep(.el-pagination) {
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.9);
  --el-pagination-button-color: var(--home-ink);
  --el-pagination-hover-color: var(--el-color-primary);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .table-shell {
    display: none;
  }

  .mobile-cards {
    display: grid;
  }

  .board-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 767px) {
  .stats-grid,
  .filter-grid,
  .mobile-card__meta {
    grid-template-columns: 1fr;
  }

  .filter-head,
  .page-actions,
  .mobile-card__header,
  .mobile-card__buttons,
  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .row-actions,
  .mobile-card__buttons,
  .table-action {
    width: 100%;
  }
}
</style>
