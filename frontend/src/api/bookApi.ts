import httpClient from './httpClient'
import type { PaginatedApiResponse } from '../types/api'
import type { Book, BookDetail, BookSearchParams, SmartSearchParams, SmartSearchResponse } from '../types/book'

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
