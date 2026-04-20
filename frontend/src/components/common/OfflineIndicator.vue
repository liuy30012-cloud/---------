<template>
  <div class="offline-layer">
    <Transition name="slide-down">
      <div v-if="!isOnline" class="offline-indicator">
        <div class="offline-content">
          <div class="offline-badge">
            <svg class="offline-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3"
              />
            </svg>
            <span class="offline-text">{{ t('offlineIndicator.offlineMode') }}</span>
          </div>

          <span class="offline-hint">{{ t('offlineIndicator.limitedFeatures') }}</span>

          <button type="button" class="offline-manage" @click="panelVisible = !panelVisible">
            {{ panelVisible ? t('offlineIndicator.hideDetails') : t('offlineIndicator.showDetails') }}
          </button>
        </div>
      </div>
    </Transition>

    <Transition name="fade-up">
      <div v-if="panelVisible || hasSyncIssue" class="offline-panel">
        <div class="offline-panel__header">
          <div>
            <p class="offline-panel__eyebrow">{{ t('offlineIndicator.syncCenter') }}</p>
            <h3>{{ t('offlineIndicator.offlineWorkspace') }}</h3>
          </div>

          <button type="button" class="offline-panel__close" @click="panelVisible = false" :aria-label="t('offlineIndicator.dismiss')">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <SyncStatus :sync-state="syncState" @retry="handleRetry" />

        <dl class="offline-stats">
          <div>
            <dt>{{ t('offlineIndicator.pendingLabel') }}</dt>
            <dd>{{ syncState.pendingCount }}</dd>
          </div>
          <div>
            <dt>{{ t('offlineIndicator.cachedBooks') }}</dt>
            <dd>{{ cacheStats.bookCount }}</dd>
          </div>
          <div>
            <dt>{{ t('offlineIndicator.hotBooks') }}</dt>
            <dd>{{ cacheStats.hotBookCount }}</dd>
          </div>
        </dl>

        <p class="offline-panel__hint">{{ panelHint }}</p>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="showOfflineNotice" class="offline-notice-overlay" @click="showOfflineNotice = false">
        <div class="offline-notice" @click.stop>
          <div class="notice-header">
            <svg class="notice-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <circle cx="12" cy="12" r="10" stroke-width="2" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01" />
            </svg>
            <h3>{{ t('offlineIndicator.networkRequired') }}</h3>
          </div>
          <p class="notice-message">{{ t('offlineIndicator.noticeMessage') }}</p>
          <button class="notice-button" @click="showOfflineNotice = false">{{ t('offlineIndicator.dismiss') }}</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SyncStatus from '@/components/common/SyncStatus.vue'
import { useNetworkStatus } from '@/composables/useNetworkStatus'
import { useOffline } from '@/composables/useOffline'

const { t } = useI18n()
const { showOfflineNotice, manualUpdateCache } = useNetworkStatus()
const { isOnline, cacheStats, syncState, initialize } = useOffline()

const panelVisible = ref(false)
const hasSyncIssue = computed(() => Boolean(syncState.value.lastSyncError) || syncState.value.pendingCount > 0)
const panelHint = computed(() => {
  if (syncState.value.lastSyncError) {
    return t('offlineIndicator.retryHint')
  }

  if (!isOnline.value) {
    return t('offlineIndicator.queueHint')
  }

  return t('offlineIndicator.onlineHint')
})

const handleRetry = async () => {
  if (!isOnline.value) {
    return
  }

  await manualUpdateCache()
}

watch(hasSyncIssue, (nextValue) => {
  if (nextValue) {
    panelVisible.value = true
  }
})

onMounted(() => {
  initialize().catch(() => {})
})
</script>

<style scoped>
.offline-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  pointer-events: none;
}

.offline-indicator,
.offline-panel {
  pointer-events: auto;
}

.offline-indicator {
  padding: 0.85rem 1rem 0;
}

.offline-content {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  max-width: 1120px;
  margin: 0 auto;
  padding: 0.95rem 1.1rem;
  border-radius: 1.15rem;
  background: linear-gradient(135deg, rgba(168, 98, 57, 0.95), rgba(121, 64, 39, 0.96));
  color: #fff9f1;
  box-shadow: 0 18px 40px rgba(69, 33, 16, 0.28);
}

.offline-badge {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-weight: 700;
}

.offline-icon {
  width: 1.1rem;
  height: 1.1rem;
  flex-shrink: 0;
}

.offline-text {
  font-size: 0.92rem;
}

.offline-hint {
  flex: 1;
  min-width: 0;
  font-size: 0.8rem;
  opacity: 0.88;
}

.offline-manage {
  border: none;
  border-radius: 999px;
  background: rgba(255, 248, 239, 0.16);
  color: inherit;
  padding: 0.55rem 0.95rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.offline-panel {
  max-width: 1120px;
  margin: 0.7rem auto 0;
  padding: 0 1rem;
}

.offline-panel::before {
  content: '';
  display: block;
  border-radius: 1.25rem;
  background: linear-gradient(180deg, rgba(255, 251, 245, 0.97), rgba(250, 242, 232, 0.95));
  box-shadow: 0 22px 52px rgba(80, 54, 31, 0.16);
  position: absolute;
}

.offline-panel {
  position: relative;
}

.offline-panel > * {
  position: relative;
}

.offline-panel {
  padding: 1.05rem 1rem 1rem;
  border-radius: 1.25rem;
  background: linear-gradient(180deg, rgba(255, 251, 245, 0.97), rgba(250, 242, 232, 0.95));
  box-shadow: 0 22px 52px rgba(80, 54, 31, 0.16);
}

.offline-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.offline-panel__eyebrow {
  margin: 0 0 0.2rem;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9b7350;
}

.offline-panel__header h3 {
  margin: 0;
  font-size: 1rem;
  color: #49311e;
}

.offline-panel__close {
  border: none;
  background: rgba(112, 78, 50, 0.08);
  color: #6e4e32;
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.offline-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0.9rem 0 0;
}

.offline-stats div {
  padding: 0.9rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.72);
}

.offline-stats dt {
  font-size: 0.74rem;
  color: #8d6848;
}

.offline-stats dd {
  margin: 0.3rem 0 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #513622;
}

.offline-panel__hint {
  margin: 0.8rem 0 0;
  font-size: 0.8rem;
  color: #7c6149;
  line-height: 1.5;
}

.offline-notice-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 20px;
  pointer-events: auto;
}

.offline-notice {
  background: white;
  border-radius: 12px;
  padding: 24px;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.notice-icon {
  width: 24px;
  height: 24px;
  color: #f59e0b;
  flex-shrink: 0;
}

.notice-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.notice-message {
  margin: 0 0 20px 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

.notice-button {
  width: 100%;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.notice-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to,
.fade-up-enter-from,
.fade-up-leave-to {
  opacity: 0;
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: transform 0.28s ease, opacity 0.28s ease;
}

.fade-up-enter-from,
.fade-up-leave-to {
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .offline-content {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .offline-hint {
    flex-basis: 100%;
    order: 3;
  }

  .offline-stats {
    grid-template-columns: 1fr;
  }
}
</style>
