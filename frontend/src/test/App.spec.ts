import { defineComponent, h, nextTick, reactive, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const routeState = reactive({
  name: 'Dashboard',
  path: '/dashboard',
  fullPath: '/dashboard?page=1',
  query: { page: '1' },
  hash: '#section',
  params: {},
  meta: { layout: 'page', shell: 'default' } as Record<string, unknown>,
})

const routerPushMock = vi.fn(() => Promise.resolve())
const routerReplaceMock = vi.fn(() => Promise.resolve())
const notifController = {
  notifications: ref<any[]>([]),
  showNotifPanel: ref(false),
  loadNotifications: vi.fn(),
}
const historyController = {
  searchHistory: ref<any[]>([]),
  showHistoryPanel: ref(false),
  loadHistory: vi.fn(),
}
const userStoreState = reactive({
  isLoggedIn: false,
})
const mountedWrappers: Array<ReturnType<typeof mount>> = []

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ locale: ref('zh-CN') }),
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRoute: () => routeState,
    useRouter: () => ({
      push: routerPushMock,
      replace: routerReplaceMock,
    }),
  }
})

vi.mock('@/composables/useNotifications', () => ({
  useNotifications: () => notifController,
}))

vi.mock('@/composables/useSearchHistory', () => ({
  useSearchHistory: () => historyController,
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => userStoreState,
}))

const createStub = (name: string, options?: { emit?: { name: string; payload?: unknown } }) => defineComponent({
  name,
  emits: options?.emit ? [options.emit.name] : [],
  setup(_, { emit, slots }) {
    return () => h('div', {
      class: 'component-stub',
      'data-component': name,
      onClick: options?.emit ? () => emit(options.emit!.name, options.emit!.payload) : undefined,
    }, slots.default ? slots.default() : name)
  },
})

vi.mock('@/components/home/HeroSection.vue', () => ({
  default: createStub('HeroSection', { emit: { name: 'search', payload: 'history' } }),
}))
vi.mock('@/components/home/RelatedVolumes.vue', () => ({ default: createStub('RelatedVolumes') }))
vi.mock('@/components/home/SiteFooter.vue', () => ({ default: createStub('SiteFooter') }))
vi.mock('@/components/layout/PageScaffold.vue', () => ({ default: createStub('PageScaffold') }))
vi.mock('@/components/navigation/TopNav.vue', () => ({ default: createStub('TopNav') }))
vi.mock('@/pet/DesktopPet.vue', () => ({ default: createStub('DesktopPet') }))
vi.mock('@/components/common/CaptchaChallengeModal.vue', () => ({ default: createStub('CaptchaChallengeModal') }))
vi.mock('@/components/common/ToastContainer.vue', () => ({ default: createStub('ToastContainer') }))
vi.mock('@/components/common/OfflineIndicator.vue', () => ({ default: createStub('OfflineIndicator') }))
vi.mock('@/components/panels/QuickActionPanel.vue', () => ({ default: createStub('QuickActionPanel') }))
vi.mock('@/components/common/ErrorBoundary.vue', () => ({
  default: defineComponent({
    name: 'ErrorBoundary',
    emits: ['retry'],
    setup(_, { emit, slots }) {
      return () => h('div', [
        h('button', { class: 'error-boundary-trigger', onClick: () => emit('retry') }, 'retry'),
        slots.default?.(),
      ])
    },
  }),
}))

const RouterViewStub = defineComponent({
  name: 'RouterView',
  setup(_, { slots }) {
    const RoutedComponent = defineComponent({
      name: 'RoutedComponent',
      setup: () => () => h('div', { class: 'routed-component' }, 'page'),
    })

    return () => slots.default?.({ Component: RoutedComponent })
  },
})

const mountApp = async () => {
  const module = await import('@/App.vue')
  const wrapper = mount(module.default, {
    global: {
      stubs: {
        RouterView: RouterViewStub,
        transition: false,
        Transition: false,
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('App', () => {
  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
  })

  beforeEach(() => {
    routeState.name = 'Dashboard'
    routeState.path = '/dashboard'
    routeState.fullPath = '/dashboard?page=1'
    routeState.query = { page: '1' }
    routeState.hash = '#section'
    routeState.params = {}
    routeState.meta = { layout: 'page', shell: 'default' }
    userStoreState.isLoggedIn = false
    notifController.notifications.value = []
    notifController.showNotifPanel.value = false
    historyController.searchHistory.value = []
    historyController.showHistoryPanel.value = false
    notifController.loadNotifications.mockReset()
    historyController.loadHistory.mockReset()
    routerPushMock.mockClear()
    routerReplaceMock.mockClear()
  })

  it('renders home layout branches', async () => {
    routeState.meta = { layout: 'home' }
    const wrapper = await mountApp()

    expect(wrapper.find('[data-component="TopNav"]').exists()).toBe(true)
    expect(wrapper.find('[data-component="HeroSection"]').exists()).toBe(true)
    expect(wrapper.find('[data-component="RelatedVolumes"]').exists()).toBe(true)
    expect(wrapper.find('[data-component="SiteFooter"]').exists()).toBe(true)
  })

  it('renders page layout branch and routed component', async () => {
    const wrapper = await mountApp()

    expect(wrapper.find('[data-component="PageScaffold"]').exists()).toBe(true)
    expect(wrapper.find('.routed-component').exists()).toBe(true)
  })

  it('renders fallback router view when layout is immersive', async () => {
    routeState.meta = { layout: 'immersive' }
    const wrapper = await mountApp()

    expect(wrapper.find('[data-component="TopNav"]').exists()).toBe(false)
    expect(wrapper.find('[data-component="PageScaffold"]').exists()).toBe(false)
  })

  it('shows quick action panel only for logged in users', async () => {
    const loggedOut = await mountApp()
    expect(loggedOut.find('[data-component="QuickActionPanel"]').exists()).toBe(false)

    userStoreState.isLoggedIn = true
    const loggedIn = await mountApp()
    expect(loggedIn.find('[data-component="QuickActionPanel"]').exists()).toBe(true)
  })

  it('pushes search route from hero search event', async () => {
    routeState.meta = { layout: 'home' }
    const wrapper = await mountApp()

    await wrapper.get('[data-component="HeroSection"]').trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'BookSearch',
      query: { keyword: 'history', page: '0', size: '12' },
    })
  })

  it('replaces current route when retrying error boundary', async () => {
    const wrapper = await mountApp()

    await wrapper.get('.error-boundary-trigger').trigger('click')
    expect(routerReplaceMock).toHaveBeenCalledWith({
      path: '/dashboard?page=1',
      query: { page: '1' },
      hash: '#section',
    })
  })

  it('loads notifications and history on mount when logged in', async () => {
    userStoreState.isLoggedIn = true
    await mountApp()

    expect(notifController.loadNotifications).toHaveBeenCalledTimes(1)
    expect(historyController.loadHistory).toHaveBeenCalledTimes(1)
  })

  it('reacts to login state transitions', async () => {
    await mountApp()

    userStoreState.isLoggedIn = true
    await nextTick()
    expect(notifController.loadNotifications).toHaveBeenCalledTimes(1)
    expect(historyController.loadHistory).toHaveBeenCalledTimes(1)

    notifController.notifications.value = [{ id: 1 }]
    notifController.showNotifPanel.value = true
    historyController.searchHistory.value = ['foo']
    historyController.showHistoryPanel.value = true

    userStoreState.isLoggedIn = false
    await nextTick()
    expect(notifController.notifications.value).toEqual([])
    expect(notifController.showNotifPanel.value).toBe(false)
    expect(historyController.searchHistory.value).toEqual([])
    expect(historyController.showHistoryPanel.value).toBe(false)
  })
})
