<template>
  <nav ref="navRef" class="top-nav" :class="{ 'top-nav--scrolled': isScrolled }" aria-label="Primary">
    <div class="nav-container">
      <div class="nav-left">
        <RouterLink to="/" class="brand-wrapper brand-link" aria-label="Home">
          <div class="photo-logo">
            <img src="/school-badge.png" alt="Logo" width="40" height="40" />
          </div>
          <div class="brand-identity">
            <span class="brand-kicker">{{ t('nav.systemName') }}</span>
            <span class="brand-logo">中国劳动关系学院</span>
          </div>
        </RouterLink>

        <div
          class="nav-links-shell"
          :class="{
            'nav-links-shell--active': showNavScrollControls,
            'nav-links-shell--left': canScrollNavLeft,
            'nav-links-shell--right': canScrollNavRight,
          }"
        >
          <button
            v-show="showNavScrollControls"
            type="button"
            class="nav-scroll-btn nav-scroll-btn--left"
            :class="{ 'is-disabled': !canScrollNavLeft }"
            :disabled="!canScrollNavLeft"
            :aria-label="navScrollLabels.left"
            @click="scrollNavLinks('left')"
          >
            <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
          </button>

          <div ref="linksRef" class="nav-links" @scroll.passive="syncNavScrollState">
            <RouterLink
              v-for="item in visibleNavItems"
              :key="item.name"
              :to="item.to"
              class="nav-link"
              :class="{ active: isActive(item) }"
              :aria-current="isActive(item) ? 'page' : undefined"
            >
              {{ item.label }}
            </RouterLink>
          </div>

          <button
            v-show="showNavScrollControls"
            type="button"
            class="nav-scroll-btn nav-scroll-btn--right"
            :class="{ 'is-disabled': !canScrollNavRight }"
            :disabled="!canScrollNavRight"
            :aria-label="navScrollLabels.right"
            @click="scrollNavLinks('right')"
          >
            <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
          </button>
        </div>
      </div>

      <div class="nav-right">
        <div ref="actionsRef" class="action-icons">
          <LibraryButton
            type="ghost"
            size="small"
            class="font-bold text-lg"
            :aria-label="t('nav.switchLang')"
            @click="toggleLang"
          >
            {{ $t('nav.lang') }}
          </LibraryButton>

          <LibraryButton
            type="ghost"
            size="small"
            class="theme-toggle-btn"
            :aria-label="theme.isDark.value ? t('nav.switchToLight') : t('nav.switchToDark')"
            :title="theme.isDark.value ? t('nav.switchToLight') : t('nav.switchToDark')"
            @click="theme.toggle"
          >
            <span class="material-symbols-outlined" aria-hidden="true">{{ theme.isDark.value ? 'light_mode' : 'dark_mode' }}</span>
          </LibraryButton>

          <div class="icon-btn-wrapper">
            <LibraryButton
              type="ghost"
              size="small"
              ref="notifBtnRef"
              :aria-label="t('nav.openNotifications')"
              @click="props.notif.toggleNotifPanel(closeHistoryPanel)"
            >
              <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
              <span v-if="props.notif.unreadCount.value > 0" class="unread-badge">{{ props.notif.unreadCount.value }}</span>
            </LibraryButton>
            <Transition name="popup">
              <div
                v-if="props.notif.showNotifPanel.value"
                ref="notifPanelRef"
                class="popup-panel notif-panel"
                role="dialog"
                aria-modal="false"
                :aria-label="t('nav.notificationPanel')"
              >
                <div class="popup-header">
                  <h4 class="popup-title">{{ $t('notifications.title') }}</h4>
                  <LibraryButton v-if="props.notif.unreadCount.value > 0" type="ghost" size="small" @click="props.notif.markAllRead()">
                    {{ $t('notifications.markAllRead') }}
                  </LibraryButton>
                </div>
                <div class="popup-body">
                  <button
                    v-for="n in props.notif.notifications.value"
                    :key="n.id"
                    :class="['notif-item', { 'notif-read': n.read }]"
                    type="button"
                    :aria-label="`${t('nav.openNotifTitle')}${props.notif.getNotifTitle(n)}`"
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
            <LibraryButton
              type="ghost"
              size="small"
              ref="historyBtnRef"
              :aria-label="t('nav.openSearchHistory')"
              @click="props.history.toggleHistoryPanel(closeNotifPanel)"
            >
              <span class="material-symbols-outlined" aria-hidden="true">history</span>
            </LibraryButton>
            <Transition name="popup">
              <div
                v-if="props.history.showHistoryPanel.value"
                ref="historyPanelRef"
                class="popup-panel history-panel"
                role="dialog"
                aria-modal="false"
                :aria-label="t('nav.searchHistoryPanel')"
              >
                <div class="popup-header">
                  <h4 class="popup-title">{{ $t('history.title') }}</h4>
                  <LibraryButton v-if="props.history.searchHistory.value.length > 0" type="ghost" size="small" @click="props.history.clearHistory()">
                    {{ $t('history.clear') }}
                  </LibraryButton>
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
                    :aria-label="`${t('nav.searchAgain')}${item.label || item.keyword}`"
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
          <LibraryButton
            v-if="!userStore.isLoggedIn"
            type="primary"
            size="small"
            :aria-label="t('nav.goToSignIn')"
            @click="goToLogin"
          >
            {{ t('nav.signIn') }}
          </LibraryButton>

          <div v-else class="user-menu">
            <button
              class="user-avatar-btn"
              ref="userBtnRef"
              type="button"
              :aria-label="t('nav.userMenu')"
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
                :aria-label="t('nav.userMenu')"
              >
                <div class="user-info">
                  <p class="user-name">{{ userStore.user?.username }}</p>
                  <p class="user-id">{{ userStore.user?.studentId }}</p>
                </div>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" type="button" @click="openAccount">
                  <span class="material-symbols-outlined" aria-hidden="true">manage_accounts</span>
                  <span>{{ t('nav.myAccount') }}</span>
                </button>
                <button class="dropdown-item" type="button" @click="openBookshelf">
                  <span class="material-symbols-outlined" aria-hidden="true">favorite</span>
                  <span>{{ t('nav.myBookshelf') }}</span>
                </button>
                <button class="dropdown-item" type="button" @click="handleLogout">
                  <span class="material-symbols-outlined" aria-hidden="true">logout</span>
                  <span>{{ t('nav.signOut') }}</span>
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
import { computed, inject, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { gsap, prefersReducedMotion } from '../../motion'
import { useUserStore } from '../../stores/user'
import { handleImageError } from '../../utils/imageHelpers'
import { useTheme } from '../../composables/useTheme'
import LibraryButton from '../common/LibraryButton.vue'

const props = defineProps({
  notif: { type: Object, required: true },
  history: { type: Object, required: true },
})

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const theme = useTheme()
const petEventBus = inject('petEventBus') as any
const navRef = ref<HTMLElement | null>(null)
const linksRef = ref<HTMLElement | null>(null)
const actionsRef = ref<HTMLElement | null>(null)
const avatarRef = ref<HTMLElement | null>(null)
const isScrolled = ref(false)
const showNavScrollControls = ref(false)
const canScrollNavLeft = ref(false)
const canScrollNavRight = ref(false)

const navItems = computed(() => [
    { name: 'Home', to: '/', label: t('nav.home') },
    { name: 'BookSearch', to: '/books/search', label: t('nav.search') },
    { name: 'MyBorrows', to: '/my-borrows', label: t('nav.borrows') },
    { name: 'MyReservations', to: '/my-reservations', label: t('nav.reservations') },
    { name: 'MyBookshelf', to: '/my-bookshelf', label: t('nav.bookshelf') },
    { name: 'MyAccount', to: '/my-account', label: t('nav.account') },
    { name: 'Dashboard', to: '/dashboard', label: t('nav.dashboard') },
    { name: 'InventoryAlerts', to: '/inventory-alerts', label: t('nav.alerts') },
    { name: 'UserManagement', to: '/user-management', label: t('nav.userManagement') },
    { name: 'PurchaseSuggestions', to: '/purchase-suggestions', label: t('nav.purchases') },
  ])

const adminOnlyPages = new Set(['Dashboard', 'InventoryAlerts', 'UserManagement'])
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

const navScrollLabels = computed(() => (
  locale.value === 'en'
    ? {
        left: 'Scroll navigation left',
        right: 'Scroll navigation right',
      }
    : {
        left: '向左滑动导航',
        right: '向右滑动导航',
      }
))

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
  localStorage.setItem('locale', locale.value)
  if (petEventBus?.value) {
    petEventBus.value.push({ event: 'lang:switch', ts: Date.now() })
  }
}

let scrollTicking = false

const scheduleNavScrollSync = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.requestAnimationFrame(() => {
    syncNavScrollState()
  })
}

const updateScrollState = () => {
  isScrolled.value = typeof window !== 'undefined' && window.scrollY > 24
}

const syncNavScrollState = () => {
  const linksEl = linksRef.value
  if (!linksEl || typeof window === 'undefined' || window.innerWidth < 768) {
    showNavScrollControls.value = false
    canScrollNavLeft.value = false
    canScrollNavRight.value = false
    return
  }

  const overflowAmount = linksEl.scrollWidth - linksEl.clientWidth
  const hasOverflow = overflowAmount > 12
  showNavScrollControls.value = hasOverflow

  if (!hasOverflow) {
    canScrollNavLeft.value = false
    canScrollNavRight.value = false
    return
  }

  canScrollNavLeft.value = linksEl.scrollLeft > 6
  canScrollNavRight.value = linksEl.scrollLeft < overflowAmount - 6
}

const scrollNavLinks = (direction: 'left' | 'right') => {
  const linksEl = linksRef.value
  if (!linksEl) {
    return
  }

  const distance = Math.max(linksEl.clientWidth * 0.72, 180)
  linksEl.scrollBy({
    left: direction === 'left' ? -distance : distance,
    behavior: 'smooth',
  })
}

const handleWindowScroll = () => {
  if (scrollTicking) return

  scrollTicking = true
  window.requestAnimationFrame(() => {
    updateScrollState()
    scrollTicking = false
  })
}

const handleWindowResize = () => {
  scheduleNavScrollSync()
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

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key !== 'Escape') {
    return
  }

  let closed = false

  if (props.notif.showNotifPanel.value) {
    closeNotifPanel()
    closed = true
  }

  if (props.history.showHistoryPanel.value) {
    closeHistoryPanel()
    closed = true
  }

  if (showUserMenu.value) {
    showUserMenu.value = false
    closed = true
  }

  if (closed) {
    e.preventDefault()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
  updateScrollState()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  window.addEventListener('resize', handleWindowResize, { passive: true })
  nextTick(() => {
    scheduleNavScrollSync()
  })
  window.requestAnimationFrame(animateNavEntrance)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('scroll', handleWindowScroll)
  window.removeEventListener('resize', handleWindowResize)
})

watch(
  [visibleNavItems, () => route.fullPath, locale],
  () => {
    nextTick(() => {
      scheduleNavScrollSync()
    })
  },
  { deep: true },
)
</script>
