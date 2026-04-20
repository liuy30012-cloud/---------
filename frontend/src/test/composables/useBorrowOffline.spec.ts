import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

const applyBorrowMock = vi.fn()
const pickupBorrowMock = vi.fn()
const returnBookMock = vi.fn()
const renewBorrowMock = vi.fn()
const reserveBookMock = vi.fn()
const cancelReservationMock = vi.fn()
const pickupReservationMock = vi.fn()
const extendReservationMock = vi.fn()
const enqueueOperationMock = vi.fn()

vi.mock('@/api/borrowApi', () => ({
  borrowApi: {
    applyBorrow: applyBorrowMock,
    pickupBorrow: pickupBorrowMock,
    returnBook: returnBookMock,
    renewBorrow: renewBorrowMock,
  },
  reservationApi: {
    reserveBook: reserveBookMock,
    cancelReservation: cancelReservationMock,
    pickupReservation: pickupReservationMock,
    extendReservation: extendReservationMock,
  },
}))

describe('Offline borrow operations', () => {
  beforeEach(() => {
    applyBorrowMock.mockReset()
    pickupBorrowMock.mockReset()
    returnBookMock.mockReset()
    renewBorrowMock.mockReset()
    enqueueOperationMock.mockReset()
  })

  it('queues borrow_apply when offline', async () => {
    const isOnline = ref(false)
    const { applyBorrowOfflineAware } = await import('@/composables/useBorrowOffline')

    await applyBorrowOfflineAware({
      bookId: 42,
      notes: 'test note',
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'borrow_apply',
      payload: { bookId: 42, notes: 'test note' },
    })
    expect(applyBorrowMock).not.toHaveBeenCalled()
  })

  it('calls API directly when online', async () => {
    const isOnline = ref(true)
    applyBorrowMock.mockResolvedValue({ data: { success: true } })

    const { applyBorrowOfflineAware } = await import('@/composables/useBorrowOffline')

    await applyBorrowOfflineAware({
      bookId: 42,
      notes: 'test note',
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(applyBorrowMock).toHaveBeenCalledWith({ bookId: 42, notes: 'test note' })
    expect(enqueueOperationMock).not.toHaveBeenCalled()
  })

  it('queues borrow_return when offline', async () => {
    const isOnline = ref(false)
    const { returnBookOfflineAware } = await import('@/composables/useBorrowOffline')

    await returnBookOfflineAware({
      recordId: 123,
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'borrow_return',
      payload: { recordId: 123 },
    })
    expect(returnBookMock).not.toHaveBeenCalled()
  })
})

describe('Offline reservation operations', () => {
  beforeEach(() => {
    reserveBookMock.mockReset()
    cancelReservationMock.mockReset()
    enqueueOperationMock.mockReset()
  })

  it('queues reservation_create when offline', async () => {
    const isOnline = ref(false)
    const { reserveBookOfflineAware } = await import('@/composables/useBorrowOffline')

    await reserveBookOfflineAware({
      bookId: 42,
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'reservation_create',
      payload: { bookId: 42 },
    })
    expect(reserveBookMock).not.toHaveBeenCalled()
  })

  it('queues reservation_cancel when offline', async () => {
    const isOnline = ref(false)
    const { cancelReservationOfflineAware } = await import('@/composables/useBorrowOffline')

    await cancelReservationOfflineAware({
      reservationId: 123,
      reason: 'changed mind',
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'reservation_cancel',
      payload: { reservationId: 123, reason: 'changed mind' },
    })
    expect(cancelReservationMock).not.toHaveBeenCalled()
  })
})
