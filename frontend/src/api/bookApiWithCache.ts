/**
 * 书籍 API 离线缓存增强版
 * 在原有 API 基础上添加 IndexedDB 缓存支持
 */

import { bookApi, Book, BookDetail, BookSearchParams, PaginatedApiResponse } from './bookApi';
import { offlineDB, BookCache } from '@/utils/offlineDB';

/**
 * 将 API 返回的书籍数据转换为缓存格式
 */
function bookToCache(book: Book | BookDetail, isHotBook = false): BookCache {
  return {
    id: book.id,
    title: book.title,
    author: book.author,
    isbn: book.isbn,
    category: book.category,
    description: book.description,
    coverImage: book.coverUrl,
    location: book.location ? {
      floor: book.location.split('-')[0] || '',
      shelf: book.location.split('-')[1] || '',
      layer: book.location.split('-')[2] || ''
    } : undefined,
    cachedAt: Date.now(),
    viewCount: 0,
    isHotBook
  };
}

/**
 * 将缓存数据转换为 API 格式
 */
function cacheToBook(cache: BookCache): Book {
  return {
    id: cache.id,
    title: cache.title,
    author: cache.author,
    isbn: cache.isbn,
    location: cache.location
      ? `${cache.location.floor}-${cache.location.shelf}-${cache.location.layer}`
      : '',
    coverUrl: cache.coverImage || '',
    status: 'AVAILABLE',
    year: '',
    description: cache.description,
    languageCode: 'zh',
    availability: 'AVAILABLE',
    category: cache.category,
    totalCopies: 0,
    availableCopies: 0,
    borrowedCount: 0,
    circulationPolicy: 'AUTO'
  };
}

export const bookApiWithCache = {
  /**
   * 搜索书籍 - 优先使用网络,离线时搜索缓存
   */
  async searchBooks(params: BookSearchParams): Promise<PaginatedApiResponse<Book[]>> {
    try {
      // 尝试网络请求
      const response = await bookApi.searchBooks(params);

      // 缓存搜索结果
      if (response.data.success && response.data.data) {
        const books = response.data.data;
        const cacheBooks = books.map(book => bookToCache(book, false));
        await offlineDB.saveBooks(cacheBooks);
      }

      return response.data;
    } catch (error: any) {
      // 网络失败,尝试从缓存搜索
      if (error.message?.includes('offline') || !navigator.onLine) {
        console.log('[BookAPI] 离线模式,从缓存搜索');

        const keyword = params.keyword || '';
        const cachedBooks = await offlineDB.searchBooks(keyword);

        return {
          success: true,
          data: cachedBooks.map(cacheToBook),
          message: '离线模式:显示已缓存的书籍',
          total: cachedBooks.length,
          page: 0,
          size: cachedBooks.length,
          totalPages: 1
        };
      }

      throw error;
    }
  },

  /**
   * 获取书籍详情 - 优先使用网络,离线时返回缓存
   */
  async getBookDetail(id: number): Promise<{ success: boolean; data: BookDetail; message?: string }> {
    try {
      // 尝试网络请求
      const response = await bookApi.getBookDetail(id);

      // 缓存书籍详情
      if (response.data.success && response.data.data) {
        const book = response.data.data;
        const cacheBook = bookToCache(book, false);
        await offlineDB.saveBook(cacheBook);

        // 更新查看次数
        const existing = await offlineDB.getBook(id);
        if (existing) {
          cacheBook.viewCount = (existing.viewCount || 0) + 1;
          await offlineDB.saveBook(cacheBook);
        }
      }

      return response.data;
    } catch (error: any) {
      // 网络失败,尝试从缓存获取
      if (error.message?.includes('offline') || !navigator.onLine) {
        console.log('[BookAPI] 离线模式,从缓存获取书籍详情');

        const cachedBook = await offlineDB.getBook(id);

        if (cachedBook) {
          // 构造基本的 BookDetail 对象
          const bookDetail: BookDetail = {
            ...cacheToBook(cachedBook),
            averageRating: 0,
            totalReviews: 0,
            latestReviews: [],
            borrowHistorySummary: {
              totalBorrows: 0,
              activeBorrowCount: 0,
              lastBorrowedAt: null,
              recentActivity: []
            },
            relatedBooks: [],
            availabilityContext: {
              canBorrow: false,
              canReserve: false,
              state: 'OFFLINE',
              summary: '离线模式:无法获取实时库存信息',
              nextAction: '请连接网络后查看',
              availableCopies: 0,
              totalCopies: 0
            },
            queueContext: {
              waitingCount: 0,
              availableReservationCount: 0,
              estimatedWaitDays: 0,
              summary: '离线模式'
            },
            locationContext: {
              breadcrumbs: cachedBook.location
                ? [cachedBook.location.floor, cachedBook.location.shelf, cachedBook.location.layer]
                : [],
              pickupCardTitle: '书籍位置',
              pickupHint: '请连接网络获取最新位置信息'
            }
          };

          return {
            success: true,
            data: bookDetail,
            message: '离线模式:显示缓存的书籍信息'
          };
        }

        throw new Error('书籍未缓存,请连接网络后重试');
      }

      throw error;
    }
  },

  /**
   * 获取热门书籍 - 用于预缓存
   */
  async getHotBooks(): Promise<Book[]> {
    try {
      // 这里假设后端有热门书籍接口,如果没有可以用搜索接口代替
      const response = await bookApi.searchBooks({
        sort: 'borrowedCount,desc',
        size: 100
      });

      if (response.data.success && response.data.data) {
        const books = response.data.data;

        // 标记为热门书籍并缓存
        const cacheBooks = books.map(book => bookToCache(book, true));
        await offlineDB.saveBooks(cacheBooks);

        // 更新元数据
        await offlineDB.setMetadata('hot_books_last_update', Date.now());

        return books;
      }

      return [];
    } catch (error) {
      console.error('[BookAPI] 获取热门书籍失败:', error);
      return [];
    }
  },

  /**
   * 其他 API 方法保持不变
   */
  getBookReviews: bookApi.getBookReviews,
  addReview: bookApi.addReview,
  getCategories: bookApi.getCategories,
  getLanguages: bookApi.getLanguages
};
