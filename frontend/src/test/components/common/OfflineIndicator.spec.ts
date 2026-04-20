import { computed, defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const manualUpdateCacheMock = vi.fn()
const initializeMock = vi.fn()
const showOfflineNotice = ref(false)
const isOnline = ref(true)
const cacheStats = ref({
  bookCount: 12,
  hotBookCount: 5,
})
const syncState = ref({
  lastSyncError: null as string | null,
  pendingCount: 0,
})

const messages: Record<string, string> = {
  'offlineIndicator.offlineMode': 'Offline mode',
  'offlineIndicator.retryHint': 'Retry cache sync',
  'offlineIndicator.queueHint': 'Queued updates pending',
  'offlineIndicator.onlineHint': 'Workspace synced',
  'offlineIndicator.limitedFeatures': 'Some features are limited offline',
  'offlineIndicator.showDetails': 'Show details',
  'offlineIndicator.hideDetails': 'Hide details',
  'offlineIndicator.networkRequired': 'Network required',
  'offlineIndicator.noticeMessage': 'Reconnect to continue',
  'offlineIndicator.dismiss': 'Dismiss',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
  }),
}))

vi.mock('@/composables/useNetworkStatus', () => ({
  useNetworkStatus: () => ({
    showOfflineNotice,
    manualUpdateCache: manualUpdateCacheMock,
  }),
}))

vi.mock('@/composables/useOffline', () => ({
  useOffline: () => ({
    isOnline: computed(() => isOnline.value),
    cacheStats: computed(() => cacheStats.value),
    syncState: computed(() => syncState.value),
    initialize: initializeMock,
  }),
}))

const SyncStatusStub = defineComponent({
  name: 'SyncStatusStub',
  props: {
    syncState: {
      type: Object,
      required: true,
    },
  },
  emits: ['retry'],
  setup(props, { emit }) {
    return () => h('button', {
      class: 'sync-status-stub',
      'data-pending': String((props.syncState as { pendingCount?: number }).pendingCount ?? 0),
      onClick: () => emit('retry'),
    }, 'retry')
  },
})

const mountIndicator = async () => {
  const module = await import('@/components/common/OfflineIndicator.vue')
  return mount(module.default, {
    global: {
      stubs: {
        SyncStatus: SyncStatusStub,
        transition: false,
        Transition: false,
      },
    },
  })
}

describe('OfflineIndicator', () => {
  beforeEach(() => {
    manualUpdateCacheMock.mockReset()
    manualUpdateCacheMock.mockResolvedValue(true)
    initializeMock.mockReset()
    initializeMock.mockResolvedValue(undefined)

    showOfflineNotice.value = false
    isOnline.value = true
    cacheStats.value = {
      bookCount: 12,
      hotBookCount: 5,
    }
    syncState.value = {
      lastSyncError: null,
      pendingCount: 0,
    }
  })

  it('shows offline banner only when offline', async () => {
    const onlineWrapper = await mountIndicator()
    expect(onlineWrapper.find('.offline-indicator').exists()).toBe(false)

    isOnline.value = false
    const offlineWrapper = await mountIndicator()
    expect(offlineWrapper.find('.offline-indicator').exists()).toBe(true)
    expect(offlineWrapper.text()).toContain('Offline mode')
  })

  it('auto expands details when sync has issue', async () => {
    syncState.value = {
      lastSyncError: 'sync failed',
      pendingCount: 0,
    }

    const wrapper = await mountIndicator()
    expect(wrapper.find('.offline-panel').exists()).toBe(true)
    expect(wrapper.text()).toContain('Retry cache sync')
  })

  it('toggles detail panel from manage button', async () => {
    isOnline.value = false
    const wrapper = await mountIndicator()

    expect(wrapper.find('.offline-panel').exists()).toBe(false)
    await wrapper.get('.offline-manage').trigger('click')
    expect(wrapper.find('.offline-panel').exists()).toBe(true)
  })

  it('renders hint branches from sync state', async () => {
    syncState.value = {
      lastSyncError: 'failed',
      pendingCount: 0,
    }
    let wrapper = await mountIndicator()
    expect(wrapper.text()).toContain('Retry cache sync')

    syncState.value = {
      lastSyncError: null,
      pendingCount: 1,
    }
    isOnline.value = false
    wrapper = await mountIndicator()
    expect(wrapper.text()).toContain('Queued updates pending')

    isOnline.value = true
    syncState.value = {
      lastSyncError: null,
      pendingCount: 1,
    }
    wrapper = await mountIndicator()
    expect(wrapper.text()).toContain('Workspace synced')
  })

  it('closes offline notice overlay from button and backdrop', async () => {
    showOfflineNotice.value = true
    const wrapper = await mountIndicator()

    expect(wrapper.find('.offline-notice-overlay').exists()).toBe(true)
    await wrapper.get('.notice-button').trigger('click')
    expect(showOfflineNotice.value).toBe(false)

    showOfflineNotice.value = true
    await wrapper.vm.$nextTick()
    await wrapper.get('.offline-notice-overlay').trigger('click')
    expect(showOfflineNotice.value).toBe(false)
  })

  it('retries sync only when online', async () => {
    syncState.value = {
      lastSyncError: 'failed',
      pendingCount: 1,
    }

    const wrapper = await mountIndicator()
    await wrapper.get('.sync-status-stub').trigger('click')
    expect(manualUpdateCacheMock).toHaveBeenCalledTimes(1)

    manualUpdateCacheMock.mockClear()
    isOnline.value = false
    await wrapper.get('.sync-status-stub').trigger('click')
    expect(manualUpdateCacheMock).not.toHaveBeenCalled()
  })

  it('calls initialize on mount and swallows rejection', async () => {
    initializeMock.mockRejectedValueOnce(new Error('init failed'))
    await mountIndicator()
    await Promise.resolve()
    expect(initializeMock).toHaveBeenCalledTimes(1)
  })
})
