import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref, nextTick } from 'vue'
import HeroSection from '@/components/home/HeroSection.vue'

// Mock dependencies
const mockSuggestions = ref<string[]>([])
const mockLoading = ref(false)
const mockIsOpen = ref(false)
const mockActiveIndex = ref(-1)

vi.mock('@/composables/useSearchAutocomplete', () => ({
  useSearchAutocomplete: () => ({
    listId: 'search-suggestions',
    suggestions: mockSuggestions,
    loading: mockLoading,
    isOpen: mockIsOpen,
    activeIndex: mockActiveIndex,
    getOptionId: (index: number) => `search-option-${index}`,
    schedule: vi.fn(),
    openIfAvailable: vi.fn(),
    close: vi.fn(),
    clear: vi.fn(),
    selectSuggestion: (suggestion: string) => suggestion,
    handleKeydown: vi.fn(),
  }),
}))

vi.mock('@/api/bookApi', () => ({
  bookApi: {
    getSearchSuggestions: vi.fn(),
  },
}))

vi.mock('@/motion', () => ({
  gsap: {
    context: vi.fn(() => ({ revert: vi.fn() })),
    timeline: vi.fn(() => ({
      fromTo: vi.fn().mockReturnThis(),
    })),
    utils: {
      toArray: vi.fn(() => []),
    },
    set: vi.fn(),
    to: vi.fn(),
  },
  prefersReducedMotion: vi.fn(() => true),
  ScrollTrigger: {
    create: vi.fn(),
  },
}))

const messages: Record<string, string> = {
  'hero.kicker': '读者高频检索入口',
  'hero.title': '从一次搜索，直接进入借阅动作。',
  'hero.subtitle': '首页只负责填写条件并跳转到统一结果页。查询条件、页码与排序都会保留在链接里，适合刷新、分享和连续追书。',
  'hero.searchTitle': '统一结果页入口',
  'hero.searchNote': '支持关键字、作者、年份、分类与排序',
  'hero.searchPlaceholder': '输入书名、作者或 ISBN',
  'hero.searchBtn': '进入结果页',
  'hero.trendingLabel': '常用入口',
  'hero.tag1Label': '人工智能',
  'hero.tag1Query': '人工智能',
  'hero.tag2Label': '设计模式',
  'hero.tag2Query': '设计模式',
  'hero.tag3Label': '数字人文',
  'hero.tag3Query': '数字人文',
  'hero.tag4Label': '古典文学',
  'hero.tag4Query': '古典文学',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
    locale: ref('zh-CN'),
  }),
}))

// Mock clock components
vi.mock('@/dizhi/DizhiClock.vue', () => ({
  default: {
    name: 'DizhiClock',
    props: ['subtle'],
    template: '<div class="dizhi-clock-mock">Dizhi Clock</div>',
  },
}))

vi.mock('@/dizhi/ModernClock.vue', () => ({
  default: {
    name: 'ModernClock',
    template: '<div class="modern-clock-mock">Modern Clock</div>',
  },
}))

describe('HeroSection', () => {
  let wrapper: ReturnType<typeof mount>

  beforeEach(() => {
    vi.clearAllMocks()
    mockSuggestions.value = []
    mockLoading.value = false
    mockIsOpen.value = false
    mockActiveIndex.value = -1
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const mountComponent = async () => {
    wrapper = mount(HeroSection, {
      global: {
        stubs: {
          transition: false,
          Transition: false,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  describe('DOM structure requirements', () => {
    it('renders hero stage wrapper', async () => {
      await mountComponent()
      expect(wrapper.find('.hero-stage').exists()).toBe(true)
    })

    it('renders hero intro section with kicker, title and subtitle', async () => {
      await mountComponent()
      const intro = wrapper.find('.hero-intro')
      expect(intro.exists()).toBe(true)

      expect(intro.find('.hero-kicker').exists()).toBe(true)
      expect(intro.find('.hero-title').exists()).toBe(true)
      expect(intro.find('.hero-subtitle').exists()).toBe(true)
    })

    it('renders hero search panel with input and button', async () => {
      await mountComponent()
      const searchPanel = wrapper.find('.hero-search-panel')
      expect(searchPanel.exists()).toBe(true)

      expect(searchPanel.find('.search-input').exists()).toBe(true)
      expect(searchPanel.find('.search-btn').exists()).toBe(true)
    })

    it('renders value grid with three value cards', async () => {
      await mountComponent()
      const valueGrid = wrapper.find('.hero-value-grid')
      expect(valueGrid.exists()).toBe(true)

      const valueCards = valueGrid.findAll('.hero-value-card')
      expect(valueCards.length).toBe(3)
    })

    it('renders trending tags section', async () => {
      await mountComponent()
      const trendingTags = wrapper.find('.trending-tags')
      expect(trendingTags.exists()).toBe(true)

      const tagButtons = trendingTags.findAll('.tag-btn')
      expect(tagButtons.length).toBe(4)
    })
  })

  describe('Search functionality', () => {
    it('emits search event when submit button is clicked', async () => {
      await mountComponent()
      const input = wrapper.find('.search-input')
      await input.setValue('test query')

      await wrapper.find('.search-btn').trigger('click')

      expect(wrapper.emitted('search')).toBeTruthy()
      expect(wrapper.emitted('search')![0]).toEqual(['test query'])
    })

    it('emits search event when Enter key is pressed', async () => {
      await mountComponent()
      const input = wrapper.find('.search-input')
      await input.setValue('test query')

      await input.trigger('keydown', { key: 'Enter' })

      expect(wrapper.emitted('search')).toBeTruthy()
      expect(wrapper.emitted('search')![0]).toEqual(['test query'])
    })

    it('applies trending tag and searches when tag is clicked', async () => {
      await mountComponent()
      const firstTag = wrapper.find('.tag-btn')
      await firstTag.trigger('click')

      expect(wrapper.emitted('search')).toBeTruthy()
      const emittedQuery = wrapper.emitted('search')![0][0] as string
      expect(emittedQuery).toBeTruthy()
    })
  })

  describe('Content rendering', () => {
    it('renders search title and note', async () => {
      await mountComponent()
      expect(wrapper.find('.hero-search-title').exists()).toBe(true)
      expect(wrapper.find('.hero-search-note').exists()).toBe(true)
    })

    it('renders search placeholder text', async () => {
      await mountComponent()
      const input = wrapper.find('.search-input')
      expect(input.attributes('placeholder')).toBe('输入书名、作者或 ISBN')
    })

    it('renders trending label', async () => {
      await mountComponent()
      expect(wrapper.find('.trending-label').exists()).toBe(true)
    })
  })

  describe('Search autocomplete', () => {
    it('opens suggestion popover when suggestions are available', async () => {
      await mountComponent()
      mockIsOpen.value = true
      mockSuggestions.value = ['Suggestion 1', 'Suggestion 2']

      await nextTick()

      const popover = wrapper.find('.hero-suggestion-popover')
      expect(popover.exists()).toBe(true)
    })

    it('renders suggestion items', async () => {
      await mountComponent()
      mockIsOpen.value = true
      mockSuggestions.value = ['Book Title 1', 'Book Title 2']

      await nextTick()

      const items = wrapper.findAll('.hero-suggestion-item')
      expect(items.length).toBe(2)
    })
  })
})
