<template>
  <div class="damage-reports page-stack">
    <PageHeader
      :title="t('damageReports.title')"
      eyebrow="Damage Reports"
      :description="t('damageReports.description')"
    >
      <template #actions>
        <div class="page-actions">
          <button class="page-action-btn page-action-btn--secondary" @click="refresh">
            <span class="material-symbols-outlined">refresh</span>
            <span>{{ t('damageReports.refresh') }}</span>
          </button>
        </div>
      </template>
    </PageHeader>

    <!-- 统计卡片 -->
    <section v-if="isAdmin && statistics" class="stats-grid surface-card">
      <div class="stat-tile">
        <span class="stat-label">{{ t('damageReports.stats.pending') }}</span>
        <strong class="stat-value stat-value--pending">{{ statistics.pendingCount }}</strong>
      </div>
      <div class="stat-tile">
        <span class="stat-label">{{ t('damageReports.stats.inProgress') }}</span>
        <strong class="stat-value stat-value--progress">{{ statistics.inProgressCount }}</strong>
      </div>
      <div class="stat-tile">
        <span class="stat-label">{{ t('damageReports.stats.resolved') }}</span>
        <strong class="stat-value stat-value--resolved">{{ statistics.resolvedCount }}</strong>
      </div>
      <div class="stat-tile">
        <span class="stat-label">{{ t('damageReports.stats.rejected') }}</span>
        <strong class="stat-value stat-value--rejected">{{ statistics.rejectedCount }}</strong>
      </div>
    </section>

    <!-- 筛选（管理员） -->
    <section v-if="isAdmin" class="filter-bar surface-card">
      <div class="filter-chips">
        <button
          v-for="f in statusFilters"
          :key="f.value"
          class="filter-chip"
          :class="{ active: currentFilter === f.value }"
          @click="setFilter(f.value)"
        >
          {{ f.label }}
        </button>
      </div>
    </section>

    <!-- 报告列表 -->
    <section class="report-list">
      <div v-if="loading && reports.length === 0" class="surface-card loading-card">
        <div class="spinner"></div>
        <p>{{ t('damageReports.loading') }}</p>
      </div>

      <div v-else-if="reports.length === 0" class="surface-card empty-card">
        <span class="material-symbols-outlined empty-icon">inventory_2</span>
        <p>{{ t('damageReports.empty') }}</p>
      </div>

      <div v-else class="report-cards">
        <div
          v-for="report in reports"
          :key="report.id"
          class="report-card surface-card"
          :class="'report-card--' + report.status.toLowerCase()"
          @click="selectReport(report)"
        >
          <div class="report-card-body">
            <div class="report-main">
              <h3 class="report-title">{{ report.bookTitle }}</h3>
              <div class="report-types">
                <span v-for="dt in report.damageTypes" :key="dt" class="damage-type-label">{{ damageTypeLabel(dt) }}</span>
              </div>
              <p class="report-meta">
                {{ formatDate(report.createdAt) }} · {{ report.reporterName }}
                <template v-if="report.resolvedAt"> · {{ t('damageReports.detail.processedAt', { date: formatDate(report.resolvedAt) }) }}</template>
              </p>
            </div>
            <span class="status-pill" :class="'status-pill--' + report.status.toLowerCase()">
              {{ statusLabel(report.status) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page === 0" @click="page--; loadReports()">{{ t('damageReports.pagination.prev') }}</button>
        <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="page >= totalPages - 1" @click="page++; loadReports()">{{ t('damageReports.pagination.next') }}</button>
      </div>
    </section>

    <!-- 详情弹窗 -->
    <div v-if="selectedReport" class="modal-overlay" @click="selectedReport = null">
      <div class="detail-modal" @click.stop>
        <div class="modal-header">
          <h2>{{ t('damageReports.detail.title') }}</h2>
          <button class="close-btn" @click="selectedReport = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.book') }}</span>
            <p>《{{ selectedReport.bookTitle }}》</p>
          </div>
          <div class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.reporter') }}</span>
            <p>{{ selectedReport.reporterName }}</p>
          </div>
          <div class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.damageType') }}</span>
            <div class="report-types">
              <span v-for="dt in selectedReport.damageTypes" :key="dt" class="damage-type-label">{{ damageTypeLabel(dt) }}</span>
            </div>
          </div>
          <div v-if="selectedReport.description" class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.description') }}</span>
            <p>{{ selectedReport.description }}</p>
          </div>
          <div class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.photos') }}</span>
            <div class="detail-photos">
              <img
                v-for="(url, idx) in selectedReport.photoUrls"
                :key="idx"
                :src="photoFullUrl(url)"
                :alt="t('damageReports.detail.photoAlt')"
                class="detail-photo"
              />
            </div>
          </div>
          <div class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.status') }}</span>
            <span class="status-pill" :class="'status-pill--' + selectedReport.status.toLowerCase()">
              {{ statusLabel(selectedReport.status) }}
            </span>
          </div>
          <div v-if="selectedReport.adminNotes" class="detail-section">
            <span class="detail-label">{{ t('damageReports.detail.adminNotes') }}</span>
            <p>{{ selectedReport.adminNotes }}</p>
          </div>

          <!-- 管理员操作 -->
          <template v-if="isAdmin && selectedReport.status === 'PENDING'">
            <div class="admin-actions">
              <textarea v-model="adminNotes" class="form-textarea" :placeholder="t('damageReports.detail.processingNote')" rows="2"></textarea>
              <div class="admin-btns">
                <button class="btn-action btn-action--resolve" @click="handleUpdate('IN_PROGRESS')">{{ t('damageReports.detail.startProcessing') }}</button>
                <button class="btn-action btn-action--reject" @click="handleUpdate('REJECTED')">{{ t('damageReports.detail.reject') }}</button>
              </div>
            </div>
          </template>
          <template v-if="isAdmin && selectedReport.status === 'IN_PROGRESS'">
            <div class="admin-actions">
              <textarea v-model="adminNotes" class="form-textarea" :placeholder="t('damageReports.detail.processingNote')" rows="2"></textarea>
              <div class="admin-btns">
                <button class="btn-action btn-action--resolve" @click="handleUpdate('RESOLVED')">{{ t('damageReports.detail.markResolved') }}</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../stores/user'
import { damageReportApi, type DamageReport, type DamageReportStatistics } from '../api/damageReportApi'
import { API_CONFIG } from '../config'
import PageHeader from '../components/layout/PageHeader.vue'
import { formatLocalDate as formatDate } from '../utils/timeHelpers'
import { logger } from '../utils/logger'

const { t } = useI18n()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const reports = ref<DamageReport[]>([])
const statistics = ref<DamageReportStatistics | null>(null)
const loading = ref(false)
const page = ref(0)
const totalPages = ref(1)
const currentFilter = ref('')
const selectedReport = ref<DamageReport | null>(null)
const adminNotes = ref('')

const statusFilters = computed(() => [
  { label: t('damageReports.filters.all'), value: '' },
  { label: t('damageReports.filters.pending'), value: 'PENDING' },
  { label: t('damageReports.filters.inProgress'), value: 'IN_PROGRESS' },
  { label: t('damageReports.filters.resolved'), value: 'RESOLVED' },
  { label: t('damageReports.filters.rejected'), value: 'REJECTED' },
])

function damageTypeLabel(key: string) {
  return t(`damageReports.damageType.${key}`, key)
}

function statusLabel(key: string) {
  return t(`damageReports.statusMap.${key}`, key)
}

function photoFullUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `${API_CONFIG.baseURL}${url}`
}

function setFilter(value: string) {
  currentFilter.value = value
  page.value = 0
  loadReports()
}

function selectReport(report: DamageReport) {
  selectedReport.value = report
  adminNotes.value = ''
}

async function loadReports() {
  loading.value = true
  try {
    if (isAdmin.value) {
      const res = await damageReportApi.getAllReports({
        status: currentFilter.value || undefined,
        page: page.value,
        size: 10,
      })
      if (res.data.success) {
        reports.value = res.data.data
        totalPages.value = res.data.totalPages || 1
      }
    } else {
      const res = await damageReportApi.getMyReports(page.value, 10)
      if (res.data.success) {
        reports.value = res.data.data
        totalPages.value = res.data.totalPages || 1
      }
    }
  } catch (e) {
    logger.error('Failed to load reports', e)
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  if (!isAdmin.value) return
  try {
    const res = await damageReportApi.getStatistics()
    if (res.data.success) {
      statistics.value = res.data.data
    }
  } catch (e) {
    logger.error('Failed to load statistics', e)
  }
}

async function handleUpdate(status: string) {
  if (!selectedReport.value) return
  try {
    const res = await damageReportApi.updateStatus(selectedReport.value.id, {
      status,
      adminNotes: adminNotes.value.trim() || undefined,
    })
    if (res.data.success) {
      selectedReport.value = null
      await refresh()
    }
  } catch (e) {
    logger.error('Failed to update status', e)
  }
}

async function refresh() {
  await Promise.all([loadReports(), loadStatistics()])
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.stat-tile {
  text-align: center;
  padding: 0.75rem 0;
}

.stat-label {
  display: block;
  font-size: 0.75rem;
  color: rgba(57, 68, 58, 0.6);
  margin-bottom: 0.25rem;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
}

.stat-value--pending { color: #8b6f47; }
.stat-value--progress { color: #3c6e71; }
.stat-value--resolved { color: #557c55; }
.stat-value--rejected { color: #7c4f4f; }

.filter-bar {
  padding: 0.75rem 1.25rem;
  margin-bottom: 1rem;
}

.filter-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 4px 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(110, 122, 102, 0.14);
  border-radius: 20px;
  color: rgba(50, 67, 56, 0.7);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-chip.active {
  background: rgba(133, 160, 131, 0.18);
  border-color: rgba(133, 160, 131, 0.32);
  color: #3d5a3a;
}

.report-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.report-card {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  border-left: 3px solid transparent;
  padding: 1rem 1.25rem;
}

.report-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(47, 58, 48, 0.1);
}

.report-card--pending { border-left-color: #8b6f47; }
.report-card--in_progress { border-left-color: #3c6e71; }
.report-card--resolved { border-left-color: #557c55; }
.report-card--rejected { border-left-color: #7c4f4f; }

.report-card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.report-title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 500;
  color: #1b2821;
}

.report-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.damage-type-label {
  padding: 2px 8px;
  background: rgba(133, 160, 131, 0.1);
  border-radius: 10px;
  font-size: 0.7rem;
  color: rgba(61, 90, 58, 0.7);
}

.report-meta {
  margin: 4px 0 0;
  font-size: 0.75rem;
  color: rgba(50, 67, 56, 0.45);
}

.status-pill {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

.status-pill--pending { background: rgba(139, 111, 71, 0.14); color: #7c633e; }
.status-pill--in_progress { background: rgba(60, 110, 113, 0.14); color: #315f63; }
.status-pill--resolved { background: rgba(85, 124, 85, 0.14); color: #456846; }
.status-pill--rejected { background: rgba(124, 79, 79, 0.14); color: #7c4f4f; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.page-btn {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(110, 122, 102, 0.16);
  border-radius: 8px;
  color: #1b2821;
  font-size: 0.8rem;
  cursor: pointer;
}

.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.page-info {
  font-size: 0.8rem;
  color: rgba(57, 68, 58, 0.6);
}

.loading-card, .empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: rgba(57, 68, 58, 0.6);
  text-align: center;
}

.empty-icon { font-size: 2.5rem; margin-bottom: 0.5rem; opacity: 0.4; }

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(110, 124, 104, 0.15);
  border-top-color: #5b725a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 0.75rem;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 详情弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.detail-modal {
  background: linear-gradient(180deg, rgba(249, 246, 239, 0.96) 0%, rgba(239, 234, 223, 0.92) 100%);
  border: 1px solid rgba(110, 124, 104, 0.14);
  border-radius: 16px;
  width: 100%;
  max-width: 520px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 40px 90px rgba(47, 58, 48, 0.18);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(88, 100, 81, 0.12);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: #1b2821;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: rgba(50, 67, 56, 0.5);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}

.modal-body {
  padding: 1.25rem 1.5rem 1.5rem;
}

.detail-section {
  margin-bottom: 1rem;
}

.detail-label {
  display: block;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(118, 91, 56, 0.78);
  margin-bottom: 0.25rem;
}

.detail-section p {
  margin: 0;
  color: #1b2821;
  font-size: 0.9rem;
}

.detail-photos {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-photo {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgba(110, 124, 104, 0.14);
}

.admin-actions {
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(88, 100, 81, 0.12);
}

.form-textarea {
  width: 100%;
  padding: 10px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.92) 0%, rgba(246, 241, 231, 0.88) 100%);
  border: 1px solid rgba(110, 122, 102, 0.16);
  border-radius: 8px;
  color: #1b2821;
  font-size: 0.85rem;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
  margin-bottom: 0.75rem;
}

.admin-btns {
  display: flex;
  gap: 8px;
}

.btn-action {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-action--resolve {
  background: linear-gradient(135deg, #335c67, #537072);
  color: #fffdf5;
  box-shadow: 0 8px 20px rgba(51, 92, 103, 0.2);
}

.btn-action--reject {
  background: rgba(184, 92, 56, 0.12);
  color: #8b482f;
  border: 1px solid rgba(184, 92, 56, 0.2);
}
</style>
