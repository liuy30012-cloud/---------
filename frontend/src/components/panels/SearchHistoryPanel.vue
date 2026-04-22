<template>
  <div class="icon-btn-wrapper">
    <LibraryButton type="ghost" size="small" ref="historyBtnRef" @click="togglePanel">
      <span class="material-symbols-outlined" aria-hidden="true">history</span>
    </LibraryButton>
    <Transition name="popup">
      <div v-if="showPanel" ref="historyPanelRef" class="popup-panel history-panel">
        <div class="popup-header">
          <h4 class="popup-title">{{ $t('history.title') }}</h4>
          <LibraryButton v-if="searchHistory.length > 0" type="ghost" size="small" @click="$emit('clear-history')">
            {{ $t('history.clear') }}
          </LibraryButton>
        </div>
        <div class="popup-body">
          <div v-if="searchHistory.length === 0" class="popup-empty">
            <span class="material-symbols-outlined empty-icon">manage_search</span>
            <p>{{ $t('history.empty') }}</p>
          </div>
          <div
            v-for="item in searchHistory"
            :key="item.keyword + item.timestamp.getTime()"
            class="history-item"
            @click="$emit('re-search', item.keyword)"
          >
            <span class="material-symbols-outlined history-item-icon">search</span>
            <div class="history-item-content">
              <span class="history-keyword">{{ item.keyword }}</span>
              <span class="history-meta">{{ item.resultCount }} {{ $t('history.results') }} · {{ formatHistoryTime(item.timestamp) }}</span>
            </div>
            <span class="material-symbols-outlined history-arrow">north_west</span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import LibraryButton from '@/components/common/LibraryButton.vue'

interface SearchHistoryItem {
  keyword: string
  resultCount: number
  timestamp: Date
}

defineProps<{
  searchHistory: SearchHistoryItem[]
}>()

defineEmits(['clear-history', 're-search'])

const { t } = useI18n()
const showPanel = ref(false)
const historyBtnRef = ref<InstanceType<typeof LibraryButton>>()
const historyPanelRef = ref<HTMLElement>()

const togglePanel = () => {
  showPanel.value = !showPanel.value
}

const formatHistoryTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return t('history.justNow')
  if (minutes < 60) return t('history.minutesAgo', { n: minutes })
  if (hours < 24) return t('history.hoursAgo', { n: hours })
  return t('history.daysAgo', { n: days })
}

const handleClickOutside = (event: MouseEvent) => {
  if (
    showPanel.value &&
    historyPanelRef.value &&
    historyBtnRef.value &&
    !historyPanelRef.value.contains(event.target as Node) &&
    !(historyBtnRef.value.$el as HTMLElement).contains(event.target as Node)
  ) {
    showPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.icon-btn-wrapper {
  position: relative;
}

.popup-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 360px;
  max-height: 480px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 1001;
}

.popup-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.popup-title {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.popup-body {
  max-height: 400px;
  overflow-y: auto;
}

.popup-empty {
  padding: 48px 20px;
  text-align: center;
  color: #a0aec0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.popup-empty p {
  margin: 0;
  font-size: 14px;
}

.history-item {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f7fafc;
}

.history-item:hover {
  background: #f7fafc;
}

.history-item-icon {
  font-size: 20px;
  color: #a0aec0;
  flex-shrink: 0;
}

.history-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-keyword {
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  font-size: 12px;
  color: #a0aec0;
}

.history-arrow {
  font-size: 18px;
  color: #cbd5e0;
  flex-shrink: 0;
}

.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
