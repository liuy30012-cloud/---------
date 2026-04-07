import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import httpClient from '../api/httpClient'
import { sanitizeApiMessage } from '../utils/apiMessage'
import { safeGetItem, safeGetJSON, safeRemoveItem, safeSetItem, safeSetJSON } from '../utils/storageHelpers'
import { logger } from '../utils/logger'

enum UserRole {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  ADMIN = 'ADMIN'
}

interface UserInfo {
  id: number
  studentId: string
  username: string
  email?: string
  phone?: string
  role: UserRole
  avatarUrl?: string
}

interface AuthResponse {
  token: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(safeGetItem('token'))
  const refreshToken = ref<string | null>(safeGetItem('refreshToken'))
  const user = ref<UserInfo | null>(safeGetJSON<UserInfo>('user'))
  const isLoading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const isStudent = computed(() => user.value?.role === UserRole.STUDENT)
  const isTeacher = computed(() => user.value?.role === UserRole.TEACHER)
  const isAdmin = computed(() => user.value?.role === UserRole.ADMIN)

  function setAuthState(newToken: string | null, newRefresh: string | null, newUser: UserInfo | null) {
    token.value = newToken
    refreshToken.value = newRefresh
    user.value = newUser

    if (newToken && newRefresh && newUser) {
      safeSetItem('token', newToken)
      safeSetItem('refreshToken', newRefresh)
      safeSetJSON('user', newUser)
      return
    }

    safeRemoveItem('token')
    safeRemoveItem('refreshToken')
    safeRemoveItem('user')
  }

  async function login(studentId: string, password: string, rememberMe: boolean = false) {
    isLoading.value = true
    try {
      const response = await httpClient.post<AuthResponse>('/api/auth/login', {
        studentId,
        password,
        rememberMe,
      })

      const data = response.data
      setAuthState(data.token, data.refreshToken, data.user)
      return { success: true, message: '登录成功' }
    } catch (error: any) {
      const message = sanitizeApiMessage(error.response?.data?.message, '登录失败，请重试')
      return { success: false, message }
    } finally {
      isLoading.value = false
    }
  }

  async function register(data: {
    studentId: string
    username: string
    password: string
    confirmPassword: string
    email?: string
    phone?: string
  }) {
    isLoading.value = true
    try {
      const response = await httpClient.post('/api/auth/register', data)
      return { success: true, message: sanitizeApiMessage(response.data.message, '注册成功') }
    } catch (error: any) {
      const message = sanitizeApiMessage(error.response?.data?.message, '注册失败，请重试')
      return { success: false, message }
    } finally {
      isLoading.value = false
    }
  }

  async function logout() {
    try {
      await httpClient.post('/api/auth/logout')
    } catch (error) {
      logger.error('Logout error:', error)
    } finally {
      setAuthState(null, null, null)
    }
  }

  async function fetchUserInfo() {
    if (!token.value) {
      return
    }

    try {
      const response = await httpClient.get('/api/auth/me')
      if (response.data.success) {
        user.value = response.data.user
        safeSetJSON('user', response.data.user)
      }
    } catch (error) {
      logger.error('Failed to fetch user info:', error)
      await tryRefreshToken()
    }
  }

  let refreshLock: Promise<boolean> | null = null

  async function tryRefreshToken() {
    if (!refreshToken.value) {
      await logout()
      return false
    }

    if (refreshLock) {
      return refreshLock
    }

    refreshLock = (async () => {
      try {
        const response = await httpClient.post<AuthResponse>('/api/auth/refresh', {
          refreshToken: refreshToken.value,
        })

        const data = response.data
        setAuthState(data.token, data.refreshToken, data.user)
        return true
      } catch (error) {
        logger.error('Failed to refresh token:', error)
        await logout()
        return false
      } finally {
        refreshLock = null
      }
    })()

    return refreshLock
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    isLoading.value = true
    try {
      const response = await httpClient.post('/api/auth/change-password', {
        oldPassword,
        newPassword,
      })

      return { success: true, message: sanitizeApiMessage(response.data.message, '密码修改成功') }
    } catch (error: any) {
      const message = sanitizeApiMessage(error.response?.data?.message, '密码修改失败，请重试')
      return { success: false, message }
    } finally {
      isLoading.value = false
    }
  }

  function initialize() {
    try {
      const savedToken = safeGetItem('token')
      const savedRefreshToken = safeGetItem('refreshToken')
      const savedUser = safeGetJSON<UserInfo>('user')

      if (savedToken && savedUser) {
        token.value = savedToken
        refreshToken.value = savedRefreshToken
        user.value = savedUser
        fetchUserInfo()
      }
    } catch (error) {
      logger.error('Failed to initialize user store:', error)
      setAuthState(null, null, null)
    }

    window.addEventListener('storage', (event) => {
      if (event.key !== 'token') {
        return
      }

      if (event.newValue && event.newValue !== token.value) {
        token.value = event.newValue
        user.value = safeGetJSON<UserInfo>('user')
        refreshToken.value = safeGetItem('refreshToken')
        return
      }

      if (!event.newValue && token.value) {
        setAuthState(null, null, null)
      }
    })
  }

  return {
    token,
    refreshToken,
    user,
    isLoading,
    isLoggedIn,
    isStudent,
    isTeacher,
    isAdmin,
    login,
    register,
    logout,
    fetchUserInfo,
    tryRefreshToken,
    changePassword,
    initialize,
  }
})
