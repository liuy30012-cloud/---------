import httpClient from './httpClient'
import type { PaginatedApiResponse } from './bookApi'

export type ManagedUserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

export interface ManagedUser {
  id: number
  studentId: string
  username: string
  email: string | null
  role: ManagedUserRole
  avatarUrl: string | null
  status: 0 | 1
  loginCount: number
  lastLoginTime: string | null
  createdAt: string
}

export interface UserManagementStatistics {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  activeAdmins: number
}

export interface UserManagementQuery {
  keyword?: string
  role?: ManagedUserRole | ''
  status?: 0 | 1 | ''
  page?: number
  size?: number
}

export const userManagementApi = {
  getUsers: (params: UserManagementQuery) =>
    httpClient.get<PaginatedApiResponse<ManagedUser[]>>('/api/users/admin/all', { params }),

  getStatistics: () =>
    httpClient.get<{ success: boolean; data: UserManagementStatistics; message?: string }>('/api/users/admin/statistics'),

  updateRole: (userId: number, role: ManagedUserRole) =>
    httpClient.patch<{ success: boolean; data: ManagedUser; message?: string }>(`/api/users/admin/${userId}/role`, { role }),

  updateStatus: (userId: number, status: 0 | 1) =>
    httpClient.patch<{ success: boolean; data: ManagedUser; message?: string }>(`/api/users/admin/${userId}/status`, { status }),
}
