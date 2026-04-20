import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

const addFavoriteMock = vi.fn()
const removeFavoriteMock = vi.fn()
const enqueueOperationMock = vi.fn()

vi.mock('@/api/favoriteApi', () => ({
  favoriteApi: {
    addFavorite: addFavoriteMock,
    removeFavorite: removeFavoriteMock,
  },
}))

describe('Offline favorite queuing', () => {
  beforeEach(() => {
    addFavoriteMock.mockReset()
    removeFavoriteMock.mockReset()
    enqueueOperationMock.mockReset()
  })

  it('queues favorite_add operation when offline', async () => {
    const isOnline = ref(false)
    const bookId = 42

    const { toggleFavoriteOfflineAware } = await import('@/composables/useFavoriteOffline')

    await toggleFavoriteOfflineAware({
      bookId,
      isFavorited: false,
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'favorite_add',
      payload: { bookId: 42 },
    })
    expect(addFavoriteMock).not.toHaveBeenCalled()
  })

  it('queues favorite_remove operation when offline', async () => {
    const isOnline = ref(false)
    const bookId = 42

    const { toggleFavoriteOfflineAware } = await import('@/composables/useFavoriteOffline')

    await toggleFavoriteOfflineAware({
      bookId,
      isFavorited: true,
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(enqueueOperationMock).toHaveBeenCalledWith({
      type: 'favorite_remove',
      payload: { bookId: 42 },
    })
    expect(removeFavoriteMock).not.toHaveBeenCalled()
  })

  it('calls API directly when online', async () => {
    const isOnline = ref(true)
    const bookId = 42

    addFavoriteMock.mockResolvedValue({ data: { success: true } })

    const { toggleFavoriteOfflineAware } = await import('@/composables/useFavoriteOffline')

    await toggleFavoriteOfflineAware({
      bookId,
      isFavorited: false,
      isOnline,
      enqueueOperation: enqueueOperationMock,
    })

    expect(addFavoriteMock).toHaveBeenCalledWith(42)
    expect(enqueueOperationMock).not.toHaveBeenCalled()
  })
})
