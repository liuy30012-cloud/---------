import httpClient from './httpClient'

export interface Book {
  id: number
  title: string
  author: string
  isbn: string
  location: string
  coverUrl: string
  status: string
  year: string
  description: string
  languageCode: string
  availability: string
  category: string
  totalCopies: number
  availableCopies: number
  borrowedCount: number
  circulationPolicy: 'AUTO' | 'MANUAL' | 'REFERENCE_ONLY'
}

export interface BookSearchParams {
  keyword?: string
  author?: string
  year?: string
  category?: string
  status?: string
  language?: string
  sort?: string
  page?: number
  size?: number
}

export interface PaginatedApiResponse<T> {
  success: boolean
  data: T
  message?: string
  total?: number
  page?: number
  size?: number
  totalPages?: number
}

export interface BookReview {
  id: number
  bookId: number
  userId: number
  username: string
  rating: number
  content: string
  createdAt: string
  updatedAt: string
}

export interface BookDetail {
  id: number
  title: string
  author: string
  isbn: string
  location: string
  coverUrl: string
  status: string
  year: string
  description: string
  languageCode: string
  availability: string
  category: string
  totalCopies: number
  availableCopies: number
  borrowedCount: number
  circulationPolicy: 'AUTO' | 'MANUAL' | 'REFERENCE_ONLY'
  averageRating: number
  totalReviews: number
  latestReviews: BookReview[]
  borrowHistorySummary: {
    totalBorrows: number
    activeBorrowCount: number
    lastBorrowedAt: string | null
    recentActivity: Array<{
      id: number
      status: string
      borrowDate: string | null
      dueDate: string | null
      returnDate: string | null
    }>
  }
  relatedBooks: Array<{
    id: number
    title: string
    author: string
    coverUrl: string
    location: string
    availableCopies: number
    circulationPolicy: 'AUTO' | 'MANUAL' | 'REFERENCE_ONLY'
  }>
  availabilityContext: {
    canBorrow: boolean
    canReserve: boolean
    state: string
    summary: string
    nextAction: string
    availableCopies: number
    totalCopies: number
  }
  queueContext: {
    waitingCount: number
    availableReservationCount: number
    estimatedWaitDays: number
    summary: string
  }
  locationContext: {
    breadcrumbs: string[]
    pickupCardTitle: string
    pickupHint: string
    adjacentRecommendation?: string
  }
}

export interface SmartSearchParams {
  query: string
  page?: number
  size?: number
}

export interface SmartSearchResponse {
  books: Book[]
  total: number
  page: number
  totalPages: number
  originalQuery: string
  normalizedQuery: string
  searchEngine: 'ELASTICSEARCH' | 'MYSQL_FALLBACK'
  didYouMean?: string
  suggestions?: string[]
  interpretation?: string
}

export const bookApi = {
  searchBooks: (params: BookSearchParams) =>
    httpClient.get<PaginatedApiResponse<Book[]>>('/api/books/search', { params }),

  smartSearch: (params: SmartSearchParams) =>
    httpClient.get<{ success: boolean; data: SmartSearchResponse; message?: string }>('/api/smart-search/search', { params }),

  getSearchSuggestions: (prefix: string, limit = 8) =>
    httpClient.get<{ success: boolean; data: string[]; message?: string }>('/api/smart-search/suggestions', {
      params: { prefix, limit },
    }),

  getBookDetail: (id: number) =>
    httpClient.get<{ success: boolean; data: BookDetail; message?: string }>(`/api/books/${id}`),

  getBookReviews: (bookId: number, page = 0, size = 10) =>
    httpClient.get(`/api/reviews/book/${bookId}`, {
      params: { page, size, sortBy: 'createdAt' },
    }),

  addReview: (payload: { bookId: number; rating: number; content: string }) =>
    httpClient.post('/api/reviews', payload),

  getCategories: () =>
    httpClient.get<{ success: boolean; data: string[] }>('/api/books/categories'),

  getLanguages: () =>
    httpClient.get<{ success: boolean; data: string[] }>('/api/books/languages'),
}
