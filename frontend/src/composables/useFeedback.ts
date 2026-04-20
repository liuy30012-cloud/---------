// frontend/src/composables/useFeedback.ts
import { ref, readonly, onUnmounted } from 'vue'
import { feedbackManager } from '@/services/FeedbackManager'
import type { Toast, ToastOptions } from '@/types/feedback'
import type { Ref } from 'vue'

export function useFeedback(): {
  toasts: Readonly<Ref<Toast[]>>
  show: (options: ToastOptions) => string
  success: (message: string) => string
  error: (message: string) => string
  warning: (message: string) => string
  info: (message: string) => string
  loading: (message: string) => string
  hide: (id: string) => void
  clear: () => void
} {
  const toasts = ref<Toast[]>([])

  // 立即订阅，不要等到 onMounted
  const unsubscribe = feedbackManager.subscribe((newToasts) => {
    toasts.value = newToasts
  })

  // 立即获取初始状态
  toasts.value = feedbackManager.getToasts()

  // 组件卸载时取消订阅
  onUnmounted(() => {
    unsubscribe()
  })

  const show = (options: ToastOptions) => feedbackManager.show(options)
  const success = (message: string) => feedbackManager.success(message)
  const error = (message: string) => feedbackManager.error(message)
  const warning = (message: string) => feedbackManager.warning(message)
  const info = (message: string) => feedbackManager.info(message)
  const loading = (message: string) => feedbackManager.loading(message)
  const hide = (id: string) => feedbackManager.hide(id)
  const clear = () => feedbackManager.clear()

  return {
    toasts: readonly(toasts),
    show,
    success,
    error,
    warning,
    info,
    loading,
    hide,
    clear
  }
}
