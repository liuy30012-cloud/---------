export interface OfflineCacheMetadata {
  key: string
  value: unknown
  updatedAt: number
}

export type OfflineOperationType =
  | 'cache_update'
  | 'cache_clear'
  | 'metadata_update'
  | 'search_cache_refresh'
  | 'favorite_add'
  | 'favorite_remove'
  | 'borrow_apply'
  | 'borrow_pickup'
  | 'borrow_return'
  | 'borrow_renew'
  | 'reservation_create'
  | 'reservation_cancel'
  | 'reservation_pickup'
  | 'reservation_extend'
  | 'custom'

export type OfflineOperationStatus = 'pending' | 'syncing' | 'completed' | 'failed'

export interface OfflineOperationPayload {
  [key: string]: unknown
}

export interface OfflineOperation {
  id: string
  type: OfflineOperationType
  createdAt: number
  updatedAt: number
  status: OfflineOperationStatus
  retryCount: number
  payload?: OfflineOperationPayload
  errorMessage?: string | null
}

export interface OfflineCacheStats {
  bookCount: number
  hotBookCount: number
  cacheSize: number
  lastUpdate: number | null
}

export interface OfflineSyncState {
  isSyncing: boolean
  lastSyncAt: number | null
  lastSyncError: string | null
  pendingCount: number
}

export interface OfflineStatusSnapshot {
  isOnline: boolean
  initialized: boolean
  lastOnlineAt: number | null
  cacheStats: OfflineCacheStats
  syncState: OfflineSyncState
  queue: OfflineOperation[]
}

export type OfflineStatusListener = (snapshot: OfflineStatusSnapshot) => void

export interface QueueOperationInput {
  type: OfflineOperationType
  payload?: OfflineOperationPayload
}
