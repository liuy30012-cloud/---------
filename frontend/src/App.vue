<template>
  <div class="scholarly-app" :class="layoutClass">
    <ErrorBoundary @retry="handleBoundaryRetry">
      <template v-if="currentLayout === 'home'">
        <TopNav :notif="notif" :history="history" />

        <main class="main-content">
          <Transition name="route-fade" appear mode="out-in">
            <div class="page-stack" key="home-shell">
              <HeroSection @search="goToSearch" />

              <div class="page-shell page-shell--home">
                <RelatedVolumes />
              </div>
            </div>
          </Transition>
        </main>

        <SiteFooter />
      </template>

      <template v-else-if="currentLayout === 'page'">
        <TopNav :notif="notif" :history="history" />
        <PageScaffold :shell="currentShell">
          <router-view v-slot="{ Component }">
            <Transition name="route-fade" appear mode="out-in">
              <component :is="Component" :key="pageTransitionKey" />
            </Transition>
          </router-view>
        </PageScaffold>
      </template>

      <router-view v-else />
    </ErrorBoundary>

    <DesktopPet />
    <CaptchaChallengeModal />
    <ToastContainer />
    <OfflineIndicator />
    <QuickActionPanel v-if="userStore.isLoggedIn" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useNotifications } from './composables/useNotifications'
import { useSearchHistory } from './composables/useSearchHistory'
import { useUserStore } from './stores/user'
import HeroSection from './components/home/HeroSection.vue'
import RelatedVolumes from './components/home/RelatedVolumes.vue'
import SiteFooter from './components/home/SiteFooter.vue'
import PageScaffold from './components/layout/PageScaffold.vue'
import TopNav from './components/navigation/TopNav.vue'
import DesktopPet from './pet/DesktopPet.vue'
import CaptchaChallengeModal from './components/common/CaptchaChallengeModal.vue'
import ErrorBoundary from './components/common/ErrorBoundary.vue'
import OfflineIndicator from './components/common/OfflineIndicator.vue'
import ToastContainer from './components/common/ToastContainer.vue'
import QuickActionPanel from './components/panels/QuickActionPanel.vue'

const route = useRoute()
const router = useRouter()
const { locale } = useI18n()
const userStore = useUserStore()

const currentLayout = computed(() => (route.meta.layout as 'immersive' | 'home' | 'page' | undefined) || 'page')
const currentShell = computed(() => (route.meta.shell as 'default' | 'wide' | undefined) || 'default')
const layoutClass = computed(() => `layout-${currentLayout.value}`)
const pageTransitionKey = computed(() => {
  if (route.name === 'BookDetail') {
    return `${String(route.name)}-${String(route.params.id || '')}`
  }

  return String(route.name || route.path)
})

const petEventBus = ref<any[]>([])
provide('petEventBus', petEventBus)
provide('currentLocale', locale)

const notif = useNotifications(locale)
const history = useSearchHistory(locale)
provide('searchHistoryController', history)

const goToSearch = (keyword: string) => {
  router.push({
    name: 'BookSearch',
    query: keyword ? { keyword, page: '0', size: '12' } : { page: '0', size: '12' },
  }).catch(() => {})
}

const handleBoundaryRetry = () => {
  router.replace({
    path: route.fullPath,
    query: route.query,
    hash: route.hash,
  }).catch(() => {})
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    notif.loadNotifications()
    history.loadHistory()
  }
})

watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    notif.loadNotifications()
    history.loadHistory()
  } else {
    notif.notifications.value = []
    notif.showNotifPanel.value = false
    history.searchHistory.value = []
    history.showHistoryPanel.value = false
  }
})
</script>

<style src="./styles/app.css"></style>
