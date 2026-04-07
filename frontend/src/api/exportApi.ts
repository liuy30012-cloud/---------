import httpClient from './httpClient'

export const exportApi = {
  exportBorrowHistory: (format: 'excel' | 'json' = 'excel') =>
    httpClient.get('/api/export/borrow-history', {
      params: { format },
      responseType: 'blob',
    }),

  exportBookReviews: () =>
    httpClient.get('/api/export/book-reviews', {
      responseType: 'blob',
    }),

  exportAllData: () =>
    httpClient.get('/api/export/all-data', {
      responseType: 'blob',
    }),
}
