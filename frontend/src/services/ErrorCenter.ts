import type { ErrorType, ErrorLog, ErrorContext, RetryOptions, ErrorHandler } from '@/types/error'
import { ERROR_MESSAGES } from '@/types/error'
import { feedbackManager } from './FeedbackManager'

class ErrorCenter {
  private static instance: ErrorCenter
  private logs: ErrorLog[] = []
  private maxLogs = 100
  private errorHandlers: Map<ErrorType, ErrorHandler> = new Map()

  private constructor() {
    this.initializeHandlers()
    this.loadLogs()
  }

  static getInstance(): ErrorCenter {
    if (!ErrorCenter.instance) {
      ErrorCenter.instance = new ErrorCenter()
    }
    return ErrorCenter.instance
  }

  private initializeHandlers(): void {
    this.errorHandlers.set('NetworkError', this.handleNetworkError.bind(this))
    this.errorHandlers.set('APIError', this.handleAPIError.bind(this))
    this.errorHandlers.set('AuthError', this.handleAuthError.bind(this))
    this.errorHandlers.set('ValidationError', this.handleValidationError.bind(this))
    this.errorHandlers.set('BusinessError', this.handleBusinessError.bind(this))
    this.errorHandlers.set('ComponentError', this.handleComponentError.bind(this))
    this.errorHandlers.set('UnknownError', this.handleUnknownError.bind(this))
  }

  handle(error: Error, context?: ErrorContext): void {
    const errorType = this.classifyError(error)
    const handler = this.errorHandlers.get(errorType)

    if (handler) {
      handler(error, context)
    }

    this.log(error, { ...context, type: errorType })
  }

  private classifyError(error: any): ErrorType {
    if (error.message?.includes('Network') || error.message?.includes('fetch')) {
      return 'NetworkError'
    }
    if (error.response) {
      const status = error.response.status
      if (status === 401 || status === 403) {
        return 'AuthError'
      }
      return 'APIError'
    }
    if (error.name === 'ValidationError') {
      return 'ValidationError'
    }
    if (error.code?.startsWith('BUSINESS_')) {
      return 'BusinessError'
    }
    if (error.name === 'ComponentError') {
      return 'ComponentError'
    }
    return 'UnknownError'
  }

  private handleNetworkError(error: Error, context?: ErrorContext): void {
    feedbackManager.error(ERROR_MESSAGES.NETWORK_ERROR)
  }

  private handleAPIError(error: any, context?: ErrorContext): void {
    const status = error.response?.status
    const message = ERROR_MESSAGES[status] || ERROR_MESSAGES.DEFAULT
    feedbackManager.error(message)
  }

  private handleAuthError(error: any, context?: ErrorContext): void {
    const status = error.response?.status
    const message = ERROR_MESSAGES[status] || ERROR_MESSAGES[401]
    feedbackManager.error(message)

    setTimeout(() => {
      window.location.href = '/login'
    }, 1500)
  }

  private handleValidationError(error: Error, context?: ErrorContext): void {
    feedbackManager.warning(error.message || '输入验证失败')
  }

  private handleBusinessError(error: any, context?: ErrorContext): void {
    const message = ERROR_MESSAGES[error.code] || error.message || ERROR_MESSAGES.DEFAULT
    feedbackManager.warning(message)
  }

  private handleComponentError(error: Error, context?: ErrorContext): void {
    console.error('Component Error:', error)
    feedbackManager.error('页面加载失败，请刷新重试')
  }

  private handleUnknownError(error: Error, context?: ErrorContext): void {
    console.error('Unknown Error:', error)
    feedbackManager.error(ERROR_MESSAGES.DEFAULT)
  }

  log(error: Error, context?: ErrorContext): void {
    const timestamp = Date.now()
    const log: ErrorLog = {
      id: `error-${timestamp}-${Math.random().toString(36).slice(2, 11)}`,
      type: this.classifyError(error),
      message: error.message,
      stack: error.stack,
      context: {
        ...context,
        timestamp,
        userAgent: navigator.userAgent,
        url: window.location.href
      },
      timestamp
    }

    this.logs.push(log)

    if (this.logs.length > this.maxLogs) {
      this.logs = this.logs.slice(-this.maxLogs)
    }

    this.saveLogs()
  }

  getLogs(): ErrorLog[] {
    return [...this.logs]
  }

  clearLogs(): void {
    this.logs = []
    localStorage.removeItem('error-logs')
  }

  async retry<T>(
    fn: () => Promise<T>,
    options: RetryOptions = {}
  ): Promise<T> {
    const maxRetries = options.maxRetries ?? 3
    const backoff = options.backoff ?? 'exponential'
    const initialDelay = options.initialDelay ?? 1000

    let lastError: Error

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        return await fn()
      } catch (error) {
        lastError = error as Error

        if (attempt < maxRetries) {
          const delay = backoff === 'exponential'
            ? initialDelay * Math.pow(2, attempt)
            : initialDelay * (attempt + 1)

          await new Promise(resolve => setTimeout(resolve, delay))
        }
      }
    }

    throw lastError!
  }

  private saveLogs(): void {
    try {
      localStorage.setItem('error-logs', JSON.stringify(this.logs))
    } catch (e) {
      console.error('Failed to save error logs:', e)
    }
  }

  private loadLogs(): void {
    try {
      const saved = localStorage.getItem('error-logs')
      if (saved) {
        this.logs = JSON.parse(saved)
      }
    } catch (e) {
      console.error('Failed to load error logs:', e)
    }
  }
}

export const errorCenter = ErrorCenter.getInstance()
