import type { ToastType } from '@/types/feedback'

export const TOAST_ICONS: Record<ToastType, string> = {
  success: 'check_circle',
  error: 'error',
  warning: 'warning',
  info: 'info',
  loading: 'progress_activity',
} as const

export const TOAST_TRANSITION_DURATION = 300
