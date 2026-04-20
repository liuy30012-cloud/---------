import { computed } from 'vue'
import { errorCenter } from '@/services/ErrorCenter'
import type { ErrorContext, ErrorLog, RetryOptions } from '@/types/error'

export function captureError(error: Error, context?: ErrorContext): ErrorLog {
  return errorCenter.capture(error, context)
}

export function clearActiveError(): void {
  errorCenter.clearActive()
}

export function useError() {
  const activeError = computed(() => errorCenter.getActive())

  const handle = (error: Error, context?: ErrorContext) => {
    return errorCenter.handle(error, context)
  }

  const retry = async <T>(
    fn: () => Promise<T>,
    options?: RetryOptions,
  ): Promise<T> => {
    return errorCenter.retry(fn, options)
  }

  const getLogs = () => errorCenter.getLogs()
  const clearLogs = () => errorCenter.clearLogs()

  return {
    activeError,
    handle,
    retry,
    getLogs,
    clearLogs,
    clearActiveError,
  }
}
