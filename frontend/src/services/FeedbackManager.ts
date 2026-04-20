// frontend/src/services/FeedbackManager.ts
import type { Toast, ToastOptions, ToastType } from '@/types/feedback'

class FeedbackManager {
  private static instance: FeedbackManager
  private toasts: Toast[] = []
  private maxToasts = 5
  private listeners: Set<(toasts: Toast[]) => void> = new Set()
  private priority = { error: 4, warning: 3, success: 2, info: 1, loading: 0 }
  private timers: Map<string, number> = new Map()
  private idCounter = 0

  private constructor() {}

  static getInstance(): FeedbackManager {
    if (!FeedbackManager.instance) {
      FeedbackManager.instance = new FeedbackManager()
    }
    return FeedbackManager.instance
  }

  show(options: ToastOptions): string {
    const id = `toast-${Date.now()}-${++this.idCounter}`
    const toast: Toast = {
      ...options,
      id,
      createdAt: Date.now(),
      duration: options.duration ?? 3000,
      closable: options.closable ?? true,
      position: options.position ?? 'bottom-right',
      showProgress: options.showProgress ?? false
    }

    // 检查是否有相同消息（去重并重置定时器）
    const duplicate = this.toasts.find(t => t.message === toast.message && t.type === toast.type)
    if (duplicate) {
      // 清理旧定时器
      const oldTimer = this.timers.get(duplicate.id)
      if (oldTimer) {
        clearTimeout(oldTimer)
        this.timers.delete(duplicate.id)
      }
      // 移除旧 toast
      this.toasts = this.toasts.filter(t => t.id !== duplicate.id)
      // 继续添加新 toast（会设置新定时器）
    }

    // 添加到队列
    this.toasts.push(toast)

    // 按优先级排序
    this.toasts.sort((a, b) => this.priority[b.type] - this.priority[a.type])

    // 限制最大数量
    if (this.toasts.length > this.maxToasts) {
      this.toasts = this.toasts.slice(0, this.maxToasts)
    }

    // 通知监听器
    this.notifyListeners()

    // 自动移除并存储定时器
    if (toast.duration > 0 && toast.type !== 'loading') {
      const timer = window.setTimeout(() => this.hide(id), toast.duration)
      this.timers.set(id, timer)
    }

    return id
  }

  success(message: string): string {
    return this.show({ type: 'success', message })
  }

  error(message: string): string {
    return this.show({ type: 'error', message })
  }

  warning(message: string): string {
    return this.show({ type: 'warning', message })
  }

  info(message: string): string {
    return this.show({ type: 'info', message })
  }

  loading(message: string): string {
    return this.show({ type: 'loading', message, duration: 0 })
  }

  hide(id: string): void {
    // 清理定时器
    const timer = this.timers.get(id)
    if (timer) {
      clearTimeout(timer)
      this.timers.delete(id)
    }
    // 移除 toast
    this.toasts = this.toasts.filter(t => t.id !== id)
    this.notifyListeners()
  }

  clear(): void {
    // 清理所有定时器
    this.timers.forEach(timer => clearTimeout(timer))
    this.timers.clear()
    // 清空 toasts
    this.toasts = []
    this.notifyListeners()
  }

  getToasts(): Toast[] {
    return [...this.toasts]
  }

  subscribe(listener: (toasts: Toast[]) => void): () => void {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  private notifyListeners(): void {
    const toasts = this.getToasts()
    this.listeners.forEach(listener => {
      try {
        listener(toasts)
      } catch (error) {
        console.error('FeedbackManager: Listener error:', error)
      }
    })
  }
}

export const feedbackManager = FeedbackManager.getInstance()
