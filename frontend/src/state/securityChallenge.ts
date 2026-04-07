import { reactive, readonly } from 'vue'

const state = reactive({
  visible: false,
  loading: false,
  errorMessage: '',
  requestSerial: 0,
})

let activePromise: Promise<string> | null = null
let activeResolve: ((token: string) => void) | null = null
let activeReject: ((error: Error) => void) | null = null

export function useSecurityChallengeState() {
  return readonly(state)
}

export function requestCaptchaChallenge(): Promise<string> {
  if (activePromise) {
    return activePromise
  }

  state.visible = true
  state.loading = false
  state.errorMessage = ''
  state.requestSerial += 1

  activePromise = new Promise<string>((resolve, reject) => {
    activeResolve = resolve
    activeReject = reject
  })

  return activePromise
}

export function setCaptchaChallengeLoading(loading: boolean) {
  state.loading = loading
  if (loading) {
    state.errorMessage = ''
  }
}

export function setCaptchaChallengeError(message: string) {
  state.loading = false
  state.errorMessage = message
}

export function resolveCaptchaChallenge(token: string) {
  const resolve = activeResolve
  cleanup()
  resolve?.(token)
}

export function cancelCaptchaChallenge(reason: string = '验证码挑战已取消') {
  const reject = activeReject
  cleanup()
  reject?.(new Error(reason))
}

function cleanup() {
  state.visible = false
  state.loading = false
  state.errorMessage = ''
  activePromise = null
  activeResolve = null
  activeReject = null
}
