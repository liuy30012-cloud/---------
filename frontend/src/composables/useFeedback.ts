// frontend/src/composables/useFeedback.ts
import { ref, onMounted, onUnmounted } from 'vue'
import { feedbackManager } from '@/services/FeedbackManager'
import type { Toast, ToastOptions } from '@/types/feedback'

export function useFeedback() {
  const toasts = ref<Toast[]>([])

  let unsubscribe: (() => void) | null = null

  onMounted(() => {
    unsubscribe = feedbackManager.subscribe((newToasts) => {
      toasts.value = newToasts
    })
    toasts.value = feedbackManager.getToasts()
  })

  onUnmounted(() => {
    if (unsubscribe) {
      unsubscribe()
    }
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
    toasts,
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
