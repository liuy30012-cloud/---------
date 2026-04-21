import { createRouter, createWebHashHistory } from 'vue-router'
import { defineComponent, h } from 'vue'
import { useUserStore } from '../stores/user'

type AppLayout = 'immersive' | 'home' | 'page'
type AppShell = 'default' | 'wide'

const Login = () => import('../views/Login.vue')
const ForgotPassword = () => import('../views/ForgotPassword.vue')
const MyBorrows = () => import('../views/MyBorrows.vue')
const MyReservations = () => import('../views/MyReservations.vue')
const MyAccount = () => import('../views/MyAccount.vue')
const Dashboard = () => import('../views/Dashboard.vue')
const InventoryAlerts = () => import('../views/InventoryAlerts.vue')
const UserManagement = () => import('../views/UserManagement.vue')
const PurchaseSuggestions = () => import('../views/PurchaseSuggestions.vue')
const BookSearch = () => import('../views/BookSearch.vue')
const BookDetail = () => import('../views/BookDetail.vue')
const DamageReports = () => import('../views/DamageReports.vue')
const MyBookshelf = () => import('../views/MyBookshelf.vue')
const NotFound = () => import('../views/NotFound.vue')

const HomeStub = defineComponent({ render: () => h('div') })

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false, layout: 'immersive' as AppLayout }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: ForgotPassword,
    meta: { requiresAuth: false, layout: 'immersive' as AppLayout }
  },
  {
    path: '/',
    name: 'Home',
    component: HomeStub,
    meta: { requiresAuth: false, layout: 'home' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/my-borrows',
    name: 'MyBorrows',
    component: MyBorrows,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/my-reservations',
    name: 'MyReservations',
    component: MyReservations,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/my-bookshelf',
    name: 'MyBookshelf',
    component: MyBookshelf,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/my-account',
    name: 'MyAccount',
    component: MyAccount,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true, requiresAdmin: true, layout: 'page' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/inventory-alerts',
    name: 'InventoryAlerts',
    component: InventoryAlerts,
    meta: { requiresAuth: true, requiresAdmin: true, layout: 'page' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/user-management',
    name: 'UserManagement',
    component: UserManagement,
    meta: { requiresAuth: true, requiresAdmin: true, layout: 'page' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/purchase-suggestions',
    name: 'PurchaseSuggestions',
    component: PurchaseSuggestions,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/books/search',
    name: 'BookSearch',
    component: BookSearch,
    meta: { requiresAuth: false, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/books/:id',
    name: 'BookDetail',
    component: BookDetail,
    meta: { requiresAuth: false, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  },
  {
    path: '/damage-reports',
    name: 'DamageReports',
    component: DamageReports,
    meta: { requiresAuth: true, layout: 'page' as AppLayout, shell: 'wide' as AppShell }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { requiresAuth: false, layout: 'page' as AppLayout, shell: 'default' as AppShell }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { name: 'Home' }
  }

  if (to.name === 'Login' && userStore.isLoggedIn) {
    return { name: 'Home' }
  }

  return true
})

export default router
