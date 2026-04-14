<template>
  <div class="dashboard page-stack">
    <PageHeader
      :title="t('dashboard.title')"
      :eyebrow="t('dashboard.eyebrow')"
      :description="t('dashboard.description')"
    >
      <template #actions>
        <button class="page-action-btn page-action-btn--primary" @click="loadData">
          <span>{{ t('dashboard.refresh') }}</span>
        </button>
      </template>
    </PageHeader>

    <div v-if="loading" v-reveal="{ preset: 'section', once: true }" class="loading">
      <p>{{ t('dashboard.loading') }}</p>
    </div>

    <div v-else-if="errorMessage" v-reveal="{ preset: 'section', once: true }" class="error-state">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else class="dashboard-content">
      <div class="stats-cards">
        <div v-reveal="{ preset: 'card', delay: 0, once: true }" class="stat-card">
          <div class="stat-icon">B</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.totalBooks || 0 }}</div>
            <div class="stat-label">{{ t('dashboard.stats.totalBooks') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.05, once: true }" class="stat-card">
          <div class="stat-icon">A</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.availableBooks || 0 }}</div>
            <div class="stat-label">{{ t('dashboard.stats.availableBooks') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.1, once: true }" class="stat-card">
          <div class="stat-icon">O</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.borrowedBooks || 0 }}</div>
            <div class="stat-label">{{ t('dashboard.stats.borrowedBooks') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.15, once: true }" class="stat-card">
          <div class="stat-icon">!</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.overdueBooks || 0 }}</div>
            <div class="stat-label">{{ t('dashboard.stats.overdueBooks') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.2, once: true }" class="stat-card">
          <div class="stat-icon">R</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.reservedBooks || 0 }}</div>
            <div class="stat-label">{{ t('dashboard.stats.reservedBooks') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.25, once: true }" class="stat-card">
          <div class="stat-icon">%</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboardData?.inventory.utilizationRate || 0 }}%</div>
            <div class="stat-label">{{ t('dashboard.stats.utilizationRate') }}</div>
          </div>
        </div>
      </div>

      <div class="charts-grid">
        <div v-reveal="{ preset: 'section', delay: 0.06, once: true }" class="chart-card">
          <div class="chart-header">
            <h3>{{ t('dashboard.charts.borrowTrend') }}</h3>
          </div>
          <div ref="borrowTrendChart" class="chart-container"></div>
        </div>

        <div v-reveal="{ preset: 'section', delay: 0.12, once: true }" class="chart-card">
          <div class="chart-header">
            <h3>{{ t('dashboard.charts.popularBooks') }}</h3>
          </div>
          <div ref="popularBooksChart" class="chart-container"></div>
        </div>

        <div v-reveal="{ preset: 'section', delay: 0.18, once: true }" class="chart-card">
          <div class="chart-header">
            <h3>{{ t('dashboard.charts.categoryTotal') }}</h3>
          </div>
          <div ref="categoryChart" class="chart-container"></div>
        </div>

        <div v-reveal="{ preset: 'section', delay: 0.24, once: true }" class="chart-card">
          <div class="chart-header">
            <h3>{{ t('dashboard.charts.categoryRate') }}</h3>
          </div>
          <div ref="categoryRateChart" class="chart-container"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '../components/layout/PageHeader.vue'
import { useDashboardCharts } from '../composables/useDashboardCharts'

const { t } = useI18n()

const {
  loading,
  dashboardData,
  errorMessage,
  borrowTrendChart,
  popularBooksChart,
  categoryChart,
  categoryRateChart,
  initEcharts,
  loadData,
  handleResize,
  dispose,
} = useDashboardCharts()

let ticking = false
function throttledResize() {
  if (!ticking) {
    requestAnimationFrame(() => {
      handleResize()
      ticking = false
    })
    ticking = true
  }
}

onMounted(async () => {
  await initEcharts()
  loadData()
  window.addEventListener('resize', throttledResize)
})

onUnmounted(() => {
  dispose()
  window.removeEventListener('resize', throttledResize)
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.loading,
.error-state {
  text-align: center;
  padding: 5rem 1.5rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.82);
  border-radius: 1.25rem;
  box-shadow: var(--shadow-soft);
}

.loading {
  color: #718096;
}

.error-state {
  color: #c53030;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 1rem;
}

.stat-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-soft);
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.9rem;
  background: rgba(0, 83, 219, 0.08);
  color: var(--primary);
  font-family: var(--font-headline);
  font-weight: 800;
  font-size: 1.1rem;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--on-surface);
}

.stat-label {
  font-size: 0.88rem;
  color: var(--on-surface-variant);
  margin-top: 0.2rem;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(430px, 1fr));
  gap: 1rem;
}

.chart-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 1.5rem;
  box-shadow: var(--shadow-soft);
}

.chart-header {
  margin-bottom: 1rem;
}

.chart-header h3 {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--on-surface);
  margin: 0;
}

.chart-container {
  width: 100%;
  height: 400px;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
