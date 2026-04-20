import { favoriteApi } from '@/api/favoriteApi'
import { borrowApi, reservationApi } from '@/api/borrowApi'
import type { OfflineOperation } from '@/types/offline'

export async function syncOfflineOperation(operation: OfflineOperation): Promise<void> {
  const { type, payload } = operation

  if (!payload) {
    throw new Error(`Operation ${operation.id} has no payload`)
  }

  switch (type) {
    case 'favorite_add':
      await favoriteApi.addFavorite(payload.bookId as number)
      break

    case 'favorite_remove':
      await favoriteApi.removeFavorite(payload.bookId as number)
      break

    case 'borrow_apply':
      await borrowApi.applyBorrow({
        bookId: payload.bookId as number,
        notes: (payload.notes as string) || '',
      })
      break

    case 'borrow_pickup':
      await borrowApi.pickupBorrow(payload.recordId as number)
      break

    case 'borrow_return':
      await borrowApi.returnBook(payload.recordId as number)
      break

    case 'borrow_renew':
      await borrowApi.renewBorrow(payload.recordId as number)
      break

    case 'reservation_create':
      await reservationApi.reserveBook({ bookId: payload.bookId as number })
      break

    case 'reservation_cancel':
      await reservationApi.cancelReservation(
        payload.reservationId as number,
        payload.reason as string | undefined,
      )
      break

    case 'reservation_pickup':
      await reservationApi.pickupReservation(payload.reservationId as number)
      break

    case 'reservation_extend':
      await reservationApi.extendReservation(payload.reservationId as number)
      break

    default:
      throw new Error(`Unknown operation type: ${type}`)
  }
}
