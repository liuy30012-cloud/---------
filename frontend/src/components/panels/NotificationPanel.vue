<template>
  <div class="icon-btn-wrapper">
    <button class="icon-btn" ref="notifBtnRef" @click="togglePanel">
      <span class="material-symbols-outlined">notifications</span>
      <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
    </button>
    <Transition name="popup">
      <div v-if="showPanel" ref="notifPanelRef" class="popup-panel notif-panel">
        <div class="popup-header">
          <h4 class="popup-title">{{ $t('notifications.title') }}</h4>
          <button v-if="unreadCount > 0" class="popup-action-btn" @click="$emit('mark-all-read')">
            {{ $t('notifications.markAllRead') }}
          </button>
        </div>
        <div class="popup-body">
          <div
            v-for="notif in notifications"
            :key="notif.id"
            :class="['notif-item', { 'notif-read': notif.read }]"
            @click="$emit('mark-one-read', notif.id)"
          >
            <span class="material-symbols-outlined notif-icon">{{ notif.icon }}</span>
            <div class="notif-content">
              <p class="notif-title-text">{{ getNotifTitle(notif) }}</p>
              <p class="notif-desc">{{ getNotifDesc(notif) }}</p>
              <span class="notif-time">{{ formatNotifTime(notif.createdAt) }}</span>
            </div>
            <span v-if="!notif.read" class="notif-dot"></span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

interface Notification {
  id: number
  type: string
  message: string
  createdAt: Date
  read: boolean
  icon: string
}

const props = defineProps<{
  notifications: Notification[]
}>()

const emit = defineEmits(['mark-all-read', 'mark-one-read'])

const { t } = useI18n()
const showPanel = ref(false)
const notifBtnRef = ref<HTMLElement>()
const notifPanelRef = ref<HTMLElement>()

const unreadCount = computed(() => {
  return props.notifications.filter(n => !n.read).length
})

const togglePanel = () => {
  showPanel.value = !showPanel.value
}

const getNotifTitle = (notif: Notification) => {
  return t(`notifications.types.${notif.type}`)
}

const getNotifDesc = (notif: Notification) => {
  return notif.message
}

const formatNotifTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return t('notifications.justNow')
  if (minutes < 60) return t('notifications.minutesAgo', { n: minutes })
  if (hours < 24) return t('notifications.hoursAgo', { n: hours })
  return t('notifications.daysAgo', { n: days })
}

const handleClickOutside = (event: MouseEvent) => {
  if (
    showPanel.value &&
    notifPanelRef.value &&
    notifBtnRef.value &&
    !notifPanelRef.value.contains(event.target as Node) &&
    !notifBtnRef.value.contains(event.target as Node)
  ) {
    showPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside, { capture: true })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, { capture: true })
})
</script>

<style scoped>
.icon-btn-wrapper {
  position: relative;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #4a5568;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
}

.icon-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.unread-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: #f56565;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
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

.popup-action-btn {
  background: none;
  border: none;
  color: #667eea;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.popup-action-btn:hover {
  background: rgba(102, 126, 234, 0.1);
}

.popup-body {
  max-height: 400px;
  overflow-y: auto;
}

.notif-item {
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f7fafc;
  position: relative;
}

.notif-item:hover {
  background: #f7fafc;
}

.notif-item.notif-read {
  opacity: 0.6;
}

.notif-icon {
  font-size: 24px;
  color: #667eea;
  flex-shrink: 0;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 4px 0;
}

.notif-desc {
  font-size: 13px;
  color: #718096;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notif-time {
  font-size: 12px;
  color: #a0aec0;
}

.notif-dot {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
}

.popup-enter-active,
.popup-leave-active {
  transition: all 0.2s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
