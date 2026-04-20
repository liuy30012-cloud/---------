import type { ErrorContext, ErrorHandler, ErrorLog, ErrorType, RetryOptions } from '@/types/error'
import { ERROR_MESSAGES } from '@/types/error'
import { feedbackManager } from './FeedbackManager'
import { logger } from '@/utils/logger'
import { safeGetJSON, safeRemoveItem, safeSetJSON } from '@/utils/storageHelpers'

const ERROR_LOG_STORAGE_KEY = 'error-logs'
const LOGIN_REDIRECT_DELAY = 1500

class ErrorCenter {
  private static instance: ErrorCenter
  private logs: ErrorLog[] = []
  private readonly maxLogs = 100
  private readonly errorHandlers: Map<ErrorType, ErrorHandler> = new Map()
  private readonly retryOptions: RetryOptions | null = null
  private authRedirectTimer: ReturnType<typeof setTimeout> | null = null

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

  private classifyError(error: unknown): ErrorType {
    if (this.isNetworkError(error)) {
      return 'NetworkError'
    }

    if (this.hasResponseStatus(error)) {
      const status = error.response.status
      if (status === 401 || status === 403) {
        return 'AuthError'
      }
      return 'APIError'
    }

    if (this.hasName(error, 'ValidationError')) {
      return 'ValidationError'
    }

    if (this.hasBusinessCode(error)) {
      return 'BusinessError'
    }

    if (this.hasName(error, 'ComponentError')) {
      return 'ComponentError'
    }

    return 'UnknownError'
  }

  private handleNetworkError(_error: Error, _context?: ErrorContext): void {
    feedbackManager.error(ERROR_MESSAGES.NETWORK_ERROR)
  }

  private handleAPIError(error: Error, _context?: ErrorContext): void {
    const status = this.getResponseStatus(error)
    const message = this.getErrorMessage(status)
    feedbackManager.error(message)
  }

  private handleAuthError(error: Error, _context?: ErrorContext): void {
    const status = this.getResponseStatus(error)
    const message = this.getErrorMessage(status, ERROR_MESSAGES[401])
    feedbackManager.error(message)

    if (this.authRedirectTimer !== null) {
      clearTimeout(this.authRedirectTimer)
    }

    this.authRedirectTimer = setTimeout(() => {
      this.authRedirectTimer = null
      if (typeof window !== 'undefined') {
        window.location.href = '/login'
      }
    }, LOGIN_REDIRECT_DELAY)
  }

  private handleValidationError(error: Error, _context?: ErrorContext): void {
    feedbackManager.warning(error.message || '输入验证失败')
  }

  private handleBusinessError(error: Error, _context?: ErrorContext): void {
    const businessCode = this.getBusinessCode(error)
    const message = (businessCode ? ERROR_MESSAGES[businessCode] : undefined) || error.message || ERROR_MESSAGES.DEFAULT
    feedbackManager.warning(message)
  }

  private handleComponentError(error: Error, _context?: ErrorContext): void {
    logger.error('Component Error:', error)
    feedbackManager.error('页面加载失败，请刷新重试')
  }

  private handleUnknownError(error: Error, _context?: ErrorContext): void {
    logger.error('Unknown Error:', error)
    feedbackManager.error(ERROR_MESSAGES.DEFAULT)
  }

  log(error: Error, context?: ErrorContext): void {
    const timestamp = Date.now()
    const errorType = context?.type ?? this.classifyError(error)
    const log: ErrorLog = {
      id: this.createLogId(timestamp),
      type: errorType,
      message: error.message,
      stack: error.stack,
      context: {
        ...context,
        timestamp,
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown',
        url: typeof window !== 'undefined' ? window.location.href : 'unknown'
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
    safeRemoveItem(ERROR_LOG_STORAGE_KEY)
  }

  private saveLogs(): void {
    const saved = safeSetJSON(ERROR_LOG_STORAGE_KEY, this.logs)
    if (!saved) {
      logger.error('Failed to save error logs.')
    }
  }

  private loadLogs(): void {
    const savedLogs = safeGetJSON<ErrorLog[]>(ERROR_LOG_STORAGE_KEY)
    if (Array.isArray(savedLogs)) {
      this.logs = savedLogs.slice(-this.maxLogs)
      return
    }

    if (savedLogs !== null) {
      logger.error('Failed to load error logs: invalid storage format.')
    }
  }

  private createLogId(timestamp: number): string {
    const randomPart = Math.random().toString(36).slice(2, 10)
    return `error-${timestamp}-${randomPart}`
  }

  private getErrorMessage(status?: number, fallback?: string): string {
    if (typeof status === 'number' && status in ERROR_MESSAGES) {
      return ERROR_MESSAGES[status]
    }
    return fallback ?? ERROR_MESSAGES.DEFAULT
  }

  private getResponseStatus(error: Error): number | undefined {
    if (!this.hasResponseStatus(error)) {
      return undefined
    }
    return error.response.status
  }

  private getBusinessCode(error: Error): string | undefined {
    if (!this.hasBusinessCode(error)) {
      return undefined
    }
    return error.code
  }

  private isNetworkError(error: unknown): error is Error {
    if (!(error instanceof Error)) {
      return false
    }
    return error.message.includes('Network') || error.message.includes('fetch')
  }

  private hasResponseStatus(error: unknown): error is Error & { response: { status: number } } {
    if (!(error instanceof Error)) {
      return false
    }

    const response = (error as Error & { response?: { status?: unknown } }).response
    return typeof response?.status === 'number'
  }

  private hasBusinessCode(error: unknown): error is Error & { code: string } {
    if (!(error instanceof Error)) {
      return false
    }

    const code = (error as Error & { code?: unknown }).code
    return typeof code === 'string' && code.startsWith('BUSINESS_')
  }

  private hasName(error: unknown, expectedName: string): error is Error {
    return error instanceof Error && error.name === expectedName
  }
}

export const errorCenter = ErrorCenter.getInstance()
