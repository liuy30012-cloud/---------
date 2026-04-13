import httpClient from './httpClient'
import type { PaginatedApiResponse } from './bookApi'

export interface DamageReport {
  id: number
  bookId: number
  bookTitle: string
  reporterId: number
  reporterName: string
  damageTypes: string[]
  description: string | null
  photoUrls: string[]
  status: 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
  adminNotes: string | null
  resolvedByName: string | null
  resolvedAt: string | null
  createdAt: string
}

export interface DamageReportStatistics {
  pendingCount: number
  inProgressCount: number
  resolvedCount: number
  rejectedCount: number
  totalCount: number
}

export const damageReportApi = {
  submitReport: (data: FormData) =>
    httpClient.post<{ success: boolean; data: DamageReport; message?: string }>('/api/damage-reports', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    }),

  getMyReports: (page = 0, size = 10) =>
    httpClient.get<PaginatedApiResponse<DamageReport[]>>('/api/damage-reports', { params: { page, size } }),

  getReportDetail: (id: number) =>
    httpClient.get<{ success: boolean; data: DamageReport }>(`/api/damage-reports/${id}`),

  getAllReports: (params: { status?: string; page?: number; size?: number }) =>
    httpClient.get<PaginatedApiResponse<DamageReport[]>>('/api/damage-reports/admin/all', { params }),

  getStatistics: () =>
    httpClient.get<{ success: boolean; data: DamageReportStatistics }>('/api/damage-reports/admin/statistics'),

  updateStatus: (id: number, payload: { status: string; adminNotes?: string }) =>
    httpClient.patch<{ success: boolean; data: DamageReport; message?: string }>(`/api/damage-reports/admin/${id}/status`, payload),

  deleteReport: (id: number) =>
    httpClient.delete<{ success: boolean; message?: string }>(`/api/damage-reports/admin/${id}`),
}
