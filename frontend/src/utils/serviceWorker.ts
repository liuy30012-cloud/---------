/**
 * Service Worker 注册工具
 * 在应用启动时注册 Service Worker
 */

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  // 检查浏览器支持
  if (!('serviceWorker' in navigator)) {
    console.warn('[SW] Service Worker 不支持');
    return null;
  }

  try {
    // 注册 Service Worker
    const registration = await navigator.serviceWorker.register('/sw.js', {
      scope: '/'
    });

    console.log('[SW] Service Worker 注册成功:', registration.scope);

    // 监听更新
    registration.addEventListener('updatefound', () => {
      const newWorker = registration.installing;
      if (!newWorker) return;

      newWorker.addEventListener('statechange', () => {
        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
          console.log('[SW] 发现新版本,准备更新');

          // 提示用户刷新页面
          if (confirm('发现新版本,是否立即更新?')) {
            newWorker.postMessage({ type: 'SKIP_WAITING' });
            window.location.reload();
          }
        }
      });
    });

    // 监听控制器变化
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      console.log('[SW] Service Worker 已更新');
    });

    return registration;
  } catch (error) {
    console.error('[SW] Service Worker 注册失败:', error);
    return null;
  }
}

/**
 * 注销 Service Worker
 */
export async function unregisterServiceWorker(): Promise<boolean> {
  if (!('serviceWorker' in navigator)) {
    return false;
  }

  try {
    const registration = await navigator.serviceWorker.ready;
    const success = await registration.unregister();
    console.log('[SW] Service Worker 注销:', success);
    return success;
  } catch (error) {
    console.error('[SW] Service Worker 注销失败:', error);
    return false;
  }
}
