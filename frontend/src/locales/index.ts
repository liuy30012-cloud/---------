// src/locales/index.ts
import common from './modules/common'
import nav from './modules/nav'
import hero from './modules/hero'
import filters from './modules/filters'
import catalog from './modules/catalog'
import related from './modules/related'
import notifications from './modules/notifications'
import history from './modules/history'
import footer from './modules/footer'
import notFound from './modules/notFound'
import forgotPassword from './modules/forgotPassword'
import login from './modules/login'
import bookSearch from './modules/bookSearch'
import bookDetail from './modules/bookDetail'
import dashboard from './modules/dashboard'
import myBorrows from './modules/myBorrows'
import myReservations from './modules/myReservations'
import myBookshelf from './modules/myBookshelf'
import myAccount from './modules/myAccount'
import damageReports from './modules/damageReports'
import purchaseSuggestions from './modules/purchaseSuggestions'
import inventoryAlerts from './modules/inventoryAlerts'
import userManagement from './modules/userManagement'
import quickActions from './modules/quickActions'
import offlineIndicator from './modules/offlineIndicator'
import cacheManagement from './modules/cacheManagement'
import captcha from './modules/captcha'
import errorCenter from './modules/errorCenter'

function merge(modules: Record<string, any>[]) {
  const result: Record<string, any> = {}
  for (const mod of modules) {
    for (const locale of ['zh', 'en'] as const) {
      if (!result[locale]) result[locale] = {}
      if (import.meta.env.DEV && mod[locale] === undefined) {
        console.warn(`[i18n] Module missing "${locale}" key:`, Object.keys(mod))
      }
      Object.assign(result[locale], mod[locale])
    }
  }
  return result as { zh: Record<string, any>; en: Record<string, any> }
}

const messages = merge([
  common, nav, hero, filters, catalog, related, notifications,
  history, footer, notFound, forgotPassword, login, bookSearch,
  bookDetail, dashboard, myBorrows, myReservations, myBookshelf,
  myAccount, damageReports, purchaseSuggestions, inventoryAlerts, userManagement,
  quickActions, offlineIndicator, cacheManagement, captcha, errorCenter,
])

export default messages
