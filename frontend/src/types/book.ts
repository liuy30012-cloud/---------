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
