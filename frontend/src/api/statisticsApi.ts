import httpClient from './httpClient'

export interface PopularBook {
  bookId: number
  title: string
  author: string
  isbn: string
  borrowCount: number
  coverUrl: string
}

export interface BorrowTrend {
  date: string
  borrowCount: number
  returnCount: number
}

export interface CategoryStatistics {
  category: string
  totalBooks: number
  borrowedBooks: number
  availableBooks: number
  borrowRate: number
}

export interface UserProfile {
  userId: number
  studentId: string
  username: string
  totalBorrows: number
  currentBorrows: number
  favoriteCategory: string
  averageBorrowDays: number
}

export interface InventoryStatistics {
  totalBooks: number
  availableBooks: number
  borrowedBooks: number
  overdueBooks: number
  reservedBooks: number
  utilizationRate: number
}

export interface DashboardData {
  inventory: InventoryStatistics
  popularBooks: PopularBook[]
  borrowTrends: BorrowTrend[]
  categoryStatistics: CategoryStatistics[]
  totalUsers: number
  activeUsers: number
}

export interface InventoryAlert {
  bookId: number
  title: string
  author: string
  isbn: string
  category: string
  totalCopies: number
  availableCopies: number
  borrowedCopies: number
  borrowCount: number
  alertType: 'LOW_STOCK' | 'OUT_OF_STOCK' | 'HIGH_DEMAND'
  alertLevel: 'WARNING' | 'CRITICAL'
  message: string
  coverUrl: string
}

export interface InventoryAlertSummary {
  totalAlerts: number
  criticalAlerts: number
  warningAlerts: number
  outOfStockCount: number
  lowStockCount: number
  highDemandCount: number
  alerts: InventoryAlert[]
}

export interface PurchaseSuggestion {
  bookId: number
  title: string
  author: string
  isbn: string
  category: string
  currentCopies: number
  suggestedCopies: number
  additionalCopies: number
  borrowCount: number
  averageWaitTime: number
  reservationCount: number
  reason: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  score: number
  coverUrl: string
}

export interface PurchaseSuggestionSummary {
  totalSuggestions: number
  highPriority: number
  mediumPriority: number
  lowPriority: number
  totalAdditionalCopies: number
  estimatedBudget: number
  suggestions: PurchaseSuggestion[]
}

export const statisticsApi = {
  getPopularBooks: (limit: number = 10) =>
    httpClient.get(`/api/statistics/popular-books?limit=${limit}`, {
      skipErrorHandling: true,
    }),

  getBorrowTrends: (days: number = 30) =>
    httpClient.get(`/api/statistics/borrow-trends?days=${days}`),

  getCategoryStatistics: () =>
    httpClient.get('/api/statistics/category-statistics'),

  getUserProfile: () =>
    httpClient.get('/api/statistics/user-profile'),

  getInventoryStatistics: () =>
    httpClient.get('/api/statistics/inventory-statistics'),

  getDashboardData: () =>
    httpClient.get('/api/statistics/dashboard'),

  getInventoryAlerts: () =>
    httpClient.get('/api/statistics/inventory-alerts'),

  getPurchaseSuggestions: () =>
    httpClient.get('/api/statistics/purchase-suggestions'),
}
