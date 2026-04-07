/**
 * IndexedDB 离线数据库封装
 * 用于存储书籍数据、缓存元数据和搜索缓存
 */

const DB_NAME = 'LibraryOfflineDB';
const DB_VERSION = 1;

// 对象存储名称
const STORES = {
  BOOKS: 'books',
  METADATA: 'cache_metadata',
  SEARCH: 'search_cache'
};

export interface BookCache {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  description: string;
  coverImage?: string;
  location?: {
    floor: string;
    shelf: string;
    layer: string;
  };
  cachedAt: number;
  viewCount: number;
  isHotBook: boolean;
}

export interface CacheMetadata {
  key: string;
  value: any;
  updatedAt: number;
}

export interface SearchCache {
  query: string;
  results: number[];
  cachedAt: number;
  expiresAt: number;
}

class OfflineDB {
  private db: IDBDatabase | null = null;

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;

        // 创建 books 存储
        if (!db.objectStoreNames.contains(STORES.BOOKS)) {
          const bookStore = db.createObjectStore(STORES.BOOKS, { keyPath: 'id' });
          bookStore.createIndex('isHotBook', 'isHotBook', { unique: false });
          bookStore.createIndex('cachedAt', 'cachedAt', { unique: false });
          bookStore.createIndex('category', 'category', { unique: false });
        }

        // 创建 metadata 存储
        if (!db.objectStoreNames.contains(STORES.METADATA)) {
          db.createObjectStore(STORES.METADATA, { keyPath: 'key' });
        }

        // 创建 search_cache 存储
        if (!db.objectStoreNames.contains(STORES.SEARCH)) {
          const searchStore = db.createObjectStore(STORES.SEARCH, { keyPath: 'query' });
          searchStore.createIndex('expiresAt', 'expiresAt', { unique: false });
        }
      };
    });
  }

  // 书籍操作
  async saveBook(book: BookCache): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readwrite');
      const store = transaction.objectStore(STORES.BOOKS);
      const request = store.put(book);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async saveBooks(books: BookCache[]): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readwrite');
      const store = transaction.objectStore(STORES.BOOKS);

      books.forEach(book => store.put(book));

      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
  }

  async getBook(id: number): Promise<BookCache | null> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readonly');
      const store = transaction.objectStore(STORES.BOOKS);
      const request = store.get(id);
      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error);
    });
  }

  async getHotBooks(): Promise<BookCache[]> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readonly');
      const store = transaction.objectStore(STORES.BOOKS);
      const request = store.getAll();
      request.onsuccess = () => {
        const books = (request.result || []) as BookCache[];
        resolve(books.filter((book) => Boolean(book.isHotBook)));
      };
      request.onerror = () => reject(request.error);
    });
  }

  async searchBooks(query: string): Promise<BookCache[]> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readonly');
      const store = transaction.objectStore(STORES.BOOKS);
      const request = store.getAll();

      request.onsuccess = () => {
        const books = request.result || [];
        const lowerQuery = query.toLowerCase();
        const filtered = books.filter(book =>
          book.title.toLowerCase().includes(lowerQuery) ||
          book.author.toLowerCase().includes(lowerQuery) ||
          book.isbn.includes(query)
        );
        resolve(filtered);
      };
      request.onerror = () => reject(request.error);
    });
  }

  // 元数据操作
  async setMetadata(key: string, value: any): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.METADATA], 'readwrite');
      const store = transaction.objectStore(STORES.METADATA);
      const request = store.put({ key, value, updatedAt: Date.now() });
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async getMetadata(key: string): Promise<any> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.METADATA], 'readonly');
      const store = transaction.objectStore(STORES.METADATA);
      const request = store.get(key);
      request.onsuccess = () => resolve(request.result?.value || null);
      request.onerror = () => reject(request.error);
    });
  }

  // 搜索缓存操作
  async saveSearchCache(query: string, results: number[], ttl: number = 7 * 24 * 60 * 60 * 1000): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.SEARCH], 'readwrite');
      const store = transaction.objectStore(STORES.SEARCH);
      const cache: SearchCache = {
        query,
        results,
        cachedAt: Date.now(),
        expiresAt: Date.now() + ttl
      };
      const request = store.put(cache);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async getSearchCache(query: string): Promise<number[] | null> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.SEARCH], 'readonly');
      const store = transaction.objectStore(STORES.SEARCH);
      const request = store.get(query);

      request.onsuccess = () => {
        const cache = request.result;
        if (!cache || cache.expiresAt < Date.now()) {
          resolve(null);
        } else {
          resolve(cache.results);
        }
      };
      request.onerror = () => reject(request.error);
    });
  }

  // 清理过期数据
  async cleanExpiredCache(): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.SEARCH], 'readwrite');
      const store = transaction.objectStore(STORES.SEARCH);
      const index = store.index('expiresAt');
      const request = index.openCursor();

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result;
        if (cursor) {
          if (cursor.value.expiresAt < Date.now()) {
            cursor.delete();
          }
          cursor.continue();
        }
      };

      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
  }

  // 获取缓存统计
  async getCacheStats(): Promise<{ bookCount: number; hotBookCount: number; cacheSize: number }> {
    if (!this.db) await this.init();

    const bookCount = await new Promise<number>((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS], 'readonly');
      const store = transaction.objectStore(STORES.BOOKS);
      const request = store.count();
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    const hotBooks = await this.getHotBooks();

    return {
      bookCount,
      hotBookCount: hotBooks.length,
      cacheSize: 0 // 浏览器不提供精确的存储大小
    };
  }

  // 清空所有缓存
  async clearAllCache(): Promise<void> {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([STORES.BOOKS, STORES.METADATA, STORES.SEARCH], 'readwrite');

      transaction.objectStore(STORES.BOOKS).clear();
      transaction.objectStore(STORES.METADATA).clear();
      transaction.objectStore(STORES.SEARCH).clear();

      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
  }
}

export const offlineDB = new OfflineDB();
