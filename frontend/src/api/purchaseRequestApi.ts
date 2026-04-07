import httpClient from './httpClient'

export type PurchaseRequestStatus =
  | 'PENDING_REVIEW'
  | 'PRIORITY_POOL'
  | 'PLANNED'
  | 'PURCHASING'
  | 'ARRIVED'
  | 'REJECTED'

export interface PurchaseRequestItem {
  id: number
  title: string
  author: string
  isbn?: string | null
  reason?: string | null
  proposerUserId: number
  proposerName?: string | null
  supportCount: number
  status: PurchaseRequestStatus
  statusLabel: string
  statusNote?: string | null
  progressPercent: number
  progressLabel: string
  votedByCurrentUser: boolean
  canVote: boolean
  createdAt: string
  updatedAt: string
}

export interface PurchaseRequestBoard {
  totalRequests: number
  pendingReviewCount: number
  priorityPoolCount: number
  plannedCount: number
  purchasingCount: number
  arrivedCount: number
  rejectedCount: number
  totalSupportCount: number
  requests: PurchaseRequestItem[]
}

export interface CreatePurchaseRequestPayload {
  title: string
  author: string
  isbn?: string
  reason?: string
}

export interface PurchaseRequestCreateResult {
  created: boolean
  conflictType: 'NONE' | 'EXISTING_BOOK' | 'DUPLICATE_REQUEST'
  existingRequestId?: number | null
  existingBookId?: number | null
  request?: PurchaseRequestItem | null
}

export interface PurchaseRequestVoteResult {
  alreadyVoted: boolean
  request: PurchaseRequestItem
}

export interface UpdatePurchaseRequestStatusPayload {
  status: PurchaseRequestStatus
  statusNote?: string
}

interface ApiEnvelope<T> {
  success: boolean
  data: T
  message?: string
}

export const purchaseRequestApi = {
  getBoard: () =>
    httpClient.get<ApiEnvelope<PurchaseRequestBoard>>('/api/purchase-requests'),

  createRequest: (payload: CreatePurchaseRequestPayload) =>
    httpClient.post<ApiEnvelope<PurchaseRequestCreateResult>>('/api/purchase-requests', payload),

  voteRequest: (id: number) =>
    httpClient.post<ApiEnvelope<PurchaseRequestVoteResult>>(`/api/purchase-requests/${id}/vote`),

  updateStatus: (id: number, payload: UpdatePurchaseRequestStatusPayload) =>
    httpClient.patch<ApiEnvelope<PurchaseRequestItem>>(`/api/purchase-requests/${id}/status`, payload),
}
