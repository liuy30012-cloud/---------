export function handleImageError(e: Event, fallbackSrc: string = '/school-badge.png') {
  const target = e.target as HTMLImageElement
  if (target) {
    target.src = fallbackSrc
    target.onerror = null
  }
}
