import { ref, onMounted, onUnmounted } from 'vue'
import { offlineDB } from '@/utils/offlineDB'
import { offlineManager } from '@/services/OfflineManager'
import { logger } from '../utils/logger'
import { syncOfflineOperation } from './useOfflineSync'
import { getErrorMessage } from '@/utils/errorHelpers'
import type { OfflineOperation } from '@/types/offline'

export function useNetworkStatus() {
  const isOnline = ref(typeof navigator !== 'undefined' ? navigator.onLine : true)
  const showOfflineNotice = ref(false)
  const lastOnlineTime = ref<number>(Date.now())

  let unsubscribeOfflineManager: (() => void) | null = null

  const handleOnline = async () => {
    isOnline.value = true
    showOfflineNotice.value = false
    await offlineManager.markSyncState({ isSyncing: false, lastSyncError: null })

    logger.log('[Network] 网络已恢复')

    try {
      await updateHotBooksCache()
      logger.log('[Network] 热门书籍缓存已更新')
    } catch (error) {
      logger.error('[Network] 更新缓存失败:', error)
    }

    // 同步离线队列
    try {
      await syncOfflineQueue()
      logger.log('[Network] 离线队列同步完成')
    } catch (error) {
      logger.error('[Network] 离线队列同步失败:', error)
    }

    lastOnlineTime.value = Date.now()
  }

  const handleOffline = () => {
    isOnline.value = false
    offlineManager.markSyncState({ isSyncing: false }).catch((error) => {
      logger.error('[Network] 更新离线同步状态失败:', error)
    })
    logger.log('[Network] 网络已断开')
  }

  const updateHotBooksCache = async (): Promise<void> => {
    if (!('serviceWorker' in navigator)) {
      return
    }

    const registration = await navigator.serviceWorker.ready
    const activeWorker = registration.active
    if (!activeWorker) {
      return
    }

    await offlineManager.markSyncState({ isSyncing: true, lastSyncError: null })

    return new Promise((resolve, reject) => {
      const messageChannel = new MessageChannel()

      messageChannel.port1.onmessage = (event) => {
        if (event.data.success) {
          offlineManager.markSyncState({
            isSyncing: false,
            lastSyncAt: Date.now(),
            lastSyncError: null,
          }).catch((error) => {
            logger.error('[Network] 更新同步状态失败:', error)
          })
          offlineManager.refreshCacheStats().catch((error) => {
            logger.error('[Network] 刷新缓存统计失败:', error)
          })
          resolve()
        } else {
          const nextError = event.data.error || '缓存更新失败'
          offlineManager.markSyncState({
            isSyncing: false,
            lastSyncError: nextError,
          }).catch((error) => {
            logger.error('[Network] 更新同步状态失败:', error)
          })
          reject(new Error(nextError))
        }
      }

      activeWorker.postMessage(
        { type: 'UPDATE_CACHE' },
        [messageChannel.port2],
      )
    })
  }

  const manualUpdateCache = async (): Promise<boolean> => {
    if (!isOnline.value) {
      return false
    }

    try {
      await updateHotBooksCache()
      return true
    } catch (error) {
      logger.error('[Network] 手动更新缓存失败:', error)
      return false
    }
  }

  const clearAllCache = async (): Promise<boolean> => {
    try {
      await offlineDB.clearAllCache()
      await offlineManager.clearQueue()
      await offlineManager.refreshCacheStats()

      if ('serviceWorker' in navigator) {
        const registration = await navigator.serviceWorker.ready
        const activeWorker = registration.active
        if (activeWorker) {
          return new Promise((resolve) => {
            const messageChannel = new MessageChannel()

            messageChannel.port1.onmessage = (event) => {
              resolve(event.data.success)
            }

            activeWorker.postMessage(
              { type: 'CLEAR_CACHE' },
              [messageChannel.port2],
            )
          })
        }
      }

      return true
    } catch (error) {
      logger.error('[Network] 清空缓存失败:', error)
      return false
    }
  }

  const checkOfflineAction = (actionName: string): boolean => {
    if (!isOnline.value) {
      showOfflineNotice.value = true
      logger.warn(`[Network] 操作 "${actionName}" 需要网络连接`)
      return false
    }
    return true
  }

  const getCacheStats = async () => {
    try {
      return await offlineManager.refreshCacheStats()
    } catch (error) {
      logger.error('[Network] 获取缓存统计失败:', error)
      return {
        bookCount: 0,
        hotBookCount: 0,
        cacheSize: 0,
        lastUpdate: null,
      }
    }
  }

  const syncOfflineQueue = async (): Promise<void> => {
    const snapshot = offlineManager.getSnapshot()
    const pendingOps = snapshot.queue.filter((op: OfflineOperation) => op.status === 'pending')

    if (pendingOps.length === 0) {
      logger.log('[Network] 离线队列为空，无需同步')
      return
    }

    logger.log(`[Network] 开始同步 ${pendingOps.length} 个离线操作`)
    await offlineManager.markSyncState({ isSyncing: true, lastSyncError: null })

    let successCount = 0
    let failCount = 0

    await Promise.allSettled(
      pendingOps.map(async (op) => {
        try {
          await syncOfflineOperation(op)
          await offlineManager.markOperationCompleted(op.id)
          successCount++
          logger.log(`[Network] 操作 ${op.id} (${op.type}) 同步成功`)
        } catch (error) {
          failCount++
          const errorMsg = getErrorMessage(error, '未知错误')
          await offlineManager.markOperationFailed(op.id, errorMsg)
          logger.error(`[Network] 操作 ${op.id} (${op.type}) 同步失败:`, error)
        }
      })
    )

    await offlineManager.markSyncState({
      isSyncing: false,
      lastSyncAt: Date.now(),
      lastSyncError: failCount > 0 ? `${failCount} 个操作同步失败` : null,
    })

    logger.log(`[Network] 队列同步完成: 成功 ${successCount}, 失败 ${failCount}`)
  }

  onMounted(() => {
    unsubscribeOfflineManager = offlineManager.subscribe((snapshot) => {
      isOnline.value = snapshot.isOnline
    })

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    if (isOnline.value) {
      updateHotBooksCache().catch((e) => logger.error('[Network] initial cache update failed:', e))
    }
  })

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
    unsubscribeOfflineManager?.()
    unsubscribeOfflineManager = null
  })

  return {
    isOnline,
    showOfflineNotice,
    lastOnlineTime,
    checkOfflineAction,
    manualUpdateCache,
    clearAllCache,
    getCacheStats,
  }
}
