/**
 * 统一日志工具
 * 生产环境可通过环境变量控制是否输出日志
 */

const isDevelopment = import.meta.env.DEV

export const logger = {
  log(...args: any[]) {
    if (isDevelopment) {
      console.log(...args)
    }
  },

  warn(...args: any[]) {
    if (isDevelopment) {
      console.warn(...args)
    }
  },

  error(...args: any[]) {
    // 错误日志在生产环境也应该输出
    console.error(...args)
  },

  debug(...args: any[]) {
    if (isDevelopment) {
      console.debug(...args)
    }
  }
}
