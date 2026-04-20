import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const offlineDBMock = vi.hoisted(() => ({
  clearAllCache: vi.fn(),
}))

const loggerMock = vi.hoisted(() => ({
  log: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}))

const activeWorker = vi.hoisted(() => ({
  postMessage: vi.fn(),
}))

const serviceWorkerRegistration = vi.hoisted(() => ({
  active: activeWorker,
}))

const offlineManagerMock = vi.hoisted(() => ({
  subscribe: vi.fn(),
  markSyncState: vi.fn(),
  refreshCacheStats: vi.fn(),
  clearQueue: vi.fn(),
}))

vi.mock('@/utils/offlineDB', () => ({
  offlineDB: offlineDBMock,
}))

vi.mock('@/services/OfflineManager', () => ({
  offlineManager: offlineManagerMock,
}))

vi.mock('@/utils/logger', () => ({
  logger: loggerMock,
}))

const hostState: {
  api: {
    isOnline: { value: boolean }
    showOfflineNotice: { value: boolean }
    manualUpdateCache: () => Promise<boolean>
    clearAllCache: () => Promise<boolean>
    checkOfflineAction: (actionName: string) => boolean
    getCacheStats: () => Promise<{
      bookCount: number
      hotBookCount: number
      cacheSize: number
      lastUpdate: number | null
    }>
  } | null
} = {
  api: null,
}

const mountComposable = async () => {
  const module = await import('@/composables/useNetworkStatus')
  const Host = defineComponent({
    setup() {
      hostState.api = module.useNetworkStatus()
      return () => null
    },
  })

  return mount(Host)
}

describe('useNetworkStatus', () => {
  let unsubscribe: ReturnType<typeof vi.fn>
  let onlineValue = true

  beforeEach(() => {
    hostState.api = null
    unsubscribe = vi.fn()
    onlineValue = true

    vi.clearAllMocks()

    offlineManagerMock.subscribe.mockImplementation((listener: (snapshot: { isOnline: boolean }) => void) => {
      listener({ isOnline: onlineValue })
      return unsubscribe
    })
    offlineManagerMock.markSyncState.mockResolvedValue(undefined)
    offlineManagerMock.refreshCacheStats.mockResolvedValue({
      bookCount: 2,
      hotBookCount: 1,
      cacheSize: 128,
      lastUpdate: 1000,
    })
    offlineManagerMock.clearQueue.mockResolvedValue(undefined)
    offlineDBMock.clearAllCache.mockResolvedValue(undefined)

    Object.defineProperty(window.navigator, 'onLine', {
      configurable: true,
      get: () => onlineValue,
    })

    Object.defineProperty(window.navigator, 'serviceWorker', {
      configurable: true,
      value: {
        ready: Promise.resolve(serviceWorkerRegistration),
      },
    })

    activeWorker.postMessage.mockImplementation((message: { type: string }, ports?: Array<{ postMessage?: (data: { success: boolean; error?: string }) => void }>) => {
      const port = ports?.[0]
      if (!port || typeof port.postMessage !== 'function') {
        return
      }

      if (message.type === 'UPDATE_CACHE' || message.type === 'CLEAR_CACHE') {
        port.postMessage({ success: true })
      }
    })
  })

  afterEach(() => {
    hostState.api = null
  })

  it('initializes from navigator and subscribes on mount', async () => {
    const wrapper = await mountComposable()
    await Promise.resolve()
    await nextTick()

    expect(offlineManagerMock.subscribe).toHaveBeenCalledTimes(1)
    expect(hostState.api?.isOnline.value).toBe(true)
    expect(activeWorker.postMessage).toHaveBeenCalledWith(
      { type: 'UPDATE_CACHE' },
      expect.any(Array),
    )

    wrapper.unmount()
    expect(unsubscribe).toHaveBeenCalledTimes(1)
  })

  it('handles offline and online browser events', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    onlineValue = false
    window.dispatchEvent(new Event('offline'))
    await nextTick()

    expect(hostState.api?.isOnline.value).toBe(false)
    expect(offlineManagerMock.markSyncState).toHaveBeenCalledWith({ isSyncing: false })

    onlineValue = true
    window.dispatchEvent(new Event('online'))
    await Promise.resolve()
    await nextTick()

    expect(hostState.api?.isOnline.value).toBe(true)
    expect(hostState.api?.showOfflineNotice.value).toBe(false)
    expect(offlineManagerMock.markSyncState).toHaveBeenCalledWith({ isSyncing: false, lastSyncError: null })
  })

  it('returns false for manual update when offline', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    onlineValue = false
    window.dispatchEvent(new Event('offline'))
    await nextTick()

    await expect(hostState.api?.manualUpdateCache()).resolves.toBe(false)
    expect(activeWorker.postMessage).toHaveBeenCalledTimes(1)
  })

  it('returns true for manual update when service worker succeeds', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    await expect(hostState.api?.manualUpdateCache()).resolves.toBe(true)
    expect(offlineManagerMock.refreshCacheStats).toHaveBeenCalled()
  })

  it('returns false for manual update when service worker fails', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    activeWorker.postMessage.mockImplementationOnce((message: { type: string }, ports?: Array<{ postMessage?: (data: { success: boolean; error?: string }) => void }>) => {
      const port = ports?.[0]
      if (message.type === 'UPDATE_CACHE' && port?.postMessage) {
        port.postMessage({ success: false, error: 'sync failed' })
      }
    })

    await expect(hostState.api?.manualUpdateCache()).resolves.toBe(false)
    expect(offlineManagerMock.markSyncState).toHaveBeenCalledWith({
      isSyncing: false,
      lastSyncError: 'sync failed',
    })
  })

  it('clears cache and queue through offline managers', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    await expect(hostState.api?.clearAllCache()).resolves.toBe(true)
    expect(offlineDBMock.clearAllCache).toHaveBeenCalledTimes(1)
    expect(offlineManagerMock.clearQueue).toHaveBeenCalledTimes(1)
    expect(offlineManagerMock.refreshCacheStats).toHaveBeenCalled()
    expect(activeWorker.postMessage).toHaveBeenCalledWith(
      { type: 'CLEAR_CACHE' },
      expect.any(Array),
    )
  })

  it('shows offline notice for offline-only actions', async () => {
    await mountComposable()
    await Promise.resolve()
    await nextTick()

    onlineValue = false
    window.dispatchEvent(new Event('offline'))
    await nextTick()

    expect(hostState.api?.checkOfflineAction('borrow')).toBe(false)
    expect(hostState.api?.showOfflineNotice.value).toBe(true)
    expect(loggerMock.warn).toHaveBeenCalled()
  })

  it('returns fallback cache stats on refresh failure', async () => {
    offlineManagerMock.refreshCacheStats.mockRejectedValueOnce(new Error('broken'))

    await mountComposable()
    await Promise.resolve()
    await nextTick()

    await expect(hostState.api?.getCacheStats()).resolves.toEqual({
      bookCount: 0,
      hotBookCount: 0,
      cacheSize: 0,
      lastUpdate: null,
    })
    expect(loggerMock.error).toHaveBeenCalled()
  })
})
