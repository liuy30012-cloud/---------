<template>
  <nav ref="navRef" class="top-nav" :class="{ 'top-nav--scrolled': isScrolled }" aria-label="Primary">
    <div class="nav-container">
      <div class="nav-left">
        <RouterLink to="/" class="brand-wrapper brand-link" aria-label="Home">
          <div class="photo-logo">
            <img src="/school-badge.png" alt="Logo" />
          </div>
          <div class="brand-identity">
            <span class="brand-kicker">{{ locale === 'zh' ? '馆藏展陈系统' : 'Archive Exhibition System' }}</span>
            <span class="brand-logo">中国劳动关系学院</span>
          </div>
        </RouterLink>

        <div ref="linksRef" class="nav-links">
          <RouterLink
            v-for="item in visibleNavItems"
            :key="item.name"
            :to="item.to"
            class="nav-link"
            :class="{ active: isActive(item) }"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </div>

      <div class="nav-right">
        <div ref="actionsRef" class="action-icons">
          <button
            class="icon-btn font-bold text-lg"
            type="button"
            :aria-label="locale === 'zh' ? '切换语言' : 'Switch language'"
            @click="toggleLang"
          >
            {{ $t('nav.lang') }}
          </button>

          <div class="icon-btn-wrapper">
            <button
              class="icon-btn"
              ref="notifBtnRef"
              type="button"
              :aria-label="locale === 'zh' ? '打开通知面板' : 'Open notifications'"
              @click="props.notif.toggleNotifPanel(closeHistoryPanel)"
            >
              <span class="material-symbols-outlined">notifications</span>
              <span v-if="props.notif.unreadCount.value > 0" class="unread-badge">{{ props.notif.unreadCount.value }}</span>
            </button>
            <Transition name="popup">
              <div
                v-if="props.notif.showNotifPanel.value"
                ref="notifPanelRef"
                class="popup-panel notif-panel"
                role="dialog"
                aria-modal="false"
                :aria-label="locale === 'zh' ? '通知面板' : 'Notifications panel'"
              >
                <div class="popup-header">
                  <h4 class="popup-title">{{ $t('notifications.title') }}</h4>
                  <button v-if="props.notif.unreadCount.value > 0" class="popup-action-btn" type="button" @click="props.notif.markAllRead()">
                    {{ $t('notifications.markAllRead') }}
                  </button>
                </div>
                <div class="popup-body">
                  <button
                    v-for="n in props.notif.notifications.value"
                    :key="n.id"
                    :class="['notif-item', { 'notif-read': n.read }]"
                    type="button"
                    :aria-label="locale === 'zh' ? `打开通知：${props.notif.getNotifTitle(n)}` : `Open notification: ${props.notif.getNotifTitle(n)}`"
                    @click="openNotification(n)"
                  >
                    <span class="material-symbols-outlined notif-icon">{{ n.icon }}</span>
                    <div class="notif-content">
                      <p class="notif-title-text">{{ props.notif.getNotifTitle(n) }}</p>
                      <p class="notif-desc">{{ props.notif.getNotifDesc(n) }}</p>
                      <span class="notif-time">{{ props.notif.formatNotifTime(n.createdAt) }}</span>
                    </div>
                    <span v-if="!n.read" class="notif-dot"></span>
                  </button>
                </div>
              </div>
            </Transition>
          </div>

          <div class="icon-btn-wrapper">
            <button
              class="icon-btn"
              ref="historyBtnRef"
              type="button"
              :aria-label="locale === 'zh' ? '打开搜索历史' : 'Open search history'"
              @click="props.history.toggleHistoryPanel(closeNotifPanel)"
            >
              <span class="material-symbols-outlined">history</span>
            </button>
            <Transition name="popup">
              <div
                v-if="props.history.showHistoryPanel.value"
                ref="historyPanelRef"
                class="popup-panel history-panel"
                role="dialog"
                aria-modal="false"
                :aria-label="locale === 'zh' ? '搜索历史面板' : 'Search history panel'"
              >
                <div class="popup-header">
                  <h4 class="popup-title">{{ $t('history.title') }}</h4>
                  <button v-if="props.history.searchHistory.value.length > 0" class="popup-action-btn" type="button" @click="props.history.clearHistory()">
                    {{ $t('history.clear') }}
                  </button>
                </div>
                <div class="popup-body">
                  <div v-if="props.history.searchHistory.value.length === 0" class="popup-empty">
                    <span class="material-symbols-outlined empty-icon">manage_search</span>
                    <p>{{ $t('history.empty') }}</p>
                  </div>
                  <button
                    v-for="item in props.history.searchHistory.value"
                    :key="(item.id || item.keyword) + item.timestamp.getTime()"
                    class="history-item"
                    type="button"
                    :aria-label="locale === 'zh' ? `重新搜索：${item.label || item.keyword}` : `Search again: ${item.label || item.keyword}`"
                    @click="openHistoryItem(item)"
                  >
                    <span class="material-symbols-outlined history-item-icon">
                      {{ item.saved ? 'bookmark' : 'search' }}
                    </span>
                    <div class="history-item-content">
                      <span class="history-keyword">{{ item.label || item.keyword }}</span>
                      <span class="history-meta">
                        {{ item.resultCount }} {{ $t('history.results') }} · {{ props.history.formatHistoryTime(item.timestamp) }}
                      </span>
                    </div>
                    <span class="material-symbols-outlined history-arrow">north_west</span>
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div ref="avatarRef" class="avatar-wrapper">
          <button
            v-if="!userStore.isLoggedIn"
            class="login-btn"
            type="button"
            :aria-label="locale === 'zh' ? '前往登录或注册' : 'Go to sign in or register'"
            @click="goToLogin"
          >
            {{ locale === 'zh' ? '登录/注册' : 'Sign In' }}
          </button>

          <div v-else class="user-menu">
            <button
              class="user-avatar-btn"
              ref="userBtnRef"
              type="button"
              :aria-label="locale === 'zh' ? '打开用户菜单' : 'Open user menu'"
              @click="toggleUserMenu"
            >
              <img
                v-if="userStore.user?.avatarUrl"
                class="avatar-img"
                :alt="userStore.user.username"
                :src="userStore.user.avatarUrl"
                @error="onAvatarError"
              />
              <div v-else class="avatar-placeholder">{{ userStore.user?.username?.charAt(0) || 'U' }}</div>
            </button>
            <Transition name="popup">
              <div
                v-if="showUserMenu"
                ref="userMenuRef"
                class="user-dropdown"
                role="dialog"
                aria-modal="false"
                :aria-label="locale === 'zh' ? '用户菜单' : 'User menu'"
              >
                <div class="user-info">
                  <p class="user-name">{{ userStore.user?.username }}</p>
                  <p class="user-id">{{ userStore.user?.studentId }}</p>
                </div>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" type="button" @click="openAccount">
                  <span class="material-symbols-outlined">manage_accounts</span>
                  <span>{{ locale === 'zh' ? '我的账号' : 'My Account' }}</span>
                </button>
                <button class="dropdown-item" type="button" @click="openBookshelf">
                  <span class="material-symbols-outlined">favorite</span>
                  <span>{{ locale === 'zh' ? '我的书架' : 'My Bookshelf' }}</span>
                </button>
                <button class="dropdown-item" type="button" @click="handleLogout">
                  <span class="material-symbols-outlined">logout</span>
                  <span>{{ locale === 'zh' ? '退出登录' : 'Sign Out' }}</span>
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { gsap, prefersReducedMotion } from '../../motion'
import { useUserStore } from '../../stores/user'
import { handleImageError } from '../../utils/imageHelpers'

const props = defineProps({
  notif: { type: Object, required: true },
  history: { type: Object, required: true },
})

const { locale } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const petEventBus = inject('petEventBus') as any
const navRef = ref<HTMLElement | null>(null)
const linksRef = ref<HTMLElement | null>(null)
const actionsRef = ref<HTMLElement | null>(null)
const avatarRef = ref<HTMLElement | null>(null)
const isScrolled = ref(false)

const navItems = computed(() => {
  const zh = locale.value === 'zh'
  return [
    { name: 'Home', to: '/', label: zh ? '首页' : 'Home' },
    { name: 'BookSearch', to: '/books/search', label: zh ? '图书检索' : 'Search' },
    { name: 'MyBorrows', to: '/my-borrows', label: zh ? '我的借阅' : 'Borrows' },
    { name: 'MyReservations', to: '/my-reservations', label: zh ? '我的预约' : 'Reservations' },
    { name: 'MyBookshelf', to: '/my-bookshelf', label: zh ? '我的书架' : 'Bookshelf' },
    { name: 'MyAccount', to: '/my-account', label: zh ? '我的账号' : 'Account' },
    { name: 'Dashboard', to: '/dashboard', label: zh ? '数据分析' : 'Dashboard' },
    { name: 'InventoryAlerts', to: '/inventory-alerts', label: zh ? '库存预警' : 'Alerts' },
    { name: 'PurchaseSuggestions', to: '/purchase-suggestions', label: zh ? '采购建议' : 'Purchases' },
  ]
})

const adminOnlyPages = new Set(['Dashboard', 'InventoryAlerts'])
const authOnlyPages = new Set(['MyBorrows', 'MyReservations', 'MyBookshelf', 'MyAccount', 'PurchaseSuggestions'])
const visibleNavItems = computed(() => navItems.value.filter((item) => {
  if (adminOnlyPages.has(item.name) && !userStore.isAdmin) {
    return false
  }
  if (authOnlyPages.has(item.name) && !userStore.isLoggedIn) {
    return false
  }
  return true
}))

const notifPanelRef = ref<HTMLElement | null>(null)
const historyPanelRef = ref<HTMLElement | null>(null)
const notifBtnRef = ref<HTMLElement | null>(null)
const historyBtnRef = ref<HTMLElement | null>(null)
const userBtnRef = ref<HTMLElement | null>(null)
const userMenuRef = ref<HTMLElement | null>(null)
const showUserMenu = ref(false)

const isActive = (item: { name: string }) => {
  if (item.name === 'BookSearch' && route.name === 'BookDetail') {
    return true
  }
  return route.name === item.name
}

const toggleLang = () => {
  locale.value = locale.value === 'en' ? 'zh' : 'en'
  if (petEventBus?.value) {
    petEventBus.value.push({ event: 'lang:switch', ts: Date.now() })
  }
}

let scrollTicking = false

const updateScrollState = () => {
  isScrolled.value = typeof window !== 'undefined' && window.scrollY > 24
}

const handleWindowScroll = () => {
  if (scrollTicking) return

  scrollTicking = true
  window.requestAnimationFrame(() => {
    updateScrollState()
    scrollTicking = false
  })
}

const animateNavEntrance = () => {
  if (prefersReducedMotion() || !navRef.value) {
    return
  }

  const brandTarget = navRef.value.querySelector('.brand-wrapper')
  const revealTargets = [brandTarget, linksRef.value, actionsRef.value, avatarRef.value].filter(Boolean)

  gsap.fromTo(
    navRef.value,
    {
      autoAlpha: 0,
      y: -18,
      filter: 'blur(12px)',
    },
    {
      autoAlpha: 1,
      y: 0,
      filter: 'blur(0px)',
      duration: 0.58,
      ease: 'power3.out',
      clearProps: 'filter',
    }
  )

  if (revealTargets.length > 0) {
    gsap.fromTo(
      revealTargets,
      {
        autoAlpha: 0,
        y: -12,
      },
      {
        autoAlpha: 1,
        y: 0,
        duration: 0.48,
        ease: 'power2.out',
        stagger: 0.06,
        delay: 0.08,
      }
    )
  }
}

const closeNotifPanel = () => {
  props.notif.showNotifPanel.value = false
}

const closeHistoryPanel = () => {
  props.history.showHistoryPanel.value = false
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const goToLogin = () => {
  router.push('/login').catch(() => {})
}

const onAvatarError = (e: Event) => handleImageError(e)

const openAccount = () => {
  showUserMenu.value = false
  router.push({ name: 'MyAccount' }).catch(() => {})
}

const openBookshelf = () => {
  showUserMenu.value = false
  router.push({ name: 'MyBookshelf' }).catch(() => {})
}

const handleLogout = async () => {
  await userStore.logout()
  showUserMenu.value = false
  closeNotifPanel()
  closeHistoryPanel()
  router.push({ name: 'Home' }).catch(() => {})
}

const openNotification = async (item: any) => {
  await props.notif.markOneRead(item.id)
  closeNotifPanel()
  if (item.targetPath) {
    router.push(item.targetPath).catch(() => {})
  }
}

const openHistoryItem = (item: any) => {
  closeHistoryPanel()
  const fallbackTarget = `/books/search?keyword=${encodeURIComponent(item.keyword)}&page=0&size=12`
  router.push(item.targetPath || fallbackTarget).catch(() => {})
}

const handleClickOutside = (e: MouseEvent) => {
  const path = e.composedPath()

  if (props.notif.showNotifPanel.value && notifPanelRef.value && notifBtnRef.value) {
    if (!path.includes(notifPanelRef.value) && !path.includes(notifBtnRef.value)) {
      closeNotifPanel()
    }
  }

  if (props.history.showHistoryPanel.value && historyPanelRef.value && historyBtnRef.value) {
    if (!path.includes(historyPanelRef.value) && !path.includes(historyBtnRef.value)) {
      closeHistoryPanel()
    }
  }

  if (showUserMenu.value && userMenuRef.value && userBtnRef.value) {
    if (!path.includes(userMenuRef.value) && !path.includes(userBtnRef.value)) {
      showUserMenu.value = false
    }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  updateScrollState()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  window.requestAnimationFrame(animateNavEntrance)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('scroll', handleWindowScroll)
})
</script>
