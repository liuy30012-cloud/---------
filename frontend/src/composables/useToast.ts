import { reactive } from 'vue'

export interface ToastState {
  message: string
  type: 'success' | 'error' | 'info'
}

const TOAST_DURATION = 2600

export function useToast() {
  const toast = reactive<ToastState>({
    message: '',
    type: 'info',
  })

  let timer: ReturnType<typeof setTimeout> | null = null

  function showToast(message: string, type: ToastState['type'] = 'info') {
    if (timer) clearTimeout(timer)
    toast.message = message
    toast.type = type
    timer = setTimeout(() => {
      toast.message = ''
      timer = null
    }, TOAST_DURATION)
  }

  return { toast, showToast }
}
