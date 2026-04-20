import { computed, onMounted, onUnmounted, ref } from 'vue'
import { offlineManager } from '@/services/OfflineManager'
import type {
  OfflineOperation,
  OfflineStatusSnapshot,
  OfflineSyncState,
  QueueOperationInput,
} from '@/types/offline'

export function useOffline() {
  const snapshot = ref<OfflineStatusSnapshot>(offlineManager.getSnapshot())
  const isReady = ref(snapshot.value.initialized)
  let unsubscribe: (() => void) | null = null

  const bindManager = () => {
    unsubscribe = offlineManager.subscribe((nextSnapshot) => {
      snapshot.value = nextSnapshot
      isReady.value = nextSnapshot.initialized
    })
  }

  onMounted(() => {
    if (!unsubscribe) {
      bindManager()
    }
  })

  onUnmounted(() => {
    unsubscribe?.()
    unsubscribe = null
  })

  const initialize = async (): Promise<OfflineStatusSnapshot> => {
    const nextSnapshot = await offlineManager.init()
    snapshot.value = nextSnapshot
    isReady.value = nextSnapshot.initialized
    return nextSnapshot
  }

  const refreshCacheStats = async () => offlineManager.refreshCacheStats()

  const enqueueOperation = async (input: QueueOperationInput): Promise<OfflineOperation> => {
    return offlineManager.enqueueOperation(input)
  }

  const dequeueOperation = async (operationId?: string) => {
    return offlineManager.dequeueOperation(operationId)
  }

  const clearQueue = async () => {
    await offlineManager.clearQueue()
  }

  const markSyncState = async (patch: Partial<OfflineSyncState>) => {
    return offlineManager.markSyncState(patch)
  }

  const markOperationSyncing = async (operationId: string) => {
    return offlineManager.markOperationSyncing(operationId)
  }

  const markOperationCompleted = async (operationId: string) => {
    return offlineManager.markOperationCompleted(operationId)
  }

  const markOperationFailed = async (operationId: string, errorMessage: string) => {
    return offlineManager.markOperationFailed(operationId, errorMessage)
  }

  return {
    isReady,
    snapshot: computed(() => snapshot.value),
    isOnline: computed(() => snapshot.value.isOnline),
    lastOnlineAt: computed(() => snapshot.value.lastOnlineAt),
    cacheStats: computed(() => snapshot.value.cacheStats),
    syncState: computed(() => snapshot.value.syncState),
    queue: computed(() => snapshot.value.queue),
    pendingOperations: computed(() => snapshot.value.queue.filter((operation) => operation.status === 'pending' || operation.status === 'failed')),
    initialize,
    refreshCacheStats,
    enqueueOperation,
    dequeueOperation,
    clearQueue,
    markSyncState,
    markOperationSyncing,
    markOperationCompleted,
    markOperationFailed,
  }
}
