import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import httpClient from '../api/httpClient'
import { sanitizeApiMessage } from '../utils/apiMessage'
import { safeGetItem, safeGetJSON, safeRemoveItem, safeSetItem, safeSetJSON } from '../utils/storageHelpers'
import { logger } from '../utils/logger'
import i18n from '../i18n'

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

const AUTH_MESSAGE_KEY_MAP: Record<string, string> = {
  '学号不能为空': 'login.validation.studentIdRequired',
  '学号不能为空。': 'login.validation.studentIdRequired',
  '密码不能为空': 'login.validation.passwordRequired',
  '密码不能为空。': 'login.validation.passwordRequired',
  '用户名不能为空': 'login.validation.usernameRequired',
  '用户名不能为空。': 'login.validation.usernameRequired',
  '确认密码不能为空': 'login.validation.confirmPasswordRequired',
  '确认密码不能为空。': 'login.validation.confirmPasswordRequired',
  '学号必须为 7 到 10 位数字': 'login.validation.studentIdFormat',
  '用户名长度必须在 2 到 20 个字符之间': 'login.validation.usernameLength',
  '密码长度必须在 8 到 20 个字符之间': 'login.validation.passwordLength',
  '密码长度必须在 8 到 20 个字符之间。': 'login.validation.passwordLength',
  '密码长度不能少于 8 个字符。': 'login.validation.passwordLength',
  '密码长度不能超过 20 个字符。': 'login.validation.passwordLength',
  '两次输入的密码不一致。': 'login.validation.passwordMismatch',
  '邮箱格式不正确': 'login.validation.emailInvalid',
  '手机号格式不正确': 'login.validation.phoneInvalid',
  '密码必须包含数字。': 'login.validation.passwordRequiresNumber',
  '密码必须同时包含大小写字母。': 'login.validation.passwordRequiresLetterCase',
  '密码必须包含特殊字符。': 'login.validation.passwordRequiresSpecialChar',
  '学号或密码错误。': 'login.messages.invalidCredentials',
  '账号已被停用，请联系管理员。': 'login.messages.accountDisabled',
  '账号已被临时锁定，请 30 分钟后再试。': 'login.messages.accountTemporarilyLocked',
  '学号或邮箱已存在，请检查后重试。': 'login.messages.identityExists',
  '该学号已注册。': 'login.messages.studentIdExists',
  '该邮箱已被使用。': 'login.messages.emailExists',
  '注册成功': 'login.messages.registerSuccess',
}

export const useUserStore = defineStore('user', () => {
  const globalI18n = i18n as unknown as { global: { t: (message: string) => string } }
  const translate = (key: string) => globalI18n.global.t(key)
  const currentLocale = () => (i18n.global.locale as { value?: string } | string)
  const getLocaleValue = () => {
    const locale = currentLocale()
    return typeof locale === 'string' ? locale : locale.value ?? 'en'
  }
  const containsChinese = (message: string) => /[\u3400-\u9FFF]/u.test(message)
  const resolveAuthMessage = (message: unknown, fallbackKey: string) => {
    const fallback = translate(fallbackKey)
    if (typeof message !== 'string') {
      return fallback
    }

    const normalized = message.trim()
    if (!normalized) {
      return fallback
    }

    const mappedKey = AUTH_MESSAGE_KEY_MAP[normalized]
    if (mappedKey) {
      return translate(mappedKey)
    }

    const sanitized = sanitizeApiMessage(normalized, fallback)
    if (sanitized === fallback) {
      return fallback
    }

    if (getLocaleValue() === 'en' && containsChinese(normalized)) {
      return fallback
    }

    return sanitized
  }
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
      return { success: true, message: translate('login.messages.loginSuccess') }
    } catch (error: any) {
      const message = resolveAuthMessage(error.response?.data?.message, 'login.messages.loginFailed')
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
      return { success: true, message: resolveAuthMessage(response.data.message, 'login.messages.registerSuccess') }
    } catch (error: any) {
      const message = resolveAuthMessage(error.response?.data?.message, 'login.messages.registerFailed')
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
        user.value = response.data.data
        safeSetJSON('user', response.data.data)
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
