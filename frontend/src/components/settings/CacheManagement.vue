<template>
  <div class="cache-management">
    <div class="cache-header">
      <h2>离线数据管理</h2>
      <p class="cache-description">管理离线缓存的书籍数据,提升弱网环境下的使用体验</p>
    </div>

    <div class="cache-stats">
      <div class="stat-card">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.bookCount }}</div>
          <div class="stat-label">已缓存书籍</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">🔥</div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.hotBookCount }}</div>
          <div class="stat-label">热门书籍</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">🕐</div>
        <div class="stat-content">
          <div class="stat-value">{{ lastUpdateText }}</div>
          <div class="stat-label">最后更新</div>
        </div>
      </div>
    </div>

    <div class="cache-actions">
      <button
        class="action-button action-button--primary"
        :disabled="!isOnline || isUpdating"
        @click="handleUpdateCache"
      >
        <svg v-if="!isUpdating" class="button-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <svg v-else class="button-icon button-icon--spin" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        <span>{{ isUpdating ? '更新中...' : '立即更新缓存' }}</span>
      </button>

      <button
        class="action-button action-button--danger"
        :disabled="isClearing"
        @click="handleClearCache"
      >
        <svg v-if="!isClearing" class="button-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        <svg v-else class="button-icon button-icon--spin" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <circle cx="12" cy="12" r="10" stroke-width="2"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6l4 2" />
        </svg>
        <span>{{ isClearing ? '清空中...' : '清空所有缓存' }}</span>
      </button>
    </div>

    <div v-if="message" class="cache-message" :class="messageType">
      <svg v-if="messageType === 'success'" class="message-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      <svg v-else-if="messageType === 'error'" class="message-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
        <circle cx="12" cy="12" r="10" stroke-width="2"/>
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01" />
      </svg>
      <span>{{ message }}</span>
    </div>

    <div class="cache-info">
      <h3>离线功能说明</h3>
      <ul>
        <li>系统会自动缓存您浏览过的书籍信息</li>
        <li>热门书籍会在联网时自动更新</li>
        <li>离线状态下可以查看已缓存的书籍详情</li>
        <li>借阅、预约等操作需要网络连接</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useNetworkStatus } from '@/composables/useNetworkStatus';

const { isOnline, manualUpdateCache, clearAllCache, getCacheStats } = useNetworkStatus();

const stats = ref({
  bookCount: 0,
  hotBookCount: 0,
  cacheSize: 0,
  lastUpdate: null as number | null
});

const isUpdating = ref(false);
const isClearing = ref(false);
const message = ref('');
const messageType = ref<'success' | 'error'>('success');

const lastUpdateText = computed(() => {
  if (!stats.value.lastUpdate) {
    return '从未更新';
  }

  const now = Date.now();
  const diff = now - stats.value.lastUpdate;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  return `${days}天前`;
});

const loadStats = async () => {
  try {
    const data = await getCacheStats();
    stats.value = data;
  } catch (error) {
    console.error('加载缓存统计失败:', error);
  }
};

const handleUpdateCache = async () => {
  if (!isOnline.value) {
    showMessage('需要网络连接才能更新缓存', 'error');
    return;
  }

  isUpdating.value = true;
  message.value = '';

  try {
    const success = await manualUpdateCache();
    if (success) {
      showMessage('缓存更新成功', 'success');
      await loadStats();
    } else {
      showMessage('缓存更新失败,请稍后重试', 'error');
    }
  } catch (error) {
    console.error('更新缓存失败:', error);
    showMessage('缓存更新失败', 'error');
  } finally {
    isUpdating.value = false;
  }
};

const handleClearCache = async () => {
  if (!confirm('确定要清空所有缓存吗?此操作不可恢复。')) {
    return;
  }

  isClearing.value = true;
  message.value = '';

  try {
    const success = await clearAllCache();
    if (success) {
      showMessage('缓存已清空', 'success');
      await loadStats();
    } else {
      showMessage('清空缓存失败', 'error');
    }
  } catch (error) {
    console.error('清空缓存失败:', error);
    showMessage('清空缓存失败', 'error');
  } finally {
    isClearing.value = false;
  }
};

const showMessage = (text: string, type: 'success' | 'error') => {
  message.value = text;
  messageType.value = type;

  setTimeout(() => {
    message.value = '';
  }, 3000);
};

onMounted(() => {
  loadStats();
});
</script>

<style scoped>
.cache-management {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.cache-header {
  margin-bottom: 32px;
}

.cache-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.cache-description {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.cache-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.cache-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.action-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-button--primary:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.action-button--danger {
  background: white;
  color: #dc2626;
  border: 1px solid #dc2626;
}

.action-button--danger:not(:disabled):hover {
  background: #dc2626;
  color: white;
}

.button-icon {
  width: 20px;
  height: 20px;
}

.button-icon--spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cache-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 14px;
}

.cache-message.success {
  background: #d1fae5;
  color: #065f46;
}

.cache-message.error {
  background: #fee2e2;
  color: #991b1b;
}

.message-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.cache-info {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.cache-info h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.cache-info ul {
  margin: 0;
  padding-left: 20px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.8;
}

.cache-info li {
  margin-bottom: 4px;
}
</style>
