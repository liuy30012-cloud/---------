/**
 * 网络状态监听 Composable
 * 提供在线/离线状态检测和自动更新功能
 */

import { ref, onMounted, onUnmounted } from 'vue';
import { offlineDB } from '@/utils/offlineDB';

export function useNetworkStatus() {
  const isOnline = ref(navigator.onLine);
  const showOfflineNotice = ref(false);
  const lastOnlineTime = ref<number>(Date.now());

  // 网络状态变化处理
  const handleOnline = async () => {
    isOnline.value = true;
    showOfflineNotice.value = false;

    console.log('[Network] 网络已恢复');

    // 自动更新热门书籍缓存
    try {
      await updateHotBooksCache();
      console.log('[Network] 热门书籍缓存已更新');
    } catch (error) {
      console.error('[Network] 更新缓存失败:', error);
    }

    lastOnlineTime.value = Date.now();
  };

  const handleOffline = () => {
    isOnline.value = false;
    console.log('[Network] 网络已断开');
  };

  // 更新热门书籍缓存
  const updateHotBooksCache = async (): Promise<void> => {
    if (!('serviceWorker' in navigator)) {
      return;
    }

    const registration = await navigator.serviceWorker.ready;
    const activeWorker = registration.active;
    if (!activeWorker) {
      return;
    }

    return new Promise((resolve, reject) => {
      const messageChannel = new MessageChannel();

      messageChannel.port1.onmessage = (event) => {
        if (event.data.success) {
          resolve();
        } else {
          reject(new Error(event.data.error));
        }
      };

      activeWorker.postMessage(
        { type: 'UPDATE_CACHE' },
        [messageChannel.port2]
      );
    });
  };

  // 手动触发缓存更新
  const manualUpdateCache = async (): Promise<boolean> => {
    if (!isOnline.value) {
      return false;
    }

    try {
      await updateHotBooksCache();
      return true;
    } catch (error) {
      console.error('[Network] 手动更新缓存失败:', error);
      return false;
    }
  };

  // 清空所有缓存
  const clearAllCache = async (): Promise<boolean> => {
    try {
      // 清空 IndexedDB
      await offlineDB.clearAllCache();

      // 清空 Service Worker 缓存
      if ('serviceWorker' in navigator) {
        const registration = await navigator.serviceWorker.ready;
        const activeWorker = registration.active;
        if (activeWorker) {
          return new Promise((resolve) => {
            const messageChannel = new MessageChannel();

            messageChannel.port1.onmessage = (event) => {
              resolve(event.data.success);
            };

            activeWorker.postMessage(
              { type: 'CLEAR_CACHE' },
              [messageChannel.port2]
            );
          });
        }
      }

      return true;
    } catch (error) {
      console.error('[Network] 清空缓存失败:', error);
      return false;
    }
  };

  // 检查是否需要显示离线提示
  const checkOfflineAction = (actionName: string): boolean => {
    if (!isOnline.value) {
      showOfflineNotice.value = true;
      console.warn(`[Network] 操作 "${actionName}" 需要网络连接`);
      return false;
    }
    return true;
  };

  // 获取缓存统计信息
  const getCacheStats = async () => {
    try {
      const stats = await offlineDB.getCacheStats();
      const lastUpdate = await offlineDB.getMetadata('hot_books_last_update');

      return {
        ...stats,
        lastUpdate: lastUpdate || null
      };
    } catch (error) {
      console.error('[Network] 获取缓存统计失败:', error);
      return {
        bookCount: 0,
        hotBookCount: 0,
        cacheSize: 0,
        lastUpdate: null
      };
    }
  };

  // 生命周期钩子
  onMounted(() => {
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // 初始化时如果在线,更新缓存
    if (isOnline.value) {
      updateHotBooksCache().catch(console.error);
    }
  });

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  });

  return {
    isOnline,
    showOfflineNotice,
    lastOnlineTime,
    checkOfflineAction,
    manualUpdateCache,
    clearAllCache,
    getCacheStats
  };
}
