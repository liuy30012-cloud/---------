export const OFFLINE_OPERATION_TYPES = {
  FAVORITE_ADD: 'favorite_add',
  FAVORITE_REMOVE: 'favorite_remove',
  BORROW_APPLY: 'borrow_apply',
  BORROW_PICKUP: 'borrow_pickup',
  BORROW_RETURN: 'borrow_return',
  BORROW_RENEW: 'borrow_renew',
  RESERVATION_RESERVE: 'reservation_create',
  RESERVATION_CANCEL: 'reservation_cancel',
  RESERVATION_PICKUP: 'reservation_pickup',
  RESERVATION_EXTEND: 'reservation_extend',
} as const

export type OfflineOperationType = typeof OFFLINE_OPERATION_TYPES[keyof typeof OFFLINE_OPERATION_TYPES]
