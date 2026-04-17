import { ref } from 'vue'
import httpClient from '../api/httpClient'
import { formatRelativeTime } from '../utils/timeHelpers'

export interface SearchHistoryPayload {
  keyword: string
  resultCount: number
  targetPath?: string
  queryPayload?: string
  saved?: boolean
  label?: string
}

export interface HistoryItem extends SearchHistoryPayload {
  id?: number
  timestamp: Date
  usageCount?: number
}

const MAX_LOCAL_HISTORY = 10
const LOCAL_STORAGE_KEY = 'searchHistoryLocal'
const LOCAL_SUGGESTIONS_KEY = 'searchSuggestions'

export function useSearchHistory(locale: { value: string }) {
  const searchHistory = ref<HistoryItem[]>([])
  const showHistoryPanel = ref(false)
  const localHistory = ref<string[]>([])
  const suggestions = ref<string[]>([])

  const normalizeHistoryItem = (historyItem: any): HistoryItem => {
    let timestamp = historyItem.timestamp
    if (typeof timestamp === 'string') {
      timestamp = timestamp.replace(' ', 'T')
    }
    return {
      ...historyItem,
      saved: Boolean(historyItem.saved),
      timestamp: new Date(timestamp),
    }
  }

  const loadHistory = async () => {
    try {
      const res = await httpClient.get<any[]>('/api/search-history')
      searchHistory.value = res.data.data.map(normalizeHistoryItem)
    } catch {
      // keep local history
    }
  }

  const persistHistory = async (payload: SearchHistoryPayload) => {
    const trimmed = payload.keyword.trim()
    if (!trimmed) return

    const previousHistory = [...searchHistory.value]
    const now = new Date()
    const nextItem: HistoryItem = {
      ...payload,
      keyword: trimmed,
      resultCount: payload.resultCount,
      saved: Boolean(payload.saved),
      timestamp: now,
    }

    searchHistory.value = searchHistory.value.filter(item =>
      (item.queryPayload || item.keyword) !== (payload.queryPayload || trimmed),
    )
    searchHistory.value.unshift(nextItem)

    try {
      const response = await httpClient.post('/api/search-history', {
        ...payload,
        keyword: trimmed,
      })
      const savedItem = normalizeHistoryItem(response.data.data)
      searchHistory.value = [
        savedItem,
        ...searchHistory.value.filter(item => item.timestamp !== now),
      ]
    } catch {
      searchHistory.value = previousHistory
    }
  }

  const addToHistory = async (payload: SearchHistoryPayload) => {
    await persistHistory(payload)
  }

  const saveSearch = async (payload: SearchHistoryPayload) => {
    await persistHistory({ ...payload, saved: true })
  }

  const clearHistory = async () => {
    const previousHistory = [...searchHistory.value]
    searchHistory.value = []
    try {
      await httpClient.delete('/api/search-history')
    } catch {
      searchHistory.value = previousHistory
    }
  }

  const toggleHistoryPanel = (closeOthers?: () => void) => {
    closeOthers?.()
    showHistoryPanel.value = !showHistoryPanel.value
  }

  const formatHistoryTime = (date: Date): string => {
    return formatRelativeTime(date, locale.value === 'zh' ? 'zh-CN' : 'en')
  }

  return {
    searchHistory,
    showHistoryPanel,
    loadHistory,
    addToHistory,
    saveSearch,
    clearHistory,
    toggleHistoryPanel,
    formatHistoryTime,
  }
}
