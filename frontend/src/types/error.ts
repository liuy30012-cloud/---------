// frontend/src/types/error.ts

/**
 * 错误类型枚举
 */
export type ErrorType =
  | 'NetworkError'
  | 'APIError'
  | 'ValidationError'
  | 'AuthError'
  | 'BusinessError'
  | 'ComponentError'
  | 'UnknownError'

/**
 * 错误上下文信息
 */
export interface ErrorContext {
  /** 发生错误的页面 */
  page?: string
  /** 用户 ID */
  userId?: string
  /** 时间戳 */
  timestamp?: number
  /** 其他自定义上下文信息 */
  [key: string]: any
}

/**
 * 错误日志记录
 */
export interface ErrorLog {
  /** 唯一标识符 */
  id: string
  /** 错误类型 */
  type: ErrorType
  /** 错误消息 */
  message: string
  /** 错误堆栈信息 */
  stack?: string
  /** 错误上下文 */
  context: ErrorContext
  /** 发生时间戳 */
  timestamp: number
}

/**
 * 重试配置选项
 */
export interface RetryOptions {
  /** 最大重试次数，默认 3 */
  maxRetries?: number
  /** 退避策略，默认 'exponential' */
  backoff?: 'linear' | 'exponential'
  /** 初始延迟时间（毫秒），默认 1000 */
  initialDelay?: number
}

/**
 * 错误处理函数类型
 */
export type ErrorHandler = (error: Error, context?: ErrorContext) => void

/**
 * 预定义错误消息映射表
 */
export const ERROR_MESSAGES: Record<string | number, string> = {
  NETWORK_ERROR: '网络连接失败，请检查网络设置',
  TIMEOUT: '请求超时，请稍后重试',
  401: '登录已过期，请重新登录',
  403: '没有权限访问此资源',
  404: '请求的资源不存在',
  500: '服务器错误，请稍后重试',
  BOOK_NOT_AVAILABLE: '该书籍暂时不可借阅',
  RESERVATION_FULL: '预约人数已满',
  DEFAULT: '操作失败，请稍后重试'
}
