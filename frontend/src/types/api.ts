export interface PaginatedApiResponse<T> {
  success: boolean
  data: T
  message?: string
  total?: number
  page?: number
  size?: number
  totalPages?: number
}
