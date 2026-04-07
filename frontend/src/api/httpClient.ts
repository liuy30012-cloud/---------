import axios, { AxiosHeaders, type AxiosError } from 'axios'
import { API_CONFIG } from '../config'
import {
  clearCaptchaPassToken,
  injectSecurityHeaders,
  isCaptchaRequiredResponse,
} from './antiCrawler'
import { requestCaptchaChallenge } from '../state/securityChallenge'

export const httpClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
})

httpClient.interceptors.request.use((config) => {
  const headers = AxiosHeaders.from(config.headers ?? {})
  const token = typeof window !== 'undefined' ? window.localStorage.getItem('token') : null

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  config.headers = injectSecurityHeaders(headers)
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config
    if (!config || config.skipCaptchaChallenge || config._captchaRetried || !isCaptchaRequiredResponse(error.response)) {
      return Promise.reject(error)
    }

    try {
      clearCaptchaPassToken()
      await requestCaptchaChallenge()
      config._captchaRetried = true
      return httpClient(config)
    } catch (challengeError) {
      return Promise.reject(challengeError)
    }
  },
)

export default httpClient
