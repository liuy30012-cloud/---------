/**
 * Service Worker 注册工具
 * 在应用启动时注册 Service Worker
 */

import { OFFLINE_SYNC_PATHS } from '../config'

const SW_SCRIPT_URL = '/sw.js'
const SW_SCOPE = '/'
const HOT_BOOKS_SYNC_URL = new URL(OFFLINE_SYNC_PATHS.hotBooks, window.location.origin).toString()

function resolveApiPrefix(): string {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

  try {
    const resolved = new URL(baseURL, window.location.origin)
    const normalizedPath = resolved.pathname.endsWith('/') ? resolved.pathname : `${resolved.pathname}/`
    return normalizedPath === '//' ? '/' : normalizedPath
  } catch {
    return '/'
  }
}

const API_PREFIX = resolveApiPrefix()
let hasControllerChangeListener = false
let hasReloadedForControllerChange = false

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  // 检查浏览器支持
  if (!('serviceWorker' in navigator)) {
    console.warn('[SW] Service Worker 不支持')
    return null
  }

  try {
    // 注册 Service Worker
    const registration = await navigator.serviceWorker.register(SW_SCRIPT_URL, {
      scope: SW_SCOPE,
    })

    console.log('[SW] Service Worker 注册成功:', registration.scope)

    const syncHotBooksCache = (worker: ServiceWorker | null) => {
      if (!worker) {
        return
      }

      worker.postMessage({
        type: 'SET_RUNTIME_CONFIG',
        payload: {
          apiPrefix: API_PREFIX,
          hotBooksUrl: HOT_BOOKS_SYNC_URL,
        },
      })
    }

    const applyRuntimeConfig = () => {
      syncHotBooksCache(registration.installing)
      syncHotBooksCache(registration.waiting)
      syncHotBooksCache(registration.active ?? navigator.serviceWorker.controller)
    }

    applyRuntimeConfig()

    // 监听更新
    registration.addEventListener('updatefound', () => {
      const newWorker = registration.installing
      if (!newWorker) return

      syncHotBooksCache(newWorker)

      newWorker.addEventListener('statechange', () => {
        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
          console.log('[SW] 发现新版本,准备更新')

          // 提示用户刷新页面
          if (confirm('发现新版本,是否立即更新?')) {
            newWorker.postMessage({ type: 'SKIP_WAITING' })
          }
        }
      })
    })

    if (!hasControllerChangeListener) {
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (hasReloadedForControllerChange) {
          return
        }

        hasReloadedForControllerChange = true
        console.log('[SW] Service Worker 已更新')
        applyRuntimeConfig()
        window.location.reload()
      })
      hasControllerChangeListener = true
    }

    return registration
  } catch (error) {
    console.error('[SW] Service Worker 注册失败:', error)
    return null
  }
}

/**
 * 注销 Service Worker
 */
export async function unregisterServiceWorker(): Promise<boolean> {
  if (!('serviceWorker' in navigator)) {
    return false
  }

  try {
    const registration = await navigator.serviceWorker.ready
    const success = await registration.unregister()
    console.log('[SW] Service Worker 注销:', success)
    return success
  } catch (error) {
    console.error('[SW] Service Worker 注销失败:', error)
    return false
  }
}
