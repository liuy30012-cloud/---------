import { ref, reactive, computed, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'

interface FormData {
  studentId: string
  password: string
  confirmPassword: string
}

export function useLoginEffects(isLogin: Ref<boolean>, formData: FormData) {
  const { t } = useI18n()

  const sealTexts = [
    'login.sealTexts.bamboo',
    'login.sealTexts.archive',
    'login.sealTexts.quiet',
    'login.sealTexts.borrow',
    'login.sealTexts.academy',
  ] as const
  const sealTextIndex = ref(0)
  const sealClicked = ref(false)
  const sealText = computed(() => t(sealTexts[sealTextIndex.value]))

  const socialButtons = reactive([
    { icon: 'chat', titleKey: 'login.quickAccess.wechat', hovered: false },
    { icon: 'mail', titleKey: 'login.quickAccess.email', hovered: false },
    { icon: 'phone_android', titleKey: 'login.quickAccess.mobile', hovered: false },
  ])

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

  const showPassword = ref(false)

  function togglePasswordVisibility() {
    showPassword.value = !showPassword.value
  }

  const shakeError = ref(false)

  function triggerShake() {
    shakeError.value = true
    setTimeout(() => {
      shakeError.value = false
    }, 500)
  }

  const typingField = ref('')
  let typingTimer: ReturnType<typeof setTimeout> | null = null

  function onInputTyping(_event: Event, field: string) {
    typingField.value = field
    if (typingTimer) clearTimeout(typingTimer)
    typingTimer = setTimeout(() => {
      typingField.value = ''
    }, 300)
  }

  const magneticX = ref(0)
  const magneticY = ref(0)
  const submitHovered = ref(false)

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

  function cleanup() {
    if (typingTimer) clearTimeout(typingTimer)
  }

  return {
    sealClicked,
    sealText,
    socialButtons,
    greetingText,
    greetingEmoji,
    typedChars,
    passwordStrength,
    strengthLabel,
    passwordsMatch,
    formProgress,
    progressOffset,
    onSealClick,
    showPassword,
    togglePasswordVisibility,
    shakeError,
    triggerShake,
    typingField,
    onInputTyping,
    submitHovered,
    magneticBtnStyle,
    onSubmitHover,
    onSubmitLeave,
    onSubmitMouseMove,
    cleanup,
  }
}
