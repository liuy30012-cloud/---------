const coverCache = new Map<string, string | null>()

function normalizeQueryPart(value: string | null | undefined) {
  return String(value || '')
    .replace(/["“”‘’]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function cacheKey(title: string, author: string) {
  return `${normalizeQueryPart(title).toLowerCase()}::${normalizeQueryPart(author).toLowerCase()}`
}

function toHttps(url: string | undefined) {
  if (!url) return ''
  return url.startsWith('http://') ? url.replace('http://', 'https://') : url
}

function bestGoogleBooksImage(imageLinks: Record<string, string> | undefined) {
  if (!imageLinks) return ''
  return toHttps(
    imageLinks.extraLarge
    || imageLinks.large
    || imageLinks.medium
    || imageLinks.thumbnail
    || imageLinks.smallThumbnail,
  )
}

export async function findOnlineBookCover(title: string, author: string): Promise<string | null> {
  const key = cacheKey(title, author)
  if (coverCache.has(key)) return coverCache.get(key) || null

  const cleanTitle = normalizeQueryPart(title)
  const cleanAuthor = normalizeQueryPart(author).split('/')[0]?.trim() || ''
  if (!cleanTitle) {
    coverCache.set(key, null)
    return null
  }

  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 6000)

  try {
    const query = cleanAuthor ? `intitle:${cleanTitle} inauthor:${cleanAuthor}` : `intitle:${cleanTitle}`
    const params = new URLSearchParams({ q: query, maxResults: '3', printType: 'books' })
    const response = await fetch(`https://www.googleapis.com/books/v1/volumes?${params.toString()}`, {
      signal: controller.signal,
    })

    if (response.ok) {
      const payload = await response.json()
      for (const item of payload.items || []) {
        const image = bestGoogleBooksImage(item?.volumeInfo?.imageLinks)
        if (image) {
          coverCache.set(key, image)
          return image
        }
      }
    }
  } catch {
    // 网络封面只是增强展示，失败时继续使用系统占位封面。
  } finally {
    window.clearTimeout(timeout)
  }

  coverCache.set(key, null)
  return null
}
