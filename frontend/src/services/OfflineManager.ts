import { offlineDB } from '@/utils/offlineDB'
import { logger } from '@/utils/logger'
import type {
  OfflineCacheStats,
  OfflineOperation,
  OfflineStatusListener,
  OfflineStatusSnapshot,
  OfflineSyncState,
  QueueOperationInput,
} from '@/types/offline'

const METADATA_KEYS = {
  queue: 'offline_operation_queue',
  syncState: 'offline_sync_state',
  lastOnlineAt: 'offline_last_online_at',
} as const

const createDefaultCacheStats = (): OfflineCacheStats => ({
  bookCount: 0,
  hotBookCount: 0,
  cacheSize: 0,
  lastUpdate: null,
})

const createDefaultSyncState = (): OfflineSyncState => ({
  isSyncing: false,
  lastSyncAt: null,
  lastSyncError: null,
  pendingCount: 0,
})

class OfflineManager {
  private listeners = new Set<OfflineStatusListener>()
  private isInitialized = false
  private isListening = false
  private snapshot: OfflineStatusSnapshot = {
    isOnline: typeof navigator !== 'undefined' ? navigator.onLine : true,
    initialized: false,
    lastOnlineAt: null,
    cacheStats: createDefaultCacheStats(),
    syncState: createDefaultSyncState(),
    queue: [],
  }

  async init(): Promise<OfflineStatusSnapshot> {
    if (this.isInitialized) {
      this.ensureNetworkListeners()
      return this.getSnapshot()
    }

    this.ensureNetworkListeners()

    try {
      const [queue, persistedSyncState, lastOnlineAt] = await Promise.all([
        this.readQueue(),
        offlineDB.getMetadata(METADATA_KEYS.syncState),
        offlineDB.getMetadata(METADATA_KEYS.lastOnlineAt),
      ])

      this.snapshot.queue = queue
      this.snapshot.lastOnlineAt = typeof lastOnlineAt === 'number' ? lastOnlineAt : null
      this.snapshot.syncState = {
        ...createDefaultSyncState(),
        ...(this.isSyncState(persistedSyncState) ? persistedSyncState : {}),
        pendingCount: queue.filter((operation) => operation.status === 'pending' || operation.status === 'failed').length,
      }

      await this.refreshCacheStats()
      this.isInitialized = true
      this.snapshot.initialized = true
      await this.persistSyncState()
      this.emit()
    } catch (error) {
      logger.error('[OfflineManager] 初始化失败:', error)
      this.isInitialized = true
      this.snapshot.initialized = true
      this.emit()
    }

    return this.getSnapshot()
  }

  subscribe(listener: OfflineStatusListener): () => void {
    this.listeners.add(listener)
    listener(this.getSnapshot())

    if (!this.isInitialized) {
      this.init().catch((error) => {
        logger.error('[OfflineManager] 订阅初始化失败:', error)
      })
    }

    return () => {
      this.listeners.delete(listener)
    }
  }

  async refreshCacheStats(): Promise<OfflineCacheStats> {
    try {
      const [stats, lastUpdate] = await Promise.all([
        offlineDB.getCacheStats(),
        offlineDB.getMetadata('hot_books_last_update'),
      ])

      this.snapshot.cacheStats = {
        ...stats,
        lastUpdate,
      }
    } catch (error) {
      logger.error('[OfflineManager] 获取缓存统计失败:', error)
      this.snapshot.cacheStats = createDefaultCacheStats()
    }

    this.emit()
    return this.snapshot.cacheStats
  }

  async enqueueOperation(input: QueueOperationInput): Promise<OfflineOperation> {
    await this.init()

    const now = Date.now()
    const operation: OfflineOperation = {
      id: this.createOperationId(),
      type: input.type,
      createdAt: now,
      updatedAt: now,
      status: 'pending',
      retryCount: 0,
      payload: input.payload,
    }

    this.snapshot.queue = [...this.snapshot.queue, operation]
    await this.persistQueue()
    this.updatePendingCount()
    this.emit()

    return operation
  }

  async dequeueOperation(operationId?: string): Promise<OfflineOperation | null> {
    await this.init()

    if (!this.snapshot.queue.length) {
      return null
    }

    let removed: OfflineOperation | null = null

    if (operationId) {
      const index = this.snapshot.queue.findIndex((operation) => operation.id === operationId)
      if (index === -1) {
        return null
      }
      removed = this.snapshot.queue[index]
      this.snapshot.queue = this.snapshot.queue.filter((operation) => operation.id !== operationId)
    } else {
      const [firstOperation, ...rest] = this.snapshot.queue
      removed = firstOperation ?? null
      this.snapshot.queue = rest
    }

    await this.persistQueue()
    this.updatePendingCount()
    this.emit()

    return removed
  }

  async clearQueue(): Promise<void> {
    await this.init()
    this.snapshot.queue = []
    await this.persistQueue()
    this.updatePendingCount()
    this.emit()
  }

  async markOperationSyncing(operationId: string): Promise<OfflineOperation | null> {
    const updated = await this.updateOperation(operationId, (operation) => ({
      ...operation,
      status: 'syncing',
      updatedAt: Date.now(),
      errorMessage: null,
    }))

    if (updated) {
      await this.markSyncState({
        isSyncing: true,
        lastSyncError: null,
      })
    }

    return updated
  }

  async markOperationCompleted(operationId: string): Promise<OfflineOperation | null> {
    const updated = await this.updateOperation(operationId, (operation) => ({
      ...operation,
      status: 'completed',
      updatedAt: Date.now(),
      errorMessage: null,
    }))

    if (updated) {
      this.snapshot.queue = this.snapshot.queue.filter((operation) => operation.id !== operationId)
      await this.persistQueue()
      this.updatePendingCount()
      await this.markSyncState({
        isSyncing: false,
        lastSyncAt: updated.updatedAt,
        lastSyncError: null,
      })
      this.emit()
    }

    return updated
  }

  async markOperationFailed(operationId: string, errorMessage: string): Promise<OfflineOperation | null> {
    const updated = await this.updateOperation(operationId, (operation) => ({
      ...operation,
      status: 'failed',
      updatedAt: Date.now(),
      retryCount: operation.retryCount + 1,
      errorMessage,
    }))

    if (updated) {
      await this.markSyncState({
        isSyncing: false,
        lastSyncError: errorMessage,
      })
    }

    return updated
  }

  async markSyncState(patch: Partial<OfflineSyncState>): Promise<OfflineSyncState> {
    await this.init()

    const newPendingCount = this.snapshot.queue.filter((operation) => operation.status === 'pending' || operation.status === 'failed').length
    const pendingCountChanged = this.snapshot.syncState.pendingCount !== newPendingCount

    this.snapshot.syncState = {
      ...this.snapshot.syncState,
      ...patch,
      pendingCount: newPendingCount,
    }

    if (pendingCountChanged || Object.keys(patch).length > 0) {
      await this.persistSyncState()
    }
    this.emit()
    return this.snapshot.syncState
  }

  getSnapshot(): OfflineStatusSnapshot {
    return {
      ...this.snapshot,
      cacheStats: { ...this.snapshot.cacheStats },
      syncState: { ...this.snapshot.syncState },
      queue: this.snapshot.queue.map((operation) => ({ ...operation })),
    }
  }

  private ensureNetworkListeners(): void {
    if (this.isListening || typeof window === 'undefined') {
      return
    }

    window.addEventListener('online', this.handleOnline)
    window.addEventListener('offline', this.handleOffline)
    this.isListening = true
  }

  private handleOnline = (): void => {
    const lastOnlineAt = Date.now()
    this.snapshot.isOnline = true
    this.snapshot.lastOnlineAt = lastOnlineAt
    offlineDB.setMetadata(METADATA_KEYS.lastOnlineAt, lastOnlineAt).catch((error) => {
      logger.error('[OfflineManager] 保存最近在线时间失败:', error)
    })
    this.emit()
  }

  private handleOffline = (): void => {
    this.snapshot.isOnline = false
    this.emit()
  }

  private async readQueue(): Promise<OfflineOperation[]> {
    const queue = await offlineDB.getMetadata(METADATA_KEYS.queue)
    if (!Array.isArray(queue)) {
      return []
    }

    return queue.filter(this.isOfflineOperation)
  }

  private async persistQueue(): Promise<void> {
    await offlineDB.setMetadata(METADATA_KEYS.queue, this.snapshot.queue)
  }

  private async persistSyncState(): Promise<void> {
    await offlineDB.setMetadata(METADATA_KEYS.syncState, this.snapshot.syncState)
  }

  private updatePendingCount(): void {
    const newPendingCount = this.snapshot.queue.filter((operation) => operation.status === 'pending' || operation.status === 'failed').length
    if (this.snapshot.syncState.pendingCount !== newPendingCount) {
      this.snapshot.syncState.pendingCount = newPendingCount
      this.persistSyncState().catch((error) => {
        logger.error('[OfflineManager] 持久化同步状态失败:', error)
      })
    }
  }

  private async updateOperation(
    operationId: string,
    updater: (operation: OfflineOperation) => OfflineOperation,
  ): Promise<OfflineOperation | null> {
    await this.init()

    let updatedOperation: OfflineOperation | null = null
    this.snapshot.queue = this.snapshot.queue.map((operation) => {
      if (operation.id !== operationId) {
        return operation
      }

      updatedOperation = updater(operation)
      return updatedOperation
    })

    if (!updatedOperation) {
      return null
    }

    await this.persistQueue()
    this.updatePendingCount()
    this.emit()
    return updatedOperation
  }

  private createOperationId(): string {
    return `offline-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  }

  private isOfflineOperation = (value: unknown): value is OfflineOperation => {
    if (!value || typeof value !== 'object') {
      return false
    }

    const candidate = value as Partial<OfflineOperation>
    return (
      typeof candidate.id === 'string'
      && typeof candidate.type === 'string'
      && typeof candidate.createdAt === 'number'
      && typeof candidate.updatedAt === 'number'
      && typeof candidate.status === 'string'
      && typeof candidate.retryCount === 'number'
    )
  }

  private isSyncState(value: unknown): value is Partial<OfflineSyncState> {
    return Boolean(value) && typeof value === 'object'
  }

  private emit(): void {
    const snapshot = this.getSnapshot()
    this.listeners.forEach((listener) => listener(snapshot))
  }
}

export const offlineManager = new OfflineManager()
