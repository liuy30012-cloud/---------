import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import i18n from './i18n'
import { createRevealDirective, initMotion } from './motion'
import { useUserStore } from './stores/user'
import { registerServiceWorker } from './utils/serviceWorker'
import { offlineDB } from './utils/offlineDB'
import './styles/variables.css'
import './dizhi/dizhi.css'

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
