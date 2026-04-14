export default {
  zh: {
    myBookshelf: {
      title: '我的书架',
      description: '收藏的图书和阅读状态都在这里。按状态筛选、管理书架，随时找到想读的那本书。',
      loading: '正在加载书架…',
      tabs: {
        all: '全部',
        wantToRead: '想读',
        reading: '在读',
        read: '已读',
        favOnly: '仅收藏',
      },
      empty: {
        all: '书架空空如也，去发现你感兴趣的图书吧。',
        favOnly: '还没有只收藏但未标记状态的图书。',
        statusFiltered: '还没有标记为「{status}」的图书。',
      },
      goDiscover: '去发现好书',
      statusLabel: {
        WANT_TO_READ: '想读',
        READING: '在读',
        READ: '已读',
      },
      select: {
        label: '标记状态',
        wantToRead: '想读',
        reading: '在读',
        read: '已读',
      },
      dialog: {
        eyebrow: 'Remove',
        title: '取消收藏',
        message: '确定要将「{title}」从书架移除吗？',
        confirm: '确认移除',
        cancel: '再想想',
      },
      ariaLabel: {
        removeFav: '取消收藏',
      },
      toast: {
        statusCleared: '已清除阅读状态。',
        statusUpdated: '阅读状态已更新。',
        updateFailed: '更新失败。',
        removed: '已从书架移除。',
        removeFailed: '移除失败。',
      },
    },
  },
  en: {
    myBookshelf: {
      title: 'My Bookshelf',
      description: 'Your favorited books and reading status are all here. Filter by status, manage your shelf, and find the book you want to read.',
      loading: 'Loading bookshelf…',
      tabs: {
        all: 'All',
        wantToRead: 'Want to Read',
        reading: 'Reading',
        read: 'Read',
        favOnly: 'Favorites Only',
      },
      empty: {
        all: 'Your bookshelf is empty. Go discover books you might like.',
        favOnly: 'No books that are favorited without a reading status.',
        statusFiltered: 'No books marked as "{status}".',
      },
      goDiscover: 'Discover Books',
      statusLabel: {
        WANT_TO_READ: 'Want to Read',
        READING: 'Reading',
        READ: 'Read',
      },
      select: {
        label: 'Mark Status',
        wantToRead: 'Want to Read',
        reading: 'Reading',
        read: 'Read',
      },
      dialog: {
        eyebrow: 'Remove',
        title: 'Remove from Favorites',
        message: 'Are you sure you want to remove "{title}" from your bookshelf?',
        confirm: 'Confirm Removal',
        cancel: 'Let me think',
      },
      ariaLabel: {
        removeFav: 'Remove from favorites',
      },
      toast: {
        statusCleared: 'Reading status cleared.',
        statusUpdated: 'Reading status updated.',
        updateFailed: 'Update failed.',
        removed: 'Removed from bookshelf.',
        removeFailed: 'Failed to remove.',
      },
    },
  },
}
