/**
 * Service Worker - 离线功能支持
 * 实现 PWA 缓存策略和离线数据管理
 */

const CACHE_VERSION = 'v1';
const STATIC_CACHE = `library-static-${CACHE_VERSION}`;
const API_CACHE = `library-api-${CACHE_VERSION}`;

// 需要预缓存的静态资源
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json'
];

// API 端点配置
const API_BASE = 'http://localhost:8080/api';
const HOT_BOOKS_ENDPOINT = `${API_BASE}/books/hot`;

// 安装阶段 - 预缓存静态资源
self.addEventListener('install', (event) => {
  console.log('[SW] Installing service worker...');

  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then(cache => {
        console.log('[SW] Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => self.skipWaiting())
  );
});

// 激活阶段 - 清理旧缓存
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating service worker...');

  event.waitUntil(
    caches.keys()
      .then(cacheNames => {
        return Promise.all(
          cacheNames
            .filter(name => name.startsWith('library-') && name !== STATIC_CACHE && name !== API_CACHE)
            .map(name => {
              console.log('[SW] Deleting old cache:', name);
              return caches.delete(name);
            })
        );
      })
      .then(() => self.clients.claim())
  );
});

// 请求拦截 - 实现缓存策略
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // 只处理 HTTP/HTTPS 请求
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // 静态资源 - Cache First
  if (request.destination === 'script' ||
      request.destination === 'style' ||
      request.destination === 'image' ||
      request.destination === 'font') {
    event.respondWith(cacheFirst(request, STATIC_CACHE));
    return;
  }

  // API 请求处理
  if (url.origin === new URL(API_BASE).origin) {
    // 热门书籍 - Stale While Revalidate
    if (url.pathname.includes('/books/hot')) {
      event.respondWith(staleWhileRevalidate(request, API_CACHE));
      return;
    }

    // 书籍详情 - Network First
    if (url.pathname.includes('/books/') && request.method === 'GET') {
      event.respondWith(networkFirst(request, API_CACHE));
      return;
    }

    // 搜索请求 - Network Only (离线时返回提示)
    if (url.pathname.includes('/books/search')) {
      event.respondWith(networkOnly(request));
      return;
    }

    // 其他 API 请求 - Network First
    event.respondWith(networkFirst(request, API_CACHE));
    return;
  }

  // 默认策略 - Network First
  event.respondWith(networkFirst(request, STATIC_CACHE));
});

// 缓存策略实现

/**
 * Cache First - 优先使用缓存
 * 适用于静态资源
 */
async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);

  if (cached) {
    return cached;
  }

  try {
    const response = await fetch(request);
    if (response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    console.error('[SW] Cache First failed:', error);
    return new Response('Offline', { status: 503 });
  }
}

/**
 * Network First - 优先使用网络
 * 适用于需要最新数据的请求
 */
async function networkFirst(request, cacheName) {
  const cache = await caches.open(cacheName);

  try {
    const response = await fetch(request);
    if (response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    console.log('[SW] Network failed, trying cache:', request.url);
    const cached = await cache.match(request);

    if (cached) {
      return cached;
    }

    // 返回离线提示
    return new Response(
      JSON.stringify({
        error: 'offline',
        message: '当前处于离线状态，请检查网络连接'
      }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}

/**
 * Stale While Revalidate - 返回缓存同时后台更新
 * 适用于热门数据
 */
async function staleWhileRevalidate(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);

  // 后台更新
  const fetchPromise = fetch(request)
    .then(response => {
      if (response.ok) {
        cache.put(request, response.clone());
      }
      return response;
    })
    .catch(error => {
      console.log('[SW] Background update failed:', error);
    });

  // 立即返回缓存，如果没有缓存则等待网络请求
  return cached || fetchPromise;
}

/**
 * Network Only - 仅使用网络
 * 适用于必须实时的请求
 */
async function networkOnly(request) {
  try {
    return await fetch(request);
  } catch (error) {
    return new Response(
      JSON.stringify({
        error: 'offline',
        message: '搜索功能需要网络连接'
      }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}

// 消息处理 - 支持手动缓存更新
self.addEventListener('message', (event) => {
  if (event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (event.data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys()
        .then(cacheNames => {
          return Promise.all(
            cacheNames
              .filter(name => name.startsWith('library-'))
              .map(name => caches.delete(name))
          );
        })
        .then(() => {
          event.ports[0].postMessage({ success: true });
        })
    );
  }

  if (event.data.type === 'UPDATE_CACHE') {
    event.waitUntil(
      fetch(HOT_BOOKS_ENDPOINT)
        .then(response => {
          if (response.ok) {
            return caches.open(API_CACHE)
              .then(cache => cache.put(HOT_BOOKS_ENDPOINT, response));
          }
        })
        .then(() => {
          event.ports[0].postMessage({ success: true });
        })
        .catch(error => {
          event.ports[0].postMessage({ success: false, error: error.message });
        })
    );
  }
});
