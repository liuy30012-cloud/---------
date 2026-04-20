import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

const favoriteApiMock = {
  addFavorite: vi.fn(),
  removeFavorite: vi.fn(),
}

const borrowApiMock = {
  applyBorrow: vi.fn(),
  pickupBorrow: vi.fn(),
  returnBook: vi.fn(),
  renewBorrow: vi.fn(),
}

const reservationApiMock = {
  reserveBook: vi.fn(),
  cancelReservation: vi.fn(),
  pickupReservation: vi.fn(),
  extendReservation: vi.fn(),
}

vi.mock('@/api/favoriteApi', () => ({
  favoriteApi: favoriteApiMock,
}))

vi.mock('@/api/borrowApi', () => ({
  borrowApi: borrowApiMock,
  reservationApi: reservationApiMock,
}))

describe('Offline queue sync', () => {
  beforeEach(() => {
    Object.values(favoriteApiMock).forEach(fn => fn.mockReset())
    Object.values(borrowApiMock).forEach(fn => fn.mockReset())
    Object.values(reservationApiMock).forEach(fn => fn.mockReset())
  })

  it('syncs favorite_add operation', async () => {
    favoriteApiMock.addFavorite.mockResolvedValue({ data: { success: true } })

    const { syncOfflineOperation } = await import('@/composables/useOfflineSync')

    await syncOfflineOperation({
      id: 'op1',
      type: 'favorite_add',
      payload: { bookId: 42 },
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      retryCount: 0,
    })

    expect(favoriteApiMock.addFavorite).toHaveBeenCalledWith(42)
  })

  it('syncs borrow_apply operation', async () => {
    borrowApiMock.applyBorrow.mockResolvedValue({ data: { success: true } })

    const { syncOfflineOperation } = await import('@/composables/useOfflineSync')

    await syncOfflineOperation({
      id: 'op2',
      type: 'borrow_apply',
      payload: { bookId: 42, notes: 'test' },
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      retryCount: 0,
    })

    expect(borrowApiMock.applyBorrow).toHaveBeenCalledWith({ bookId: 42, notes: 'test' })
  })

  it('syncs reservation_create operation', async () => {
    reservationApiMock.reserveBook.mockResolvedValue({ data: { success: true } })

    const { syncOfflineOperation } = await import('@/composables/useOfflineSync')

    await syncOfflineOperation({
      id: 'op3',
      type: 'reservation_create',
      payload: { bookId: 42 },
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      retryCount: 0,
    })

    expect(reservationApiMock.reserveBook).toHaveBeenCalledWith({ bookId: 42 })
  })

  it('throws error when operation fails', async () => {
    favoriteApiMock.addFavorite.mockRejectedValue(new Error('API error'))

    const { syncOfflineOperation } = await import('@/composables/useOfflineSync')

    await expect(syncOfflineOperation({
      id: 'op4',
      type: 'favorite_add',
      payload: { bookId: 42 },
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      retryCount: 0,
    })).rejects.toThrow('API error')
  })
})
