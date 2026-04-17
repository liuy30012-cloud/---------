import { computed, ref } from 'vue'
import { safeGetItem, safeSetItem } from '../utils/storageHelpers'

type Theme = 'bamboo-monastery' | 'midnight-archive'

const STORAGE_KEY = 'library-theme'

const currentTheme = ref<Theme>(loadTheme())

function loadTheme(): Theme {
  const stored = safeGetItem(STORAGE_KEY)
  if (stored === 'midnight-archive' || stored === 'bamboo-monastery') {
    return stored
  }
  return 'bamboo-monastery'
}

function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme
}

export function useTheme() {
  const isDark = computed(() => currentTheme.value === 'midnight-archive')

  const toggle = () => {
    currentTheme.value = currentTheme.value === 'bamboo-monastery'
      ? 'midnight-archive'
      : 'bamboo-monastery'
    safeSetItem(STORAGE_KEY, currentTheme.value)
    applyTheme(currentTheme.value)
  }

  // Apply theme on first call
  applyTheme(currentTheme.value)

  return { isDark, toggle, currentTheme }
}
