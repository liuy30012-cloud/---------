import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../stores/user'
import { useLoginEffects } from './useLoginEffects'

export function useLoginForm() {
  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const { t } = useI18n()

  const isLogin = ref(true)
  const isLoading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const focusedField = ref('')

  const formData = reactive({
    studentId: '',
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    rememberMe: false,
  })

  const effects = useLoginEffects(isLogin, formData)

  function switchMode(login: boolean) {
    if (isLogin.value === login) return

    isLogin.value = login
    errorMessage.value = ''
    successMessage.value = ''
    Object.assign(formData, {
      studentId: '',
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: '',
      rememberMe: false,
    })
  }

  async function handleSubmit(triggerCelebration: () => void) {
    errorMessage.value = ''
    successMessage.value = ''

    if (!formData.studentId.trim()) {
      errorMessage.value = t('login.validation.studentIdRequired')
      effects.triggerShake()
      return
    }

    if (!formData.password.trim()) {
      errorMessage.value = t('login.validation.passwordRequired')
      effects.triggerShake()
      return
    }

    if (!isLogin.value) {
      if (!formData.username.trim()) {
        errorMessage.value = t('login.validation.usernameRequired')
        effects.triggerShake()
        return
      }

      if (formData.password.length < 8 || formData.password.length > 20) {
        errorMessage.value = t('login.validation.passwordLength')
        effects.triggerShake()
        return
      }

      if (formData.password !== formData.confirmPassword) {
        errorMessage.value = t('login.validation.passwordMismatch')
        effects.triggerShake()
        return
      }

      if (
        !/[a-z]/.test(formData.password)
        || !/[A-Z]/.test(formData.password)
        || !/\d/.test(formData.password)
        || !/[^a-zA-Z0-9]/.test(formData.password)
      ) {
        errorMessage.value = t('login.validation.passwordComplexity')
        effects.triggerShake()
        return
      }
    }

    isLoading.value = true

    try {
      if (isLogin.value) {
        const result = await userStore.login(
          formData.studentId,
          formData.password,
          formData.rememberMe,
        )

        if (result.success) {
          const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
          triggerCelebration()
          setTimeout(() => {
            router.push(redirectTarget).catch(() => router.push('/'))
          }, 800)
        } else {
          errorMessage.value = result.message
          effects.triggerShake()
        }

        return
      }

      const result = await userStore.register({
        studentId: formData.studentId,
        username: formData.username,
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        email: formData.email,
        phone: formData.phone,
      })

      if (result.success) {
        successMessage.value = t('login.messages.registerSuccess')
        triggerCelebration()
        setTimeout(() => {
          isLogin.value = true
          formData.password = ''
          formData.confirmPassword = ''
          successMessage.value = ''
        }, 2000)
      } else {
        errorMessage.value = result.message
        effects.triggerShake()
      }
    } catch (error: any) {
      errorMessage.value = error.message || t('login.messages.genericError')
      effects.triggerShake()
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLogin,
    isLoading,
    errorMessage,
    successMessage,
    focusedField,
    formData,
    switchMode,
    handleSubmit,
    ...effects,
  }
}
