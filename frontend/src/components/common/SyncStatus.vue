<template>
  <div class="sync-status" :class="`sync-status--${tone}`">
    <div class="sync-status__icon" aria-hidden="true">
      <span class="material-symbols-outlined">{{ icon }}</span>
    </div>

    <div class="sync-status__body">
      <p class="sync-status__title">{{ title }}</p>
      <p class="sync-status__meta">{{ description }}</p>
    </div>

    <button
      v-if="showRetry"
      type="button"
      class="sync-status__action"
      @click="$emit('retry')"
    >
      {{ t('offlineIndicator.retrySync') }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OfflineSyncState } from '@/types/offline'

const props = defineProps<{
  syncState: OfflineSyncState
}>()

defineEmits<{
  retry: []
}>()

const { t, d } = useI18n()

const showRetry = computed(() => Boolean(props.syncState.lastSyncError))

const tone = computed(() => {
  if (props.syncState.isSyncing) {
    return 'syncing'
  }

  if (props.syncState.lastSyncError) {
    return 'error'
  }

  if (props.syncState.pendingCount > 0) {
    return 'pending'
  }

  return 'idle'
})

const icon = computed(() => {
  if (props.syncState.isSyncing) {
    return 'sync'
  }

  if (props.syncState.lastSyncError) {
    return 'sync_problem'
  }

  if (props.syncState.pendingCount > 0) {
    return 'schedule'
  }

  return 'cloud_done'
})

const title = computed(() => {
  if (props.syncState.isSyncing) {
    return t('offlineIndicator.syncing')
  }

  if (props.syncState.lastSyncError) {
    return t('offlineIndicator.syncFailed')
  }

  if (props.syncState.pendingCount > 0) {
    return t('offlineIndicator.pendingChanges', { count: props.syncState.pendingCount })
  }

  return t('offlineIndicator.synced')
})

const description = computed(() => {
  if (props.syncState.isSyncing) {
    return t('offlineIndicator.syncingHint')
  }

  if (props.syncState.lastSyncError) {
    return props.syncState.lastSyncError
  }

  if (props.syncState.lastSyncAt) {
    return t('offlineIndicator.lastSyncedAt', {
      time: d(new Date(props.syncState.lastSyncAt), 'short'),
    })
  }

  return t('offlineIndicator.readyWhenOnline')
})
</script>

<style scoped>
.sync-status {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 248, 238, 0.95);
  border: 1px solid rgba(133, 99, 71, 0.12);
  box-shadow: 0 12px 32px rgba(74, 53, 36, 0.12);
  color: #573e2a;
}

.sync-status--syncing {
  background: rgba(240, 248, 255, 0.95);
  border-color: rgba(61, 122, 178, 0.18);
  color: #24506f;
}

.sync-status--error {
  background: rgba(255, 243, 239, 0.97);
  border-color: rgba(193, 94, 66, 0.2);
  color: #7a3424;
}

.sync-status--pending {
  background: rgba(255, 248, 230, 0.97);
  border-color: rgba(176, 132, 45, 0.2);
  color: #7a5a11;
}

.sync-status__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

.sync-status--syncing .sync-status__icon .material-symbols-outlined {
  animation: sync-status-spin 1.2s linear infinite;
}

.sync-status__body {
  min-width: 0;
  flex: 1;
}

.sync-status__title,
.sync-status__meta {
  margin: 0;
}

.sync-status__title {
  font-size: 0.92rem;
  font-weight: 700;
}

.sync-status__meta {
  margin-top: 0.18rem;
  font-size: 0.78rem;
  opacity: 0.82;
  line-height: 1.45;
}

.sync-status__action {
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: inherit;
  padding: 0.5rem 0.9rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

@keyframes sync-status-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

</style>
