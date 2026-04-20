import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import i18n from './i18n'
import { createRevealDirective, initMotion } from './motion'
import { useUserStore } from './stores/user'
import { registerServiceWorker } from './utils/serviceWorker'
import { offlineDB } from './utils/offlineDB'
import { errorCenter } from './services/ErrorCenter'
import { getErrorMessage } from './utils/errorHelpers'
import './styles/variables.css'
import './dizhi/dizhi.css'
import './styles/element-theme.css'

const DEFAULT_ERROR_MESSAGE = '系统暂时无法完成当前操作，请稍后重试。'
const UNEXPECTED_ERROR_MESSAGE = '出现未预期错误，请稍后再试。'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(i18n)
initMotion()
app.directive('reveal', createRevealDirective())

// 初始化用户状态 (MUST happen BEFORE router)
const userStore = useUserStore()
userStore.initialize()

app.use(router)

app.config.errorHandler = (error, instance, info) => {
  errorCenter.handle(error as Error, {
    page: window.location.pathname,
    info,
    component: instance?.$options?.name || instance?.$options?.__name || 'UnknownComponent',
    retryable: true,
  })
}

window.addEventListener('error', (event) => {
  const message = getErrorMessage(event.error || event.message, DEFAULT_ERROR_MESSAGE)
  errorCenter.handle(new Error(message), {
    page: window.location.pathname,
    info: event.filename ? `${event.filename}:${event.lineno}:${event.colno}` : undefined,
    component: 'window.error',
    retryable: true,
  })
})

window.addEventListener('unhandledrejection', (event) => {
  const message = getErrorMessage(event.reason, UNEXPECTED_ERROR_MESSAGE)
  errorCenter.handle(new Error(message), {
    page: window.location.pathname,
    info: 'unhandledrejection',
    component: 'window.unhandledrejection',
    retryable: true,
  })
})

app.mount('#app')

// 注册 Service Worker 和初始化离线数据库
if (import.meta.env.PROD || import.meta.env.VITE_ENABLE_SW) {
  registerServiceWorker()
    .then(() => {
      console.log('[App] Service Worker 已注册')
      return offlineDB.init()
    })
    .then(() => {
      console.log('[App] 离线数据库已初始化')
    })
    .catch(error => {
      console.error('[App] 离线功能初始化失败:', error)
    })
}
