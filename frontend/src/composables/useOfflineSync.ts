import { favoriteApi } from '@/api/favoriteApi'
import { borrowApi, reservationApi } from '@/api/borrowApi'
import type { OfflineOperation } from '@/types/offline'
import { OFFLINE_OPERATION_TYPES } from '@/constants/offlineOperations'

export async function syncOfflineOperation(operation: OfflineOperation): Promise<void> {
  const { type, payload } = operation

  if (!payload) {
    throw new Error(`Operation ${operation.id} has no payload`)
  }

  switch (type) {
    case OFFLINE_OPERATION_TYPES.FAVORITE_ADD:
      await favoriteApi.addFavorite(payload.bookId as number)
      break

    case OFFLINE_OPERATION_TYPES.FAVORITE_REMOVE:
      await favoriteApi.removeFavorite(payload.bookId as number)
      break

    case OFFLINE_OPERATION_TYPES.BORROW_APPLY:
      await borrowApi.applyBorrow({
        bookId: payload.bookId as number,
        notes: (payload.notes as string) || '',
      })
      break

    case OFFLINE_OPERATION_TYPES.BORROW_PICKUP:
      await borrowApi.pickupBorrow(payload.recordId as number)
      break

    case OFFLINE_OPERATION_TYPES.BORROW_RETURN:
      await borrowApi.returnBook(payload.recordId as number)
      break

    case OFFLINE_OPERATION_TYPES.BORROW_RENEW:
      await borrowApi.renewBorrow(payload.recordId as number)
      break

    case OFFLINE_OPERATION_TYPES.RESERVATION_RESERVE:
      await reservationApi.reserveBook({ bookId: payload.bookId as number })
      break

    case OFFLINE_OPERATION_TYPES.RESERVATION_CANCEL:
      await reservationApi.cancelReservation(
        payload.reservationId as number,
        payload.reason as string | undefined,
      )
      break

    case OFFLINE_OPERATION_TYPES.RESERVATION_PICKUP:
      await reservationApi.pickupReservation(payload.reservationId as number)
      break

    case OFFLINE_OPERATION_TYPES.RESERVATION_EXTEND:
      await reservationApi.extendReservation(payload.reservationId as number)
      break

    default:
      throw new Error(`Unknown operation type: ${type}`)
  }
}
