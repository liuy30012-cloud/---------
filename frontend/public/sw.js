/**
 * Service Worker - 离线功能支持
 * 实现 PWA 缓存策略和离线数据管理
 */

const CACHE_VERSION = 'v2'
const STATIC_CACHE = `library-static-${CACHE_VERSION}`
const API_CACHE = `library-api-${CACHE_VERSION}`
const APP_SHELL_CACHE = `library-shell-${CACHE_VERSION}`
const CACHE_PREFIX = 'library-'
const OFFLINE_JSON_HEADERS = {
  'Content-Type': 'application/json',
}

// 需要预缓存的静态资源
const STATIC_ASSETS = ['/', '/index.html', '/manifest.json']

const runtimeConfig = {
  apiPrefix: '/api/',
  hotBooksUrl: null,
}

const HOT_BOOKS_FALLBACK_MESSAGE = '热门书籍缓存不可用'

// 安装阶段 - 预缓存静态资源
self.addEventListener('install', (event) => {
  console.log('[SW] Installing service worker...')

  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => {
        console.log('[SW] Caching static assets')
        return cache.addAll(STATIC_ASSETS)
      })
      .then(() => self.skipWaiting()),
  )
})

// 激活阶段 - 清理旧缓存
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating service worker...')

  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((name) => name.startsWith(CACHE_PREFIX) && ![STATIC_CACHE, API_CACHE, APP_SHELL_CACHE].includes(name))
            .map((name) => {
              console.log('[SW] Deleting old cache:', name)
              return caches.delete(name)
            }),
        )
      })
      .then(() => self.clients.claim()),
  )
})

// 请求拦截 - 实现缓存策略
self.addEventListener('fetch', (event) => {
  const { request } = event

  if (request.method !== 'GET') {
    return
  }

  const url = new URL(request.url)

  // 只处理 HTTP/HTTPS 请求
  if (!url.protocol.startsWith('http')) {
    return
  }

  // 导航请求 - App Shell
  if (request.mode === 'navigate') {
    event.respondWith(networkFirst(request, APP_SHELL_CACHE, createOfflineDocumentResponse))
    return
  }

  // 静态资源 - Cache First
  if (
    request.destination === 'script'
    || request.destination === 'style'
    || request.destination === 'image'
    || request.destination === 'font'
  ) {
    event.respondWith(cacheFirst(request, STATIC_CACHE))
    return
  }

  if (isApiRequest(url)) {
    if (isHotBooksRequest(url)) {
      event.respondWith(staleWhileRevalidate(event, request, API_CACHE, createOfflineApiResponse(HOT_BOOKS_FALLBACK_MESSAGE)))
      return
    }

    if (isBookSearchRequest(url)) {
      event.respondWith(networkOnly(request, createOfflineApiResponse('搜索功能需要网络连接')))
      return
    }

    if (isBookDetailRequest(url) && request.method === 'GET') {
      event.respondWith(networkFirst(request, API_CACHE, createOfflineApiResponse()))
      return
    }

    event.respondWith(networkFirst(request, API_CACHE, createOfflineApiResponse()))
    return
  }

  event.respondWith(networkFirst(request, STATIC_CACHE))
})

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName)
  const cached = await cache.match(request)

  if (cached) {
    return cached
  }

  try {
    const response = await fetch(request)
    putInCache(cache, request, response)
    return response
  } catch (error) {
    console.error('[SW] Cache First failed:', error)
    return new Response('Offline', { status: 503 })
  }
}

async function networkFirst(request, cacheName, fallbackResponseFactory = createOfflineApiResponse()) {
  const cache = await caches.open(cacheName)

  try {
    const response = await fetch(request)
    putInCache(cache, request, response)
    return response
  } catch (error) {
    console.log('[SW] Network failed, trying cache:', request.url)
    const cached = await cache.match(request)

    if (cached) {
      return cached
    }

    return typeof fallbackResponseFactory === 'function'
      ? fallbackResponseFactory(request)
      : fallbackResponseFactory
  }
}

async function staleWhileRevalidate(event, request, cacheName, fallbackResponseFactory) {
  const cache = await caches.open(cacheName)
  const cached = await cache.match(request)

  const fetchPromise = fetch(request)
    .then((response) => {
      putInCache(cache, request, response)
      return response
    })
    .catch((error) => {
      console.log('[SW] Background update failed:', error)
      return typeof fallbackResponseFactory === 'function'
        ? fallbackResponseFactory(request)
        : fallbackResponseFactory
    })

  event.waitUntil(fetchPromise.then(() => undefined))
  return cached || fetchPromise
}

async function networkOnly(request, fallbackResponse) {
  try {
    return await fetch(request)
  } catch (error) {
    return fallbackResponse
  }
}

function isApiRequest(url) {
  const apiPrefix = normalizeApiPrefix(runtimeConfig.apiPrefix || '/api/')
  return apiPrefix !== '/' && url.pathname.startsWith(apiPrefix)
}

function isHotBooksRequest(url) {
  if (!runtimeConfig.hotBooksUrl) {
    return false
  }

  const hotBooksUrl = new URL(runtimeConfig.hotBooksUrl)
  return url.origin === hotBooksUrl.origin && url.pathname === hotBooksUrl.pathname && url.search === hotBooksUrl.search
}

function isBookSearchRequest(url) {
  return url.pathname === `${getBooksBasePath()}/search`
}

function isBookDetailRequest(url) {
  return url.pathname.startsWith(`${getBooksBasePath()}/`)
}

function getBooksBasePath() {
  return `${trimTrailingSlash(normalizeApiPrefix(runtimeConfig.apiPrefix || '/api/'))}/books`
}

function normalizeApiPrefix(value) {
  if (!value || value === '/') {
    return '/'
  }

  return value.endsWith('/') ? value : `${value}/`
}

function trimTrailingSlash(value) {
  return value.endsWith('/') ? value.slice(0, -1) : value
}

function putInCache(cache, request, response) {
  if (response && response.ok) {
    cache.put(request, response.clone())
  }
}

function createOfflineApiResponse(message = '当前处于离线状态，请检查网络连接') {
  return () => new Response(
    JSON.stringify({
      error: 'offline',
      message,
    }),
    {
      status: 503,
      headers: OFFLINE_JSON_HEADERS,
    },
  )
}

function createOfflineDocumentResponse() {
  return caches.match('/index.html').then((response) => {
    if (response) {
      return response
    }

    return new Response('Offline', {
      status: 503,
      headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    })
  })
}

// 消息处理 - 支持运行时配置和手动缓存更新
self.addEventListener('message', (event) => {
  const { data } = event

  if (!data || !data.type) {
    return
  }

  if (data.type === 'SKIP_WAITING') {
    self.skipWaiting()
    return
  }

  if (data.type === 'SET_RUNTIME_CONFIG') {
    runtimeConfig.apiPrefix = data.payload?.apiPrefix || runtimeConfig.apiPrefix
    runtimeConfig.hotBooksUrl = data.payload?.hotBooksUrl || null
    return
  }

  if (data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys()
        .then((cacheNames) => {
          return Promise.all(
            cacheNames
              .filter((name) => name.startsWith(CACHE_PREFIX))
              .map((name) => caches.delete(name)),
          )
        })
        .then(() => caches.open(STATIC_CACHE))
        .then((cache) => cache.addAll(STATIC_ASSETS))
        .then(() => {
          event.ports[0]?.postMessage({ success: true })
        }),
    )
    return
  }

  if (data.type === 'UPDATE_CACHE') {
    event.waitUntil(
      updateHotBooksCache()
        .then(() => {
          event.ports[0]?.postMessage({ success: true })
        })
        .catch((error) => {
          event.ports[0]?.postMessage({ success: false, error: error.message })
        }),
    )
  }
})

async function updateHotBooksCache() {
  if (!runtimeConfig.hotBooksUrl) {
    throw new Error('热门书籍缓存地址未配置')
  }

  const request = new Request(runtimeConfig.hotBooksUrl, {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })

  const response = await fetch(request)
  if (!response.ok) {
    throw new Error(`更新热门书籍缓存失败: ${response.status}`)
  }

  const cache = await caches.open(API_CACHE)
  await cache.put(request, response.clone())

  const hotBooksUrl = new URL(runtimeConfig.hotBooksUrl)
  const relativeRequest = new Request(`${hotBooksUrl.pathname}${hotBooksUrl.search}`, {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })
  await cache.put(relativeRequest, response.clone())
}
