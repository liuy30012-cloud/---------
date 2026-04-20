// frontend/src/types/feedback.ts

/**
 * Toast 消息类型
 */
export type ToastType = 'success' | 'error' | 'info' | 'warning' | 'loading'

/**
 * Toast 显示位置
 */
export type ToastPosition =
  | 'top-left'
  | 'top-center'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-center'
  | 'bottom-right'

/**
 * Toast 操作按钮配置
 */
export interface ToastAction {
  label: string
  onClick: () => void
}

/**
 * Toast 配置选项
 */
export interface ToastOptions {
  type: ToastType
  message: string
  duration?: number  // 默认 3000ms
  closable?: boolean  // 默认 true
  position?: ToastPosition  // 默认 'bottom-right'
  showProgress?: boolean  // 默认 false
  actions?: readonly ToastAction[]
  icon?: string
}

/**
 * Toast 实例（包含运行时信息）
 */
export interface Toast extends ToastOptions {
  id: string
  createdAt: number
}

/**
 * Toast 类型优先级映射
 */
export type ToastPriority = Record<ToastType, number>
