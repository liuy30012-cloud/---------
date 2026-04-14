import { reactive, toRefs } from 'vue'

export interface DialogState {
  open: boolean
  eyebrow: string
  title: string
  message: string
  confirmText: string
  cancelText: string
  action: string
}

export function useConfirmDialog() {
  const dialog = reactive<DialogState>({
    open: false,
    eyebrow: '',
    title: '',
    message: '',
    confirmText: '',
    cancelText: '',
    action: '',
  })

  function openDialog(options: {
    eyebrow?: string
    title: string
    message: string
    confirmText: string
    cancelText?: string
    action: string
  }) {
    dialog.open = true
    dialog.eyebrow = options.eyebrow || ''
    dialog.title = options.title
    dialog.message = options.message
    dialog.confirmText = options.confirmText
    dialog.cancelText = options.cancelText || ''
    dialog.action = options.action
  }

  function closeDialog() {
    dialog.open = false
    dialog.action = ''
  }

  return { dialog, openDialog, closeDialog }
}
