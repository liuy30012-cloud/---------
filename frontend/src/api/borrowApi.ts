import httpClient from './httpClient'

export interface BorrowRequest {
  bookId: number
  notes?: string
}

export interface BorrowRecord {
  id: number
  bookId: number
  bookTitle: string
  bookIsbn: string
  borrowDate: string | null
  dueDate: string | null
  returnDate?: string | null
  status: string
  renewed: boolean
  renewCount: number
  overdueDays: number
  fineAmount: number
  finePaid: boolean
  notes?: string
  approvedAt?: string | null
  rejectReason?: string | null
  nextAction?: string
  statusHint?: string
  pickupDeadline?: string | null
  coverUrl?: string
  location?: string
  circulationPolicy?: 'AUTO' | 'MANUAL' | 'REFERENCE_ONLY'
  createdAt: string
}

export interface ReservationRequest {
  bookId: number
}

export interface ReservationRecord {
  id: number
  bookId: number
  bookTitle: string
  reservationDate: string
  expireDate: string | null
  notifyDate?: string | null
  pickupDeadline?: string | null
  status: string
  queuePosition: number
  queueAhead?: number
  estimatedWaitDays?: number
  nextAction?: string
  statusHint?: string
  coverUrl?: string
  location?: string
  createdAt: string
  // 延期相关字段
  extensionCount?: number
  canExtend?: boolean
  pickupWindowDays?: number
  // 过期提醒相关字段
  daysUntilExpiry?: number
  isExpiringSoon?: boolean
}

export const borrowApi = {
  applyBorrow: (request: BorrowRequest) =>
    httpClient.post('/api/borrow/apply', request),

  pickupBorrow: (recordId: number) =>
    httpClient.post(`/api/borrow/${recordId}/pickup`),

  returnBook: (recordId: number) =>
    httpClient.post(`/api/borrow/${recordId}/return`),

  renewBorrow: (recordId: number) =>
    httpClient.post(`/api/borrow/${recordId}/renew`),

  getBorrowHistory: () =>
    httpClient.get('/api/borrow/history'),

  getCurrentBorrows: () =>
    httpClient.get('/api/borrow/current'),
}

export const reservationApi = {
  reserveBook: (request: ReservationRequest) =>
    httpClient.post('/api/reservation', request),

  cancelReservation: (reservationId: number, reason?: string) =>
    httpClient.delete(`/api/reservation/${reservationId}`, { params: { reason } }),

  getReservations: () =>
    httpClient.get('/api/reservation'),

  pickupReservation: (reservationId: number) =>
    httpClient.post(`/api/reservation/${reservationId}/pickup`),

  extendReservation: (reservationId: number) =>
    httpClient.post(`/api/reservation/${reservationId}/extend`),
}
