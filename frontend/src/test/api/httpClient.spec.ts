import { describe, expect, it } from 'vitest'

import { setCaptchaPassToken } from '../../api/antiCrawler'
import { httpClient } from '../../api/httpClient'

type AxiosRequestInterceptorHandler = (config: Record<string, unknown>) => Promise<Record<string, unknown>> | Record<string, unknown>

describe('httpClient request interceptor', () => {
  it('adds auth plus anti-crawler headers without deprecated signature headers', async () => {
    window.localStorage.setItem('token', 'jwt-token')
    setCaptchaPassToken('pass-token', 120, 5)

    const requestHandler = ((httpClient.interceptors.request as any).handlers.find((handler: any) => Boolean(handler?.fulfilled))?.fulfilled) as AxiosRequestInterceptorHandler
    const nextConfig = await requestHandler({ headers: { Accept: 'application/json' } })
    const headers = nextConfig.headers as { get(name: string): string | undefined }

    expect(headers.get('Authorization')).toBe('Bearer jwt-token')
    expect(headers.get('X-Device-FP')).toEqual(expect.any(String))
    expect(headers.get('X-Captcha-Pass')).toBe('pass-token')
    expect(headers.get('X-Request-Sign')).toBeUndefined()
    expect(headers.get('X-Request-Timestamp')).toBeUndefined()
    expect(headers.get('X-Request-Nonce')).toBeUndefined()
  })
})
