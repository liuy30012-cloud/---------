import { ref } from 'vue'
import axios from 'axios'
import httpClient from '../api/httpClient'
import { API_CONFIG } from '../config'
import { logger } from '../utils/logger'

export interface Book {
  id: string
  title: string
  author: string
  isbn: string
  location: string
  coverUrl?: string
  status?: 'Localized' | 'Draft' | 'Cataloged'
  year?: string
  description?: string
  languageCode?: string
  availability?: 'inLibrary' | 'checkedOut'
  category?: string
}

export const MOCK_BOOKS: Book[] = [
  {
    id: "1",
    title: "The Art of Computer Programming",
    author: "Donald Knuth",
    isbn: "978-0201896831",
    location: "1F Area A - Main Room - Shelf 03",
    coverUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuDxNL6kJydKertJtHcaPLILH1mQKaMa7cc0eqrjpKTnwrsdj5PVXCzix7EAWxcJ0Mf4M-mjkEkt85MIJL-IRLeJ5MNJLLHpxELVxufEF4gr6jsZgXjLKEok-ORgPrPnajuS9IdaehmmsIZDBzwFZHzGSbpMFz8TU_FPzj8GrBENUN5MmD1XccgKgsTOnUYwxACCeYeKBZvsTA2U2atD7XT9c46N9qAzGmnHdOn8Bb_EUjCs23vZhGyXNfOnUyaptmO6F664x-lYN8_3",
    status: 'Localized',
    year: '2011',
    languageCode: 'English',
    description: "A comprehensive monograph on computer science, covering fundamental algorithms and data structures. This edition includes localized annotations for modern archival systems.",
    availability: 'inLibrary',
    category: 'Technology'
  },
  {
    id: "2",
    title: "The Elements of Typographic Style",
    author: "Robert Bringhurst",
    isbn: "978-1933820224",
    location: "2F Design Wing - Row 12 - Shelf 01",
    coverUrl: "https://lh3.googleusercontent.com/aida-public/AB6AXuBXrD-XoDl_DQHriIn0ZgdwAVLN8ohu9KQlHy0oPQrdNfjjK2apGdXW2UOHqMvm0cEdFBGeFOTynD3hcVHmRhs2jyFucVHgQq9ZIrU-_169eVRYdZ4QNiHZ70jqErNNuP5hGmtqNp5ey6kif3FwmwVo13B0u2DK4MklLcA6LOBnqyNrNtNRU0Jc_SVH2-4gYSfKFwQdvkyEOBSvMKjl7duvcP31eUMpTMVGyyT4LaSBO6cVz-5BrzEvgYlfrTMtT8-Pok4AVANb8ZTk",
    status: 'Draft',
    year: '2004',
    languageCode: 'Latin',
    description: "Regarded as a definitive manual on typography. This volume explores the marriage of philosophy and aesthetics in the printed word across centuries of archival tradition.",
    availability: 'checkedOut',
    category: 'Art'
  },
  { id: "3", title: "设计模式", author: "Erich Gamma", isbn: "9787111075660", location: "二楼外文室 -> 经典著作区 -> B01号书架", year: '1994', status: 'Cataloged', languageCode: '中文 (Chinese)', availability: 'checkedOut', category: 'Technology' },
  { id: "4", title: "深入理解计算机系统", author: "Randal E. Bryant", isbn: "9787111544937", location: "二楼系统架构 -> 硬件区 -> C01号书架", year: '2015', status: 'Cataloged', languageCode: '中文 (Chinese)', availability: 'inLibrary', category: 'Technology' },
  { id: "5", title: "Faust", author: "Johann Wolfgang von Goethe", isbn: "9781234567890", location: "德文文献室", year: '1808', status: 'Draft', languageCode: 'Deutsch', availability: 'checkedOut', category: 'Literature' },
]

export const RELATED_BOOKS = [
  { title: "The Design of Everyday Things", author: "Don Norman", img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAD7CA9NIOl3jX9pMduAn3yJPP1w4WM0Rb4W3BiE3rszMcabHUPt6V26CF7iWmRzLFUqdGFpTwSbrS7FmhjtbWtzap3Dnb5sCKhs7Xj81BFXPvSG0oZ4o9vFJx5wp1Ufp296B-_lXe_9n9BfwABZm7tf-DGHKtKyzjnDubJmvrPENuXJgfEyfvBWFfw6Pz4dFJXUb79SNQJy_0z8lhlxdfCzQI9tH6Mdq4P0Lu-G7iPtCxXxHWQlHdPW_ZHgCAE6EKmgYDSPGsDKwDQ" },
  { title: "Universal Principles of Design", author: "William Lidwell", img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAc5gaw_WK_ZVZTuBcic21xXREHCQlwLkSqjlM08TRX_6DaXlg-G7h4W3i81fbyjYdiuI8sdaYevBcyOSYDiIwWDDVofiLNTqPyTVasMAJVdBFYCRJk7g6MKxdwOdQvBjGJXSm6eVzhZndpB7kwT4K6dLtsGTO3MtvF2dks6jMkTe1FKeSbZM6RP2N2mVLaLT3huxjwUx2d6L5y5VJ5X6qUC_vLQ78rAwmaa0PpufhB1S6lwIZAo9WUl_YYghETmvrSdPT6ahSt_TsB" },
  { title: "Grid Systems", author: "Josef Müller-Brockmann", img: "https://lh3.googleusercontent.com/aida-public/AB6AXuDYk9dJaire7Jv7-fVeJCOS5a6F_jJzm8eFD5pYvLqSJ7ra22Sqeu9lTZTuGZyy_4-PVya0ue3MK7zBB6QrlZYVQqab1iP3FKb2IVNd6sToJo_F4B2FCvpPEz26gX9WJCKGUT8Nj_2DwQzzLBg1MFLCivJe6fhr4A8x0MGQaC-zxLMhFEen_ypu8rzdMX6wNl_ZvPxJUJwTkJTow1K17MjuG3S4LRvQC3B_O9P3_KRYjRiuX2oV_jGo0G3t361Up0TAMoxsuXekcBqL" },
  { title: "Thinking with Type", author: "Ellen Lupton", img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAhAMbR8o2-8cKWRnCQJZWPQL8PqsbFaz-kdzCA-4JijqIZB9KdvjS4QGNRa7P4i217AF9XgX3SKSBnGf5FINOOeLtK-j2415k2mlN3R83Qh2NLPAkWChIPIdw6k5ntevNwL3owKqm_uz-tT6vIJld_nc4if2-UCysEmLzvf-AQS89daQj8zr9uN7FCSEM6KEzQK76BMuug1xTeH2yCKp86KKavQznEYTgHh4rz_z7NwaeZcVvo-pb1gwGsWp0mp8nW0f-7obbn5jxN" },
  { title: "Design As Art", author: "Bruno Munari", img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAvVLZnylaT-oBahqM0Wt7THgehtqChGSlB7LNBLvxzDJU1_5lmAng6LAS2BNfDEtdrav-FHwRD7ghJgoPtFs36uAkpaRlMLshWHXCyCT-hJ3j4IdECAY8lr4t7ZO6bpI-VHqRJdF_BPIcYudaLH7Qyu-8RF5n06mhK6cDVdU_JONQQn-R0Rr7ZZVH9_g8onh1pPhaU8hMNPu08Iu5ZT52i401iYs5L8b-OBwp8HaYcLLOq1H5rB_RVREqLGq2_b979gJXxw6khVI-c" }
]

export function useBookSearch(emitPetEvent: (event: string, data?: any) => void, addToHistory: (keyword: string, resultCount: number) => Promise<void>) {
  const DEMO_MODE = import.meta.env.VITE_DEMO_BOOKS === 'true'
  const searchQuery = ref('')
  const selectedLangFilters = ref<string[]>([])
  const selectedStatusFilters = ref<string[]>([])
  const selectedCategoryFilters = ref<string[]>([])
  const books = ref<Book[]>([])
  const loading = ref(false)
  const hasSearched = ref(false)
  const isOffline = ref(false)
  const viewMode = ref<'grid' | 'list'>('grid')

  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let filterDebounceTimer: ReturnType<typeof setTimeout> | null = null
  let searchAbortController: AbortController | null = null

  const setViewMode = (mode: 'grid' | 'list') => { viewMode.value = mode }

  const filterMockBooks = (keyword: string): Book[] => {
    const lower = keyword.toLowerCase().trim()
    return MOCK_BOOKS.filter(book => {
      const matchKeyword = !lower || book.title.toLowerCase().includes(lower) ||
                           book.author.toLowerCase().includes(lower) ||
                           book.isbn.includes(lower)

      const matchLang = selectedLangFilters.value.length === 0 ||
                        (book.languageCode && selectedLangFilters.value.includes(book.languageCode))

      const matchStatus = selectedStatusFilters.value.length === 0 ||
                          (book.availability && selectedStatusFilters.value.includes(book.availability))

      const matchCategory = selectedCategoryFilters.value.length === 0 ||
                            (book.category && selectedCategoryFilters.value.includes(book.category))

      return matchKeyword && matchLang && matchStatus && matchCategory
    })
  }

  const normalizeBackendBooks = (payload: any): Book[] => {
    const rawBooks = Array.isArray(payload) ? payload : payload?.data
    if (!Array.isArray(rawBooks)) {
      throw new Error(payload?.message || 'Unexpected books response')
    }

    return rawBooks.map((b: any) => ({
      ...b,
      coverUrl: b.coverUrl || '',
      status: b.status || 'Cataloged',
      year: b.year || 'N/A',
      languageCode: b.languageCode || 'Unknown',
      availability: b.availability || 'inLibrary',
      category: b.category || 'Technology'
    }))
  }

  const fetchBooks = async (query: string) => {
    if (searchAbortController) searchAbortController.abort()
    searchAbortController = new AbortController()

    loading.value = true
    isOffline.value = false
    let isAborted = false
    emitPetEvent('search:start', { query })

    try {
      const response = await httpClient.get('/api/books/search', {
        params: { keyword: query },
        signal: searchAbortController.signal,
        timeout: API_CONFIG.timeout
      })

      let resultBooks = normalizeBackendBooks(response.data)

      if (selectedLangFilters.value.length > 0) {
        resultBooks = resultBooks.filter((book: Book) =>
          book.languageCode && selectedLangFilters.value.includes(book.languageCode)
        )
      }
      if (selectedStatusFilters.value.length > 0) {
        resultBooks = resultBooks.filter((book: Book) =>
          book.availability && selectedStatusFilters.value.includes(book.availability)
        )
      }
      if (selectedCategoryFilters.value.length > 0) {
        resultBooks = resultBooks.filter((book: Book) =>
          book.category && selectedCategoryFilters.value.includes(book.category)
        )
      }

      books.value = resultBooks
      emitPetEvent('search:complete', { books: resultBooks })
    } catch (error) {
      if (axios.isCancel(error)) {
        isAborted = true
        return
      }

      logger.error('API Error:', error)
      const isNetworkFailure = axios.isAxiosError(error) ? !error.response : true
      if (isNetworkFailure && DEMO_MODE) {
        isOffline.value = true
        books.value = filterMockBooks(query)
        emitPetEvent('offline:detected')
        emitPetEvent('search:complete', { books: books.value })
      } else {
        isOffline.value = false
        books.value = []
      }
    } finally {
      if (!isAborted) {
        loading.value = false
        hasSearched.value = true
        if (query.trim()) {
          addToHistory(query, books.value.length)
        }
      }
    }
  }

  const applyFilters = () => {
    if (filterDebounceTimer) clearTimeout(filterDebounceTimer)
    filterDebounceTimer = setTimeout(() => {
      if (hasSearched.value && !isOffline.value) {
        fetchBooks(searchQuery.value)
      } else if (DEMO_MODE) {
        books.value = filterMockBooks(searchQuery.value)
      } else {
        books.value = []
      }
    }, 300)
  }

  const handleSearch = () => {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      fetchBooks(searchQuery.value)
    }, 400)
  }

  const handleSearchImmediate = () => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
    fetchBooks(searchQuery.value)
  }

  const cleanup = () => {
    if (debounceTimer) { clearTimeout(debounceTimer); debounceTimer = null }
    if (filterDebounceTimer) { clearTimeout(filterDebounceTimer); filterDebounceTimer = null }
    if (searchAbortController) { searchAbortController.abort(); searchAbortController = null }
  }

  return {
    searchQuery,
    selectedLangFilters,
    selectedStatusFilters,
    selectedCategoryFilters,
    books,
    loading,
    hasSearched,
    isOffline,
    viewMode,
    setViewMode,
    fetchBooks,
    applyFilters,
    handleSearch,
    handleSearchImmediate,
    cleanup,
  }
}
