import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../stores/user'

export function useLoginForm() {
  const router = useRouter()
  const userStore = useUserStore()
  const { t } = useI18n()

  const isLogin = ref(true)
  const isLoading = ref(false)
  const showPassword = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const focusedField = ref('')
  const shakeError = ref(false)
  const typingField = ref('')
  const submitHovered = ref(false)
  const sealClicked = ref(false)

  const sealTexts = [
    'login.sealTexts.bamboo',
    'login.sealTexts.archive',
    'login.sealTexts.quiet',
    'login.sealTexts.borrow',
    'login.sealTexts.academy',
  ] as const
  const sealTextIndex = ref(0)
  const sealText = computed(() => t(sealTexts[sealTextIndex.value]))

  const socialButtons = reactive([
    { icon: 'chat', titleKey: 'login.quickAccess.wechat', hovered: false },
    { icon: 'mail', titleKey: 'login.quickAccess.email', hovered: false },
    { icon: 'phone_android', titleKey: 'login.quickAccess.mobile', hovered: false },
  ])

  const formData = reactive({
    studentId: '',
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    rememberMe: false,
  })

  function getGreetingPeriodKey() {
    const hour = new Date().getHours()
    if (hour < 6) return 'lateNight'
    if (hour < 12) return 'morning'
    if (hour < 18) return 'afternoon'
    return 'evening'
  }

  const greetingText = computed(() => {
    const period = getGreetingPeriodKey()
    return t(`login.greetings.${period}`)
  })

  const greetingEmoji = computed(() => {
    const period = getGreetingPeriodKey()
    const emojiMap = {
      lateNight: '🌙',
      morning: '🌤️',
      afternoon: '🎋',
      evening: '🏮',
    } as const
    return emojiMap[period]
  })

  const typedChars = computed(() => t('login.brandTypingText').split(''))

  const passwordStrength = computed(() => {
    const password = formData.password
    if (!password) return 0

    let score = 0
    if (password.length >= 8) score++
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++
    if (/\d/.test(password)) score++
    if (/[^a-zA-Z0-9]/.test(password)) score++
    return score
  })

  const strengthLabel = computed(() => {
    const strengthKeys = [
      '',
      'login.passwordStrength.weak',
      'login.passwordStrength.fair',
      'login.passwordStrength.good',
      'login.passwordStrength.strong',
    ] as const
    const key = strengthKeys[passwordStrength.value]
    return key ? t(key) : ''
  })

  const passwordsMatch = computed(() => (
    formData.password === formData.confirmPassword && formData.confirmPassword.length > 0
  ))

  let typingTimer: ReturnType<typeof setTimeout> | null = null

  function onInputTyping(_event: Event, field: string) {
    typingField.value = field
    if (typingTimer) clearTimeout(typingTimer)
    typingTimer = setTimeout(() => {
      typingField.value = ''
    }, 300)
  }

  function togglePasswordVisibility() {
    showPassword.value = !showPassword.value
  }

  const magneticX = ref(0)
  const magneticY = ref(0)
  const magneticBtnStyle = computed(() => {
    if (!submitHovered.value) return {}
    return { transform: `translate(${magneticX.value}px, ${magneticY.value}px)` }
  })

  function onSubmitHover() {
    submitHovered.value = true
  }

  function onSubmitLeave() {
    submitHovered.value = false
    magneticX.value = 0
    magneticY.value = 0
  }

  function onSubmitMouseMove(event: MouseEvent, buttonElement: HTMLElement | null) {
    if (!buttonElement) return
    const rect = buttonElement.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    magneticX.value = (event.clientX - centerX) * 0.1
    magneticY.value = (event.clientY - centerY) * 0.14
  }

  const formProgress = computed(() => {
    if (!isLogin.value) return 0
    let progress = 0
    if (formData.studentId.length > 0) progress += 20
    if (formData.studentId.length >= 4) progress += 30
    if (formData.password.length > 0) progress += 20
    if (formData.password.length >= 8) progress += 30
    return Math.min(progress, 100)
  })

  const progressOffset = computed(() => {
    const circumference = 2 * Math.PI * 18
    return circumference - (formProgress.value / 100) * circumference
  })

  function onSealClick() {
    sealClicked.value = true
    sealTextIndex.value = (sealTextIndex.value + 1) % sealTexts.length
    setTimeout(() => {
      sealClicked.value = false
    }, 380)
  }

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

    const triggerShake = () => {
      shakeError.value = true
      setTimeout(() => {
        shakeError.value = false
      }, 500)
    }

    if (!formData.studentId.trim()) {
      errorMessage.value = t('login.validation.studentIdRequired')
      triggerShake()
      return
    }

    if (!formData.password.trim()) {
      errorMessage.value = t('login.validation.passwordRequired')
      triggerShake()
      return
    }

    if (!isLogin.value) {
      if (!formData.username.trim()) {
        errorMessage.value = t('login.validation.usernameRequired')
        triggerShake()
        return
      }

      if (formData.password.length < 8 || formData.password.length > 20) {
        errorMessage.value = t('login.validation.passwordLength')
        triggerShake()
        return
      }

      if (formData.password !== formData.confirmPassword) {
        errorMessage.value = t('login.validation.passwordMismatch')
        triggerShake()
        return
      }

      if (
        !/[a-z]/.test(formData.password)
        || !/[A-Z]/.test(formData.password)
        || !/\d/.test(formData.password)
        || !/[^a-zA-Z0-9]/.test(formData.password)
      ) {
        errorMessage.value = t('login.validation.passwordComplexity')
        triggerShake()
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
          triggerCelebration()
          setTimeout(() => router.push('/'), 800)
        } else {
          errorMessage.value = result.message
          triggerShake()
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
        triggerShake()
      }
    } catch (error: any) {
      errorMessage.value = error.message || t('login.messages.genericError')
      triggerShake()
    } finally {
      isLoading.value = false
    }
  }

  function cleanup() {
    if (typingTimer) clearTimeout(typingTimer)
  }

  return {
    isLogin,
    isLoading,
    showPassword,
    errorMessage,
    successMessage,
    focusedField,
    shakeError,
    typingField,
    submitHovered,
    sealClicked,
    sealText,
    socialButtons,
    formData,
    greetingText,
    greetingEmoji,
    typedChars,
    passwordStrength,
    strengthLabel,
    passwordsMatch,
    formProgress,
    progressOffset,
    magneticBtnStyle,
    onInputTyping,
    togglePasswordVisibility,
    onSubmitHover,
    onSubmitLeave,
    onSubmitMouseMove,
    onSealClick,
    switchMode,
    handleSubmit,
    cleanup,
  }
}
