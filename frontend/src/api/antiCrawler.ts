import { AxiosHeaders, type AxiosResponse } from 'axios'

const DEVICE_FP_KEY = 'deviceFingerprint'
const LEGACY_CAPTCHA_PASS_KEY = 'captchaPassToken'

interface CaptchaPassRecord {
  token: string
  expiresAt: number
  remainingUses: number
}

let captchaPassRecord: CaptchaPassRecord | null = null

function hasWindow(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function createStableDeviceFingerprint(): string {
  const existing = hasWindow() ? window.localStorage.getItem(DEVICE_FP_KEY) : null
  if (existing) {
    return existing
  }

  const randomPart = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`

  const fingerprint = [
    'lib',
    navigator.userAgent.slice(0, 32),
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    `${screen.width}x${screen.height}`,
    randomPart,
  ].join('|')

  if (hasWindow()) {
    window.localStorage.setItem(DEVICE_FP_KEY, fingerprint)
  }
  return fingerprint
}

export function getDeviceFingerprint(): string | null {
  if (!hasWindow()) {
    return null
  }

  try {
    return createStableDeviceFingerprint()
  } catch {
    return null
  }
}

export function setCaptchaPassToken(token: string, expiresInSeconds: number, remainingUses: number = 10) {
  captchaPassRecord = {
    token,
    expiresAt: Date.now() + Math.max(expiresInSeconds, 1) * 1000,
    remainingUses: Math.max(remainingUses, 1),
  }

  if (hasWindow()) {
    window.localStorage.removeItem(LEGACY_CAPTCHA_PASS_KEY)
  }
}

export function getCaptchaPassToken(): string | null {
  if (!captchaPassRecord) {
    return null
  }

  if (!captchaPassRecord.token || captchaPassRecord.expiresAt <= Date.now() || captchaPassRecord.remainingUses <= 0) {
    clearCaptchaPassToken()
    return null
  }

  return captchaPassRecord.token
}

export function clearCaptchaPassToken() {
  captchaPassRecord = null
  if (hasWindow()) {
    window.localStorage.removeItem(LEGACY_CAPTCHA_PASS_KEY)
  }
}

export function injectSecurityHeaders(
  headers?: unknown,
): AxiosHeaders {
  const nextHeaders = AxiosHeaders.from((headers ?? {}) as any)
  const deviceFingerprint = getDeviceFingerprint()
  const captchaPassToken = getCaptchaPassToken()

  if (deviceFingerprint) {
    nextHeaders.set('X-Device-FP', deviceFingerprint)
  }

  if (captchaPassToken) {
    nextHeaders.set('X-Captcha-Pass', captchaPassToken)
  } else {
    nextHeaders.delete('X-Captcha-Pass')
  }

  return nextHeaders
}

export function isCaptchaRequiredResponse(
  response?: Pick<AxiosResponse, 'status' | 'headers' | 'data'>,
): boolean {
  if (!response || response.status !== 429) {
    return false
  }

  const headerValue = response.headers?.['x-captcha-required']
  if (headerValue === 'true' || headerValue === true) {
    return true
  }

  const body = response.data as { captchaRequired?: boolean } | undefined
  return Boolean(body?.captchaRequired)
}
