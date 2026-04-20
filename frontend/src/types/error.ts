// frontend/src/types/error.ts

export type ErrorType =
  | 'NetworkError'
  | 'APIError'
  | 'ValidationError'
  | 'AuthError'
  | 'BusinessError'
  | 'ComponentError'
  | 'UnknownError'

export interface ErrorContext {
  page?: string
  userId?: string
  timestamp?: number
  info?: string
  component?: string
  retryable?: boolean
  url?: string
  [key: string]: unknown
}

export interface ErrorLog {
  id: string
  type: ErrorType
  title: string
  message: string
  details?: string
  stack?: string
  context: ErrorContext
  timestamp: number
  retryable: boolean
}

export interface RetryOptions {
  maxRetries?: number
  backoff?: 'linear' | 'exponential'
  initialDelay?: number
}

export type ErrorHandler = (error: Error, context?: ErrorContext) => void

export const ERROR_MESSAGES: Record<string | number, string> = {
  NETWORK_ERROR: '网络连接失败，请检查网络设置',
  TIMEOUT: '请求超时，请稍后重试',
  401: '登录已过期，请重新登录',
  403: '没有权限访问此资源',
  404: '请求的资源不存在',
  500: '服务器错误，请稍后重试',
  BOOK_NOT_AVAILABLE: '该书籍暂时不可借阅',
  RESERVATION_FULL: '预约人数已满',
  DEFAULT: '操作失败，请稍后重试',
}
