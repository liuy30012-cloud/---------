export function formatRelativeTime(date: string | Date, locale: string = 'zh-CN'): string {
  const d = typeof date === 'string' ? new Date(date) : date
  if (Number.isNaN(d.getTime())) return ''

  const now = Date.now()
  const diff = Math.floor((now - d.getTime()) / 1000)

  if (locale === 'zh-CN') {
    if (diff < 60) return '刚刚'
    if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
    if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
    return `${Math.floor(diff / 86400)}天前`
  }

  if (diff < 60) return 'just now'
  if (diff < 3600) return `${Math.floor(diff / 60)} minutes ago`
  if (diff < 86400) return `${Math.floor(diff / 3600)} hours ago`
  return `${Math.floor(diff / 86400)} days ago`
}

function parseLocalDateTime(date: string | null | undefined): Date | null {
  if (!date) {
    return null
  }

  const normalized = date.trim()
  if (!normalized) {
    return null
  }

  const match = normalized.match(
    /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?)?$/,
  )

  if (match) {
    const [, year, month, day, hour = '0', minute = '0', second = '0', fraction = '0'] = match
    const milliseconds = Number(fraction.padEnd(3, '0').slice(0, 3))
    const localDate = new Date(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second),
      milliseconds,
    )

    if (!Number.isNaN(localDate.getTime())) {
      return localDate
    }
  }

  const fallback = new Date(normalized)
  return Number.isNaN(fallback.getTime()) ? null : fallback
}

export function formatLocalDate(date: string | null | undefined): string {
  const localDate = parseLocalDateTime(date)
  if (!localDate) {
    return date ? date : '-'
  }

  const year = localDate.getFullYear()
  const month = String(localDate.getMonth() + 1).padStart(2, '0')
  const day = String(localDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function isPastLocalDayEnd(date: string | null | undefined): boolean {
  const localDate = parseLocalDateTime(date)
  if (!localDate) {
    return false
  }

  localDate.setHours(23, 59, 59, 999)
  return Date.now() > localDate.getTime()
}
