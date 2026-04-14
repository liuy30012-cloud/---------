<template>
  <Transition name="slide-down">
    <div v-if="!isOnline" class="offline-indicator">
      <div class="offline-content">
        <svg class="offline-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3" />
        </svg>
        <span class="offline-text">{{ t('offlineIndicator.offlineMode') }}</span>
        <span class="offline-hint">{{ t('offlineIndicator.limitedFeatures') }}</span>
      </div>
    </div>
  </Transition>

  <Transition name="fade">
    <div v-if="showOfflineNotice" class="offline-notice-overlay" @click="showOfflineNotice = false">
      <div class="offline-notice" @click.stop>
        <div class="notice-header">
          <svg class="notice-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <circle cx="12" cy="12" r="10" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01" />
          </svg>
          <h3>{{ t('offlineIndicator.networkRequired') }}</h3>
        </div>
        <p class="notice-message">{{ t('offlineIndicator.noticeMessage') }}</p>
        <button class="notice-button" @click="showOfflineNotice = false">{{ t('offlineIndicator.dismiss') }}</button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useNetworkStatus } from '@/composables/useNetworkStatus';

const { t } = useI18n();
const { isOnline, showOfflineNotice } = useNetworkStatus();
</script>

<style scoped>
.offline-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 8px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.offline-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  max-width: 1200px;
  margin: 0 auto;
}

.offline-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.offline-text {
  font-weight: 600;
  font-size: 14px;
}

.offline-hint {
  font-size: 12px;
  opacity: 0.9;
}

.offline-notice-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 20px;
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

.notice-button:active {
  transform: translateY(0);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-down-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
