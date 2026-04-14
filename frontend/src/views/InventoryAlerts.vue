<template>
  <div class="inventory-alerts page-stack">
    <PageHeader
      :title="t('inventoryAlerts.title')"
      eyebrow="Collection Signals"
      :description="t('inventoryAlerts.description')"
    >
      <template #actions>
        <button class="page-action-btn page-action-btn--primary" @click="loadAlerts">
          <span>{{ t('inventoryAlerts.refresh') }}</span>
        </button>
      </template>
    </PageHeader>

    <div v-if="loading" v-reveal="{ preset: 'section', once: true }" class="loading">
      <p>{{ t('inventoryAlerts.loading') }}</p>
    </div>

    <div v-else-if="errorMessage" v-reveal="{ preset: 'section', once: true }" class="loading">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else class="alerts-content">
      <div class="summary-cards">
        <div v-reveal="{ preset: 'card', delay: 0, once: true }" class="summary-card total">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.total') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.totalAlerts || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.totalAlerts') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.05, once: true }" class="summary-card critical">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.critical') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.criticalAlerts || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.criticalAlerts') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.1, once: true }" class="summary-card warning">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.warning') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.warningAlerts || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.warningAlerts') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.15, once: true }" class="summary-card out-of-stock">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.outOfStock') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.outOfStockCount || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.outOfStock') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.2, once: true }" class="summary-card low-stock">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.lowStock') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.lowStockCount || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.lowStock') }}</div>
          </div>
        </div>
        <div v-reveal="{ preset: 'card', delay: 0.25, once: true }" class="summary-card high-demand">
          <div class="card-icon">{{ t('inventoryAlerts.cardIcons.highDemand') }}</div>
          <div class="card-info">
            <div class="card-value">{{ alertSummary?.highDemandCount || 0 }}</div>
            <div class="card-label">{{ t('inventoryAlerts.summary.highDemand') }}</div>
          </div>
        </div>
      </div>

      <div v-reveal="{ preset: 'section', delay: 0.08, once: true }" class="alerts-list">
        <div v-if="!alertSummary?.alerts || alertSummary.alerts.length === 0" class="no-alerts">
          <div class="no-alerts-icon">{{ t('inventoryAlerts.empty.icon') }}</div>
          <p>{{ t('inventoryAlerts.empty.text') }}</p>
        </div>

        <div v-else class="alert-items">
          <div
            v-for="(alert, index) in alertSummary.alerts"
            :key="alert.bookId"
            v-reveal="{ preset: 'card', delay: index * 0.05, once: true }"
            :class="['alert-item', alert.alertLevel.toLowerCase()]"
          >
            <div class="alert-badge" :class="alert.alertLevel.toLowerCase()">
              {{ t(`inventoryAlerts.alertBadges.${alert.alertLevel}`) }}
            </div>

            <div class="alert-book-info">
              <img
                v-if="alert.coverUrl"
                :src="alert.coverUrl"
                :alt="alert.title"
                class="book-cover"
              />
              <div v-else class="book-cover-placeholder">{{ t('inventoryAlerts.coverPlaceholder') }}</div>

              <div class="book-details">
                <h3 class="book-title">{{ alert.title }}</h3>
                <p class="book-author">{{ t('inventoryAlerts.bookInfo.author') }} {{ alert.author }}</p>
                <p class="book-category">{{ t('inventoryAlerts.bookInfo.category') }} {{ alert.category }}</p>
                <p class="book-isbn">{{ t('inventoryAlerts.bookInfo.isbn') }} {{ alert.isbn }}</p>
              </div>
            </div>

            <div class="alert-stats">
              <div class="stat-item">
                <span class="stat-label">{{ t('inventoryAlerts.alertStats.totalCopies') }}</span>
                <span class="stat-value">{{ alert.totalCopies }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">{{ t('inventoryAlerts.alertStats.available') }}</span>
                <span class="stat-value available">{{ alert.availableCopies }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">{{ t('inventoryAlerts.alertStats.borrowed') }}</span>
                <span class="stat-value borrowed">{{ alert.borrowedCopies }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">{{ t('inventoryAlerts.alertStats.borrowCount') }}</span>
                <span class="stat-value borrow-count">{{ alert.borrowCount }}</span>
              </div>
            </div>

            <div class="alert-message">
              <div class="alert-type-badge" :class="getAlertTypeClass(alert.alertType)">
                {{ t(`inventoryAlerts.alertType.${alert.alertType}`) }}
              </div>
              <p class="message-text">{{ alert.message }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { statisticsApi, type InventoryAlertSummary } from '../api/statisticsApi'
import PageHeader from '../components/layout/PageHeader.vue'
import { logger } from '../utils/logger'

const { t } = useI18n()

const loading = ref(true)
const alertSummary = ref<InventoryAlertSummary | null>(null)
const errorMessage = ref('')

onMounted(() => {
  loadAlerts()
})

async function loadAlerts() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await statisticsApi.getInventoryAlerts()
    if (response.data.success && response.data.data) {
      alertSummary.value = response.data.data
    } else {
      alertSummary.value = null
      errorMessage.value = response.data.message || t('inventoryAlerts.loadError')
    }
  } catch (error) {
    logger.error(t('inventoryAlerts.loadError') + ':', error)
    alertSummary.value = null
    errorMessage.value = t('inventoryAlerts.loadError')
  } finally {
    loading.value = false
  }
}

function emptySummary(): InventoryAlertSummary {
  return {
    totalAlerts: 0,
    criticalAlerts: 0,
    warningAlerts: 0,
    outOfStockCount: 0,
    lowStockCount: 0,
    highDemandCount: 0,
    alerts: []
  }
}

function getAlertTypeClass(type: string): string {
  const classMap: Record<string, string> = {
    OUT_OF_STOCK: 'out-of-stock',
    LOW_STOCK: 'low-stock',
    HIGH_DEMAND: 'high-demand'
  }
  return classMap[type] || ''
}
</script>

<style scoped>
.inventory-alerts {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.loading {
  text-align: center;
  padding: 5rem 1.5rem;
  font-size: 1rem;
  color: #718096;
  background: rgba(255, 255, 255, 0.82);
  border-radius: 1.25rem;
  box-shadow: var(--shadow-soft);
}

.alerts-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.summary-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-soft);
}

.summary-card.critical { border-left: 4px solid #e53e3e; }
.summary-card.warning { border-left: 4px solid #dd6b20; }
.summary-card.out-of-stock { border-left: 4px solid #c53030; }
.summary-card.low-stock { border-left: 4px solid #d69e2e; }
.summary-card.high-demand { border-left: 4px solid var(--primary); }

.card-icon {
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
}

.card-value {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--on-surface);
}

.card-label {
  font-size: 0.86rem;
  color: var(--on-surface-variant);
}

.alerts-list {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 1.5rem;
  box-shadow: var(--shadow-soft);
}

.no-alerts {
  text-align: center;
  padding: 4rem 1.5rem;
}

.no-alerts-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: rgba(0, 83, 219, 0.3);
}

.alert-items {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.alert-item {
  position: relative;
  border: 1px solid rgba(115, 124, 129, 0.1);
  border-radius: 1rem;
  padding: 1.25rem;
  background: rgba(248, 249, 251, 0.72);
}

.alert-item.critical {
  border-color: rgba(229, 62, 62, 0.25);
  background: rgba(255, 245, 245, 0.9);
}

.alert-item.warning {
  border-color: rgba(221, 107, 32, 0.22);
  background: rgba(255, 250, 240, 0.9);
}

.alert-badge {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  padding: 0.38rem 0.7rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 800;
  color: white;
}

.alert-badge.critical { background: #e53e3e; }
.alert-badge.warning { background: #dd6b20; }

.alert-book-info {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.book-cover,
.book-cover-placeholder {
  width: 78px;
  height: 118px;
  border-radius: 0.8rem;
}

.book-cover {
  object-fit: cover;
}

.book-cover-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(219, 225, 255, 0.92), rgba(217, 227, 244, 0.84));
  color: rgba(0, 83, 219, 0.38);
  font-family: var(--font-headline);
  font-size: 2rem;
}

.book-title {
  margin: 0 0 0.5rem;
  font-size: 1.15rem;
}

.book-author,
.book-category,
.book-isbn {
  margin: 0 0 0.35rem;
  color: var(--on-surface-variant);
}

.alert-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border-radius: 0.95rem;
  background: rgba(255, 255, 255, 0.86);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.stat-label {
  font-size: 0.76rem;
  color: var(--outline);
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 800;
}

.stat-value.available { color: #2f855a; }
.stat-value.borrowed { color: #c53030; }
.stat-value.borrow-count { color: var(--primary); }

.alert-message {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.alert-type-badge {
  padding: 0.42rem 0.72rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 800;
  color: white;
  white-space: nowrap;
}

.alert-type-badge.out-of-stock { background: #c53030; }
.alert-type-badge.low-stock { background: #dd6b20; }
.alert-type-badge.high-demand { background: var(--primary); }

.message-text {
  font-size: 0.92rem;
  color: var(--on-surface);
  font-weight: 600;
}

@media (max-width: 768px) {
  .alert-book-info {
    flex-direction: column;
  }

  .alert-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .alert-badge {
    position: static;
    display: inline-flex;
    margin-bottom: 0.75rem;
  }

  .alert-message {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
