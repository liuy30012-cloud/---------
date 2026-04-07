import { computed, ref } from 'vue'
import httpClient from '../api/httpClient'
import { formatRelativeTime } from '../utils/timeHelpers'
import { logger } from '../utils/logger'

export interface NotificationItem {
  id: number
  type?: string
  icon: string
  titleZh: string
  titleEn: string
  descZh: string
  descEn: string
  targetPath?: string
  read: boolean
  createdAt: string
}

export function useNotifications(locale: { value: string }) {
  const notifications = ref<NotificationItem[]>([])
  const showNotifPanel = ref(false)

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  const getNotifTitle = (notif: NotificationItem) =>
    locale.value === 'zh' ? notif.titleZh : notif.titleEn

  const getNotifDesc = (notif: NotificationItem) =>
    locale.value === 'zh' ? notif.descZh : notif.descEn

  const formatNotifTime = (dateStr: string): string => {
    return formatRelativeTime(dateStr, locale.value === 'zh' ? 'zh-CN' : 'en')
  }

  const loadNotifications = async () => {
    try {
      const res = await httpClient.get<NotificationItem[]>('/api/notifications')
      notifications.value = res.data
    } catch (error) {
      logger.warn('Notifications API unavailable, returning empty list', error)
      notifications.value = []
    }
  }

  const toggleNotifPanel = (closeOthers?: () => void) => {
    closeOthers?.()
    showNotifPanel.value = !showNotifPanel.value
  }

  const markAllRead = async () => {
    try {
      await httpClient.put('/api/notifications/read-all')
    } finally {
      notifications.value = notifications.value.map(item => ({ ...item, read: true }))
    }
  }

  const markOneRead = async (id: number) => {
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.read = true
      try {
        await httpClient.put(`/api/notifications/${id}/read`)
      } catch {
        // local state already updated
      }
    }
  }

  return {
    notifications,
    showNotifPanel,
    unreadCount,
    getNotifTitle,
    getNotifDesc,
    formatNotifTime,
    loadNotifications,
    toggleNotifPanel,
    markAllRead,
    markOneRead,
  }
}
