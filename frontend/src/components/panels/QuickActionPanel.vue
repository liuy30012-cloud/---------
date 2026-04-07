<template>
  <div class="quick-action-panel">
    <!-- 快捷面板触发按钮 -->
    <button
      class="quick-action-btn"
      :class="{ active: isPanelOpen }"
      @click="togglePanel"
      title="快捷操作面板 (Alt+Q)"
    >
      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <polyline points="12 6 12 12 16 14"></polyline>
      </svg>
      <span>快捷</span>
    </button>

    <!-- 快捷操作面板 -->
    <transition name="slide">
      <div v-if="isPanelOpen" class="quick-action-content">
        <div class="panel-header">
          <h3>快捷操作</h3>
          <button class="close-btn" @click="togglePanel">×</button>
        </div>

        <div class="panel-body">
          <!-- 快速续借 -->
          <div class="action-group">
            <h4>我的借阅</h4>
            <div class="action-list">
              <button
                class="action-item"
                @click="goToMyBorrows"
                title="查看我的借阅记录 (Alt+B)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                </svg>
                <span>我的借阅</span>
              </button>
              <button
                class="action-item"
                @click="showQuickRenew"
                title="快速续借 (Alt+R)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
                  <path d="M3 3v5h5"></path>
                </svg>
                <span>快速续借</span>
              </button>
            </div>
          </div>

          <!-- 预约相关 -->
          <div class="action-group">
            <h4>预约管理</h4>
            <div class="action-list">
              <button
                class="action-item"
                @click="goToMyReservations"
                title="查看我的预约 (Alt+P)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>我的预约</span>
              </button>
              <button
                class="action-item"
                @click="goToSearch"
                title="新建搜索 (Alt+S)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <path d="m21 21-4.35-4.35"></path>
                </svg>
                <span>新建搜索</span>
              </button>
            </div>
          </div>

          <!-- 快速搜索历史 -->
          <div class="action-group" v-if="searchHistory.length > 0">
            <h4>最近搜索</h4>
            <div class="search-history">
              <button
                v-for="(item, index) in searchHistory.slice(0, 5)"
                :key="index"
                class="history-item"
                @click="quickSearch(item)"
                :title="`快速搜索: ${item}`"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <circle cx="11" cy="11" r="8"></circle>
                  <path d="m21 21-4.35-4.35"></path>
                </svg>
                {{ item }}
              </button>
            </div>
          </div>

          <!-- 账户相关 -->
          <div class="action-group">
            <h4>账户</h4>
            <div class="action-list">
              <button
                class="action-item"
                @click="goToAccount"
                title="我的账户 (Alt+A)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>我的账户</span>
              </button>
              <button
                class="action-item"
                @click="logout"
                title="退出登录 (Alt+L)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </div>
        </div>

        <div class="panel-footer">
          <small>💡 提示：使用快捷键快速访问功能</small>
        </div>
      </div>
    </transition>

    <!-- 快速续借模态框 -->
    <QuickRenewModal
      v-if="showRenewModal"
      @close="showRenewModal = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import QuickRenewModal from './QuickRenewModal.vue';

const router = useRouter();
const userStore = useUserStore();

const isPanelOpen = ref(false);
const showRenewModal = ref(false);
const searchHistory = ref<string[]>([]);

// 从本地存储加载搜索历史
onMounted(() => {
  loadSearchHistory();
  registerKeyboardShortcuts();
});

onBeforeUnmount(() => {
  unregisterKeyboardShortcuts();
});

const togglePanel = () => {
  isPanelOpen.value = !isPanelOpen.value;
};

const loadSearchHistory = () => {
  const history = localStorage.getItem('searchHistory');
  if (history) {
    searchHistory.value = JSON.parse(history).slice(0, 10);
  }
};

const quickSearch = (keyword: string) => {
  router.push({
    name: 'BookSearch',
    query: { keyword }
  });
  isPanelOpen.value = false;
};

const goToMyBorrows = () => {
  router.push({ name: 'MyBorrows' });
  isPanelOpen.value = false;
};

const goToMyReservations = () => {
  router.push({ name: 'MyReservations' });
  isPanelOpen.value = false;
};

const goToSearch = () => {
  router.push({ name: 'BookSearch' });
  isPanelOpen.value = false;
};

const goToAccount = () => {
  router.push({ name: 'MyAccount' });
  isPanelOpen.value = false;
};

const showQuickRenew = () => {
  showRenewModal.value = true;
  isPanelOpen.value = false;
};

const logout = () => {
  userStore.logout();
  router.push({ name: 'Login' });
};

// 键盘快捷键处理
const handleKeyDown = (event: KeyboardEvent) => {
  if (!event.altKey) return;

  switch (event.key.toLowerCase()) {
    case 'q':
      togglePanel();
      event.preventDefault();
      break;
    case 'b':
      goToMyBorrows();
      event.preventDefault();
      break;
    case 'r':
      showQuickRenew();
      event.preventDefault();
      break;
    case 'p':
      goToMyReservations();
      event.preventDefault();
      break;
    case 's':
      goToSearch();
      event.preventDefault();
      break;
    case 'a':
      goToAccount();
      event.preventDefault();
      break;
    case 'l':
      logout();
      event.preventDefault();
      break;
  }
};

const registerKeyboardShortcuts = () => {
  window.addEventListener('keydown', handleKeyDown);
};

const unregisterKeyboardShortcuts = () => {
  window.removeEventListener('keydown', handleKeyDown);
};
</script>

<style scoped lang="scss">
.quick-action-panel {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

.quick-action-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  font-weight: 500;

  &:hover:not(.active) {
    transform: scale(1.1);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  }

  &.active {
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  }

  .icon {
    width: 24px;
    height: 24px;
  }
}

.quick-action-content {
  position: absolute;
  bottom: 80px;
  right: 0;
  width: 320px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    color: #999;
    cursor: pointer;
    padding: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      color: #333;
    }
  }
}

.panel-body {
  max-height: 500px;
  overflow-y: auto;
  padding: 12px 0;
}

.action-group {
  padding: 12px 16px;

  h4 {
    margin: 0 0 8px 0;
    font-size: 12px;
    font-weight: 600;
    color: #999;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &:not(:last-child) {
    border-bottom: 1px solid #f5f5f5;
  }
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f9f9f9;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  transition: all 0.2s ease;

  svg {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
    color: #667eea;
  }

  &:hover {
    background: #f0f4ff;
    border-color: #667eea;
    transform: translateX(4px);
  }

  &:active {
    transform: translateX(4px) scale(0.98);
  }
}

.search-history {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f9f9f9;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s ease;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  svg {
    color: #999;
    flex-shrink: 0;
  }

  &:hover {
    background: #f0f4ff;
    color: #333;
    border-color: #667eea;
  }
}

.panel-footer {
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  font-size: 12px;
  color: #999;
  text-align: center;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
