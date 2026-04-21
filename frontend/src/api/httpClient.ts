import axios, { AxiosHeaders, type AxiosError } from 'axios'
import { API_CONFIG } from '../config'
import {
  clearCaptchaPassToken,
  injectSecurityHeaders,
  isCaptchaRequiredResponse,
} from './antiCrawler'
import { requestCaptchaChallenge } from '../state/securityChallenge'
import { errorCenter } from '../services/ErrorCenter'
import { getErrorMessage } from '../utils/errorHelpers'
import { safeGetItem } from '../utils/storageHelpers'

export const httpClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
})

type ErrorResponseData = {
  message?: unknown
}

type HttpErrorLike = Error & {
  response?: {
    status?: number
    data?: ErrorResponseData
  }
  code?: string
}

const NETWORK_ERROR_MESSAGE = '无法连接到服务器，请检查网络后重试。'
const UNEXPECTED_ERROR_MESSAGE = '出现未预期错误，请稍后再试。'

httpClient.interceptors.request.use((config) => {
  const headers = AxiosHeaders.from(config.headers ?? {})
  const token = safeGetItem('token')

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
      const resolvedMessage = resolveHttpErrorMessage(error)
      const normalizedError: HttpErrorLike = new Error(resolvedMessage)
      normalizedError.response = {
        status: error.response?.status,
        data: error.response?.data as ErrorResponseData | undefined,
      }
      normalizedError.code = error.code

      errorCenter.handle(normalizedError, {
        page: window.location.pathname,
        url: config?.url,
        retryable: !error.response || (error.response.status >= 500 && error.response.status < 600),
      })

      return Promise.reject(Object.assign(error, { userMessage: resolvedMessage }))
    }

    try {
      clearCaptchaPassToken()
      await requestCaptchaChallenge()
      config._captchaRetried = true
      return httpClient(config)
    } catch (challengeError) {
      const message = getErrorMessage(challengeError, UNEXPECTED_ERROR_MESSAGE)
      errorCenter.handle(new Error(message), {
        page: window.location.pathname,
        url: config.url,
        retryable: true,
      })
      return Promise.reject(challengeError)
    }
  },
)

function resolveHttpErrorMessage(error: AxiosError): string {
  const data = error.response?.data as ErrorResponseData | undefined
  const apiMessage = data?.message

  if (typeof apiMessage === 'string' && apiMessage.trim()) {
    return apiMessage
  }

  if (!error.response) {
    return NETWORK_ERROR_MESSAGE
  }

  return getErrorMessage(error, UNEXPECTED_ERROR_MESSAGE)
}

export default httpClient
