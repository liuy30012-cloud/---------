import type { Ref } from 'vue'
import { favoriteApi } from '@/api/favoriteApi'
import type { QueueOperationInput } from '@/types/offline'
import { OFFLINE_OPERATION_TYPES } from '@/constants/offlineOperations'

interface ToggleFavoriteOfflineParams {
  bookId: number
  isFavorited: boolean
  isOnline: Ref<boolean>
  enqueueOperation: (input: QueueOperationInput) => Promise<unknown>
}

export async function toggleFavoriteOfflineAware(params: ToggleFavoriteOfflineParams): Promise<void> {
  const { bookId, isFavorited, isOnline, enqueueOperation } = params

  if (!isOnline.value) {
    await enqueueOperation({
      type: isFavorited ? OFFLINE_OPERATION_TYPES.FAVORITE_REMOVE : OFFLINE_OPERATION_TYPES.FAVORITE_ADD,
      payload: { bookId },
    })
    return
  }

  if (isFavorited) {
    await favoriteApi.removeFavorite(bookId)
  } else {
    await favoriteApi.addFavorite(bookId)
  }
}
