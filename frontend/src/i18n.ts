import { createI18n } from 'vue-i18n'
import messages from './locales'

const getBrowserLang = (): string => {
  const stored = localStorage.getItem('locale')
  if (stored === 'zh' || stored === 'en') return stored
  const nav = navigator as Navigator & { userLanguage?: string }
  const browserLang = navigator.language || nav.userLanguage
  if (browserLang && browserLang.toLowerCase().includes('zh')) {
    return 'zh'
  }
  return 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: getBrowserLang(),
  fallbackLocale: 'en',
  messages,
})

export default i18n
