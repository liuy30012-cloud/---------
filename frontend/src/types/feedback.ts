// frontend/src/types/feedback.ts
export type ToastType = 'success' | 'error' | 'info' | 'warning' | 'loading'

export type ToastPosition =
  | 'top-left'
  | 'top-center'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-center'
  | 'bottom-right'

export interface ToastAction {
  label: string
  onClick: () => void
}

export interface ToastOptions {
  type: ToastType
  message: string
  duration?: number
  closable?: boolean
  position?: ToastPosition
  showProgress?: boolean
  actions?: ToastAction[]
  icon?: string
}

export interface Toast extends ToastOptions {
  id: string
  createdAt: number
}

export interface ToastPriority {
  error: number
  warning: number
  success: number
  info: number
  loading: number
}
