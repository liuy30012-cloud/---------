const MOJIBAKE_MARKERS = [
  '鐧', '閻', '鍊', '娑', '妫', '鏉', '鏆', '璇', '鍤', '鎿',
  '棣', '瀵', '鍙', '閫', '鎴', '锛', '鈥', '€',
]

function looksLikeMojibake(message: string): boolean {
  if (/[-]/u.test(message) || message.includes('\uFFFD')) {
    return true
  }

  const hitCount = MOJIBAKE_MARKERS.reduce((count, marker) => (
    count + (message.includes(marker) ? 1 : 0)
  ), 0)

  return hitCount >= 2
}

export function sanitizeApiMessage(message: unknown, fallback: string): string {
  if (typeof message !== 'string') {
    return fallback
  }

  const normalized = message.trim()
  if (!normalized || looksLikeMojibake(normalized)) {
    return fallback
  }

  return normalized
}
