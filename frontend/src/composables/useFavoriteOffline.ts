import type { Ref } from 'vue'
import { favoriteApi } from '@/api/favoriteApi'
import type { QueueOperationInput } from '@/types/offline'

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
      type: isFavorited ? 'favorite_remove' : 'favorite_add',
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
