import { onBeforeUnmount, ref } from 'vue'

interface UseSearchAutocompleteOptions {
  debounceMs?: number
  limit?: number
  minChars?: number
}

type SuggestionFetcher = (query: string, limit: number) => Promise<string[]>

const createListId = () => `search-suggestions-${Math.random().toString(36).slice(2, 10)}`

export function useSearchAutocomplete(
  fetchSuggestions: SuggestionFetcher,
  options: UseSearchAutocompleteOptions = {},
) {
  const debounceMs = options.debounceMs ?? 180
  const limit = options.limit ?? 6
  const minChars = options.minChars ?? 2

  const listId = createListId()
  const suggestions = ref<string[]>([])
  const loading = ref(false)
  const isOpen = ref(false)
  const activeIndex = ref(-1)

  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let requestVersion = 0
  let skipNextSchedule = false

  const normalize = (value: string) => value.trim().toLowerCase()
  const isComposing = (event: KeyboardEvent) => event.isComposing || event.keyCode === 229

  const getOptionId = (index: number) => `${listId}-option-${index}`

  const invalidatePendingRequests = () => {
    requestVersion += 1
  }

  const close = () => {
    invalidatePendingRequests()
    loading.value = false
    isOpen.value = false
    activeIndex.value = -1
  }

  const clear = () => {
    suggestions.value = []
    loading.value = false
    close()
  }

  const setSuggestions = (nextSuggestions: string[], query: string) => {
    const normalizedQuery = normalize(query)
    const unique = Array.from(
      new Set(
        nextSuggestions
          .map(item => item.trim())
          .filter(item => item && normalize(item) !== normalizedQuery),
      ),
    ).slice(0, limit)

    suggestions.value = unique
    isOpen.value = unique.length > 0
    activeIndex.value = -1
  }

  const updateSuggestions = async (query: string) => {
    const trimmed = query.trim()
    if (trimmed.length < minChars) {
      clear()
      return
    }

    const version = ++requestVersion
    loading.value = true

    try {
      const nextSuggestions = await fetchSuggestions(trimmed, limit)
      if (version !== requestVersion) return
      setSuggestions(nextSuggestions, trimmed)
    } catch {
      if (version !== requestVersion) return
      clear()
    } finally {
      if (version === requestVersion) {
        loading.value = false
      }
    }
  }

  const schedule = (query: string) => {
    if (skipNextSchedule) {
      skipNextSchedule = false
      return
    }

    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    const trimmed = query.trim()
    if (trimmed.length < minChars) {
      invalidatePendingRequests()
      clear()
      return
    }

    debounceTimer = setTimeout(() => {
      debounceTimer = null
      void updateSuggestions(trimmed)
    }, debounceMs)
  }

  const openIfAvailable = () => {
    if (suggestions.value.length > 0) {
      isOpen.value = true
    }
  }

  const selectSuggestion = (value: string) => {
    skipNextSchedule = true
    clear()
    return value
  }

  const moveActive = (direction: 1 | -1) => {
    if (suggestions.value.length === 0) {
      activeIndex.value = -1
      return
    }

    if (activeIndex.value < 0) {
      activeIndex.value = direction > 0 ? 0 : suggestions.value.length - 1
      return
    }

    activeIndex.value = (activeIndex.value + direction + suggestions.value.length) % suggestions.value.length
  }

  const handleKeydown = (event: KeyboardEvent) => {
    if (isComposing(event)) {
      return null
    }

    if (!isOpen.value || suggestions.value.length === 0) {
      return null
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      moveActive(1)
      return null
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      moveActive(-1)
      return null
    }

    if (event.key === 'Escape') {
      event.preventDefault()
      close()
      return null
    }

    if (event.key === 'Enter' && activeIndex.value >= 0) {
      event.preventDefault()
      return selectSuggestion(suggestions.value[activeIndex.value])
    }

    return null
  }

  onBeforeUnmount(() => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
  })

  return {
    listId,
    suggestions,
    loading,
    isOpen,
    activeIndex,
    getOptionId,
    schedule,
    openIfAvailable,
    close,
    clear,
    selectSuggestion,
    handleKeydown,
  }
}
