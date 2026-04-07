import { logger } from './logger'

export function safeSetItem(key: string, value: string): boolean {
  try {
    localStorage.setItem(key, value)
    return true
  } catch (e) {
    logger.warn(`Failed to save to localStorage (${key}):`, e)
    return false
  }
}

export function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch (e) {
    logger.warn(`Failed to read from localStorage (${key}):`, e)
    return null
  }
}

export function safeRemoveItem(key: string): boolean {
  try {
    localStorage.removeItem(key)
    return true
  } catch (e) {
    logger.warn(`Failed to remove from localStorage (${key}):`, e)
    return false
  }
}

export function safeSetJSON<T>(key: string, value: T): boolean {
  try {
    localStorage.setItem(key, JSON.stringify(value))
    return true
  } catch (e) {
    logger.warn(`Failed to save JSON to localStorage (${key}):`, e)
    return false
  }
}

export function safeGetJSON<T>(key: string): T | null {
  try {
    const item = localStorage.getItem(key)
    return item ? JSON.parse(item) : null
  } catch (e) {
    logger.warn(`Failed to parse JSON from localStorage (${key}):`, e)
    return null
  }
}
