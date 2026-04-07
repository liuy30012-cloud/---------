import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import zh from './locales/zh.json'

// Check browser language or fallback to English
const getBrowserLang = (): string => {
  const nav = navigator as Navigator & { userLanguage?: string }
  const browserLang = navigator.language || nav.userLanguage
  if (browserLang && browserLang.toLowerCase().includes('zh')) {
    return 'zh'
  }
  return 'en'
}

const i18n = createI18n({
  legacy: false, // you must set `false`, to use Composition API
  locale: getBrowserLang(), // set locale
  fallbackLocale: 'en', // set fallback locale
  messages: {
    en,
    zh
  }
})

export default i18n
