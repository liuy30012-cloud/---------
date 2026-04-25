import { API_CONFIG } from '../config'

function apiOrigin() {
  try {
    return new URL(API_CONFIG.baseURL, window.location.origin).origin
  } catch {
    return window.location.origin
  }
}

export function resolveCoverUrl(coverUrl: string | null | undefined) {
  if (!coverUrl) return ''
  if (/^https?:\/\//i.test(coverUrl)) return coverUrl
  if (coverUrl.startsWith('/book-covers/') || coverUrl.startsWith('/damage-photos/')) {
    return `${apiOrigin()}${coverUrl}`
  }
  return coverUrl
}
