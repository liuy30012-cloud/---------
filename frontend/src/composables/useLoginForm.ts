import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

export function useLoginForm() {
  const router = useRouter()
  const userStore = useUserStore()

  const isLogin = ref(true)
  const isLoading = ref(false)
  const showPassword = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const focusedField = ref('') // 保留用于模板绑定
  const shakeError = ref(false)
  const typingField = ref('')
  const submitHovered = ref(false)
  const sealClicked = ref(false)

  const sealTexts = ['竹里', '藏阅', '静观', '借阅', '书院']
  const sealTextIndex = ref(0)
  const sealText = computed(() => sealTexts[sealTextIndex.value])

  const socialButtons = reactive([
    { icon: 'chat', title: '微信入口', hovered: false },
    { icon: 'mail', title: '邮箱入口', hovered: false },
    { icon: 'phone_android', title: '移动入口', hovered: false },
  ])

  const formData = reactive({
    studentId: '', username: '', password: '', confirmPassword: '',
    email: '', phone: '', rememberMe: false,
  })

  const greetingText = computed(() => {
    const hour = new Date().getHours()
    if (hour < 6) return '夜深竹影静，仍可从容入馆'
    if (hour < 12) return '晨光入院，适合检索新书'
    if (hour < 18) return '午后竹影缓，正宜静心找书'
    return '晚灯已启，书院仍为你留座'
  })

  const greetingEmoji = computed(() => {
    const hour = new Date().getHours()
    if (hour < 6) return '月'
    if (hour < 12) return '晨'
    if (hour < 18) return '竹'
    return '灯'
  })

  const fullText = '竹里禅院 BAMBOO CLOISTER'
  const typedChars = computed(() => fullText.split(''))

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

  const strengthLabel = computed(() => ['', '偏弱', '合格', '稳妥', '很强'][passwordStrength.value] || '')
  const passwordsMatch = computed(() => formData.password === formData.confirmPassword && formData.confirmPassword.length > 0)

  let typingTimer: ReturnType<typeof setTimeout> | null = null
  function onInputTyping(_e: Event, field: string) {
    typingField.value = field
    if (typingTimer) clearTimeout(typingTimer)
    typingTimer = setTimeout(() => { typingField.value = '' }, 300)
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

  function onSubmitHover() { submitHovered.value = true }
  function onSubmitLeave() {
    submitHovered.value = false
    magneticX.value = 0
    magneticY.value = 0
  }

  function onSubmitMouseMove(e: MouseEvent, btnEl: HTMLElement | null) {
    if (!btnEl) return
    const rect = btnEl.getBoundingClientRect()
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2
    magneticX.value = (e.clientX - cx) * 0.1
    magneticY.value = (e.clientY - cy) * 0.14
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
    setTimeout(() => { sealClicked.value = false }, 380)
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
      setTimeout(() => { shakeError.value = false }, 500)
    }

    if (!formData.studentId.trim()) {
      errorMessage.value = '请输入学工号。'
      triggerShake()
      return
    }

    if (!formData.password.trim()) {
      errorMessage.value = '请输入密码。'
      triggerShake()
      return
    }

    if (!isLogin.value) {
      if (!formData.username.trim()) {
        errorMessage.value = '请输入用户名。'
        triggerShake()
        return
      }
      if (formData.password.length < 8 || formData.password.length > 20) {
        errorMessage.value = '密码长度需保持在 8 到 20 位之间。'
        triggerShake()
        return
      }
      if (formData.password !== formData.confirmPassword) {
        errorMessage.value = '两次输入的密码不一致。'
        triggerShake()
        return
      }
      if (!/[a-z]/.test(formData.password) || !/[A-Z]/.test(formData.password) || !/\d/.test(formData.password) || !/[^a-zA-Z0-9]/.test(formData.password)) {
        errorMessage.value = '密码需同时包含大小写字母、数字和特殊字符。'
        triggerShake()
        return
      }
    }

    isLoading.value = true
    try {
      if (isLogin.value) {
        const result = await userStore.login(formData.studentId, formData.password, formData.rememberMe)
        if (result.success) {
          triggerCelebration()
          setTimeout(() => router.push('/'), 800)
        } else {
          errorMessage.value = result.message
          triggerShake()
        }
      } else {
        const result = await userStore.register({
          studentId: formData.studentId,
          username: formData.username,
          password: formData.password,
          confirmPassword: formData.confirmPassword,
          email: formData.email,
          phone: formData.phone,
        })
        if (result.success) {
          successMessage.value = '注册完成，请使用新账户登录。'
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
      }
    } catch (error: any) {
      errorMessage.value = error.message || '操作未完成，请稍后再试。'
      triggerShake()
    } finally {
      isLoading.value = false
    }
  }

  function cleanup() {
    if (typingTimer) clearTimeout(typingTimer)
  }

  return {
    isLogin, isLoading, showPassword, errorMessage, successMessage,
    focusedField, shakeError, typingField, submitHovered, sealClicked,
    sealText, socialButtons, formData,
    greetingText, greetingEmoji, typedChars,
    passwordStrength, strengthLabel, passwordsMatch,
    formProgress, progressOffset, magneticBtnStyle,
    onInputTyping, togglePasswordVisibility,
    onSubmitHover, onSubmitLeave, onSubmitMouseMove,
    onSealClick, switchMode, handleSubmit, cleanup,
  }
}
