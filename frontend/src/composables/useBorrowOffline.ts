import type { Ref } from 'vue'
import { borrowApi, reservationApi } from '@/api/borrowApi'
import type { QueueOperationInput } from '@/types/offline'
import { OFFLINE_OPERATION_TYPES } from '@/constants/offlineOperations'

interface BorrowOfflineParams {
  bookId: number
  notes?: string
  isOnline: Ref<boolean>
  enqueueOperation: (input: QueueOperationInput) => Promise<unknown>
}

interface BorrowActionOfflineParams {
  recordId: number
  isOnline: Ref<boolean>
  enqueueOperation: (input: QueueOperationInput) => Promise<unknown>
}

interface ReservationOfflineParams {
  bookId: number
  isOnline: Ref<boolean>
  enqueueOperation: (input: QueueOperationInput) => Promise<unknown>
}

interface ReservationActionOfflineParams {
  reservationId: number
  reason?: string
  isOnline: Ref<boolean>
  enqueueOperation: (input: QueueOperationInput) => Promise<unknown>
}

export async function applyBorrowOfflineAware(params: BorrowOfflineParams): Promise<void> {
  const { bookId, notes, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.BORROW_APPLY,
      payload: { bookId, notes },
    })
    return
  }

  await borrowApi.applyBorrow({ bookId, notes: notes || '' })
}

export async function pickupBorrowOfflineAware(params: BorrowActionOfflineParams): Promise<void> {
  const { recordId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.BORROW_PICKUP,
      payload: { recordId },
    })
    return
  }

  await borrowApi.pickupBorrow(recordId)
}

export async function returnBookOfflineAware(params: BorrowActionOfflineParams): Promise<void> {
  const { recordId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.BORROW_RETURN,
      payload: { recordId },
    })
    return
  }

  await borrowApi.returnBook(recordId)
}

export async function renewBorrowOfflineAware(params: BorrowActionOfflineParams): Promise<void> {
  const { recordId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.BORROW_RENEW,
      payload: { recordId },
    })
    return
  }

  await borrowApi.renewBorrow(recordId)
}

export async function reserveBookOfflineAware(params: ReservationOfflineParams): Promise<void> {
  const { bookId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.RESERVATION_RESERVE,
      payload: { bookId },
    })
    return
  }

  await reservationApi.reserveBook({ bookId })
}

export async function cancelReservationOfflineAware(params: ReservationActionOfflineParams): Promise<void> {
  const { reservationId, reason, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.RESERVATION_CANCEL,
      payload: { reservationId, reason },
    })
    return
  }

  await reservationApi.cancelReservation(reservationId, reason)
}

export async function pickupReservationOfflineAware(params: ReservationActionOfflineParams): Promise<void> {
  const { reservationId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.RESERVATION_PICKUP,
      payload: { reservationId },
    })
    return
  }

  await reservationApi.pickupReservation(reservationId)
}

export async function extendReservationOfflineAware(params: ReservationActionOfflineParams): Promise<void> {
  const { reservationId, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: OFFLINE_OPERATION_TYPES.RESERVATION_EXTEND,
      payload: { reservationId },
    })
    return
  }

  await reservationApi.extendReservation(reservationId)
}
