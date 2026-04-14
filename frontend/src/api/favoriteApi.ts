import httpClient from './httpClient'

// ── 收藏相关 ──

export interface FavoriteInfo {
  id: number
  bookId: number
  bookTitle: string
  author: string
  coverUrl: string | null
  category: string | null
  availableCopies: number | null
  readingStatus: string | null
  notes: string | null
  favoritedAt: string
}

// ── 阅读状态相关 ──

export type ReadingStatusEnum = 'WANT_TO_READ' | 'READING' | 'READ'

export interface ReadingStatusInfo {
  id: number
  bookId: number
  bookTitle: string
  author: string
  coverUrl: string | null
  category: string | null
  status: ReadingStatusEnum
  notes: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ReadingStatusCounts {
  WANT_TO_READ: number
  READING: number
  READ: number
  TOTAL: number
}

// ── API 响应信封 ──

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message?: string
}

// ── 收藏 API ──

export const favoriteApi = {
  addFavorite: (bookId: number) =>
    httpClient.post<ApiEnvelope<FavoriteInfo>>('/api/favorites', { bookId }),

  removeFavorite: (bookId: number) =>
    httpClient.delete<ApiEnvelope<void>>(`/api/favorites/${bookId}`),

  getFavorites: () =>
    httpClient.get<ApiEnvelope<FavoriteInfo[]>>('/api/favorites'),

  checkFavorite: (bookId: number) =>
    httpClient.get<ApiEnvelope<boolean>>('/api/favorites/check', { params: { bookId } }),

  batchCheckFavorites: (bookIds: number[]) =>
    httpClient.post<ApiEnvelope<number[]>>('/api/favorites/batch-check', bookIds),
}

// ── 阅读状态 API ──

export const readingStatusApi = {
  upsertReadingStatus: (bookId: number, status: ReadingStatusEnum, notes?: string) =>
    httpClient.put<ApiEnvelope<ReadingStatusInfo>>('/api/reading-status', { bookId, status, notes: notes || null }),

  removeReadingStatus: (bookId: number) =>
    httpClient.delete<ApiEnvelope<void>>(`/api/reading-status/${bookId}`),

  getReadingStatuses: (status?: ReadingStatusEnum) =>
    httpClient.get<ApiEnvelope<ReadingStatusInfo[]>>('/api/reading-status', { params: status ? { status } : {} }),

  getReadingStatus: (bookId: number) =>
    httpClient.get<ApiEnvelope<ReadingStatusInfo>>(`/api/reading-status/${bookId}`),

  getStatusCounts: () =>
    httpClient.get<ApiEnvelope<ReadingStatusCounts>>('/api/reading-status/counts'),
}
