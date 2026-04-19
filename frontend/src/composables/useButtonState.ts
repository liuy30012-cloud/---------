import { ref, readonly } from 'vue'

export type ButtonState = 'idle' | 'loading' | 'success' | 'error'

const FEEDBACK_DURATION = 2000

export function useButtonState() {
  const state = ref<ButtonState>('idle')
  let feedbackTimer: ReturnType<typeof setTimeout> | null = null

  function clearTimer() {
    if (feedbackTimer) {
      clearTimeout(feedbackTimer)
      feedbackTimer = null
    }
  }

  async function execute(action: () => Promise<unknown>): Promise<boolean> {
    if (state.value === 'loading') return false

    clearTimer()
    state.value = 'loading'

    try {
      await action()
      state.value = 'success'
      feedbackTimer = setTimeout(() => {
        state.value = 'idle'
        feedbackTimer = null
      }, FEEDBACK_DURATION)
      return true
    } catch {
      state.value = 'error'
      feedbackTimer = setTimeout(() => {
        state.value = 'idle'
        feedbackTimer = null
      }, FEEDBACK_DURATION)
      return false
    }
  }

  function reset() {
    clearTimer()
    state.value = 'idle'
  }

  return {
    state: readonly(state),
    execute,
    reset,
  }
}
