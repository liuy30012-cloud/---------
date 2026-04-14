export default {
  zh: {
    inventoryAlerts: {
      title: '库存预警',
      description: '集中查看缺货、低库存与高需求书目，帮助馆员优先处理最值得介入的馆藏问题。',
      refresh: '刷新数据',
      loading: '加载中...',
      summary: {
        totalAlerts: '总预警数',
        criticalAlerts: '严重预警',
        warningAlerts: '一般预警',
        outOfStock: '缺货书籍',
        lowStock: '低库存',
        highDemand: '高需求',
      },
      cardIcons: {
        total: '总',
        critical: '严',
        warning: '警',
        outOfStock: '缺',
        lowStock: '低',
        highDemand: '热',
      },
      alertBadges: {
        CRITICAL: '严重',
        WARNING: '警告',
      },
      bookInfo: {
        author: '作者:',
        category: '分类:',
        isbn: 'ISBN:',
      },
      alertStats: {
        totalCopies: '总册数',
        available: '可借',
        borrowed: '已借',
        borrowCount: '借阅次数',
      },
      alertType: {
        OUT_OF_STOCK: '缺货',
        LOW_STOCK: '低库存',
        HIGH_DEMAND: '高需求',
      },
      coverPlaceholder: '书',
      loadError: '加载预警数据失败',
      empty: {
        icon: '稳',
        text: '暂无库存预警',
      },
    },
  },
  en: {
    inventoryAlerts: {
      title: 'Inventory Alerts',
      description: 'Centrally view out-of-stock, low-stock, and high-demand titles to help librarians prioritize collection issues.',
      refresh: 'Refresh Data',
      loading: 'Loading...',
      summary: {
        totalAlerts: 'Total Alerts',
        criticalAlerts: 'Critical',
        warningAlerts: 'Warnings',
        outOfStock: 'Out of Stock',
        lowStock: 'Low Stock',
        highDemand: 'High Demand',
      },
      cardIcons: {
        total: 'All',
        critical: 'Crit',
        warning: 'Warn',
        outOfStock: 'Out',
        lowStock: 'Low',
        highDemand: 'Hot',
      },
      alertBadges: {
        CRITICAL: 'Critical',
        WARNING: 'Warning',
      },
      bookInfo: {
        author: 'Author:',
        category: 'Category:',
        isbn: 'ISBN:',
      },
      alertStats: {
        totalCopies: 'Total Copies',
        available: 'Available',
        borrowed: 'Borrowed',
        borrowCount: 'Borrows',
      },
      alertType: {
        OUT_OF_STOCK: 'Out of Stock',
        LOW_STOCK: 'Low Stock',
        HIGH_DEMAND: 'High Demand',
      },
      coverPlaceholder: 'Book',
      loadError: 'Failed to load inventory alerts',
      empty: {
        icon: 'OK',
        text: 'No inventory alerts',
      },
    },
  },
}
