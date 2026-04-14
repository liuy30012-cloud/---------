export default {
  zh: {
    cacheManagement: {
      title: '离线数据管理',
      description: '管理离线缓存的书籍数据,提升弱网环境下的使用体验',
      stats: {
        cachedBooks: '已缓存书籍',
        hotBooks: '热门书籍',
        lastUpdate: '最后更新',
      },
      buttons: {
        updateNow: '立即更新缓存',
        updating: '更新中...',
        clearAll: '清空所有缓存',
        clearing: '清空中...',
      },
      info: {
        title: '离线功能说明',
        item1: '系统会自动缓存您浏览过的书籍信息',
        item2: '热门书籍会在联网时自动更新',
        item3: '离线状态下可以查看已缓存的书籍详情',
        item4: '借阅、预约等操作需要网络连接',
      },
      time: {
        never: '从未更新',
        justNow: '刚刚',
        minutesAgo: '{minutes}分钟前',
        hoursAgo: '{hours}小时前',
        daysAgo: '{days}天前',
      },
      confirm: '确定要清空所有缓存吗?此操作不可恢复。',
      toast: {
        networkRequired: '需要网络连接才能更新缓存',
        updateSuccess: '缓存更新成功',
        updateFailed: '缓存更新失败,请稍后重试',
        cleared: '缓存已清空',
        clearFailed: '清空缓存失败',
      },
    },
  },
  en: {
    cacheManagement: {
      title: 'Offline Data Management',
      description: 'Manage offline cached book data to improve the experience under weak network conditions.',
      stats: {
        cachedBooks: 'Cached Books',
        hotBooks: 'Popular Books',
        lastUpdate: 'Last Update',
      },
      buttons: {
        updateNow: 'Update Cache Now',
        updating: 'Updating...',
        clearAll: 'Clear All Cache',
        clearing: 'Clearing...',
      },
      info: {
        title: 'Offline Features',
        item1: 'The system automatically caches book information you have viewed.',
        item2: 'Popular books are automatically updated when online.',
        item3: 'Cached book details can be viewed offline.',
        item4: 'Borrowing, reserving, and other operations require a network connection.',
      },
      time: {
        never: 'Never updated',
        justNow: 'Just now',
        minutesAgo: '{minutes} minutes ago',
        hoursAgo: '{hours} hours ago',
        daysAgo: '{days} days ago',
      },
      confirm: 'Are you sure you want to clear all cache? This action cannot be undone.',
      toast: {
        networkRequired: 'Network connection required to update cache',
        updateSuccess: 'Cache updated successfully',
        updateFailed: 'Cache update failed. Please try again later.',
        cleared: 'Cache cleared',
        clearFailed: 'Failed to clear cache',
      },
    },
  },
}
