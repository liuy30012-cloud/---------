// API Configuration
const DEFAULT_API_BASE_URL = '/'

export const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
  timeout: 10000, // 10 seconds
} as const

export const OFFLINE_SYNC_PATHS = {
  hotBooks: '/api/books/search?sort=borrowedCount,desc&size=100',
} as const
