import { describe, expect, it, vi } from 'vitest'

import {
  clearCaptchaPassToken,
  getCaptchaPassToken,
  injectSecurityHeaders,
  setCaptchaPassToken,
} from '../../api/antiCrawler'

describe('antiCrawler helpers', () => {
  it('keeps captcha pass tokens in memory and clears the legacy storage slot', () => {
    window.localStorage.setItem('captchaPassToken', JSON.stringify({ token: 'legacy' }))

    setCaptchaPassToken('memory-token', 120, 5)

    expect(getCaptchaPassToken()).toBe('memory-token')
    expect(window.localStorage.getItem('captchaPassToken')).toBeNull()
  })

  it('injects only the device fingerprint and captcha pass token security headers', () => {
    setCaptchaPassToken('memory-token', 120, 5)

    const headers = injectSecurityHeaders()

    expect(headers.get('X-Device-FP')).toEqual(expect.any(String))
    expect(headers.get('X-Captcha-Pass')).toBe('memory-token')
    expect(headers.get('X-Request-Sign')).toBeUndefined()
    expect(headers.get('X-Request-Timestamp')).toBeUndefined()
    expect(headers.get('X-Request-Nonce')).toBeUndefined()
  })

  it('does not keep captcha pass tokens after a module reload', async () => {
    clearCaptchaPassToken()
    setCaptchaPassToken('transient-token', 120, 5)
    expect(getCaptchaPassToken()).toBe('transient-token')

    vi.resetModules()
    const reloadedModule = await import('../../api/antiCrawler')

    expect(reloadedModule.getCaptchaPassToken()).toBeNull()
  })
})
