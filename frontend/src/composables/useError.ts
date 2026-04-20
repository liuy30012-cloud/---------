import { errorCenter } from '@/services/ErrorCenter'
import type { ErrorContext, RetryOptions } from '@/types/error'

export function useError() {
  const handle = (error: Error, context?: ErrorContext) => {
    errorCenter.handle(error, context)
  }

  const retry = async <T>(
    fn: () => Promise<T>,
    options?: RetryOptions
  ): Promise<T> => {
    return errorCenter.retry(fn, options)
  }

  const getLogs = () => errorCenter.getLogs()
  const clearLogs = () => errorCenter.clearLogs()

  return {
    handle,
    retry,
    getLogs,
    clearLogs
  }
}
