<template>
  <div class="my-bookshelf page-stack">
    <PageHeader
      title="我的书架"
      eyebrow="Personal Shelf"
      description="收藏的图书和阅读状态都在这里。按状态筛选、管理书架，随时找到想读的那本书。"
    >
      <template #actions>
        <div class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="{ active: activeTab === tab.key }"
            type="button"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
          </button>
        </div>
      </template>
    </PageHeader>

    <div v-if="loading" class="state-card surface-card">
      <div class="spinner"></div>
      <p>正在加载书架…</p>
    </div>

    <div v-else-if="filteredItems.length === 0" class="state-card surface-card">
      <span class="material-symbols-outlined empty-icon">auto_stories</span>
      <p class="state-title">{{ emptyMessage }}</p>
      <button class="page-action-btn page-action-btn--primary" type="button" @click="goToSearch">
        去发现好书
      </button>
    </div>

    <div v-else class="shelf-grid">
      <article
        v-for="(item, index) in filteredItems"
        :key="item.bookId"
        v-reveal="{ preset: 'card', delay: index * 0.04, once: true }"
        class="shelf-card"
        @click="goToBook(item.bookId)"
      >
        <div class="shelf-cover">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.bookTitle" @error="onImgError" />
          <div v-else class="shelf-cover-placeholder">
            <span class="material-symbols-outlined">menu_book</span>
          </div>
          <span v-if="item.readingStatus" class="status-tag" :class="`status-tag--${item.readingStatus.toLowerCase()}`">
            {{ statusLabel(item.readingStatus) }}
          </span>
          <button
            class="shelf-remove"
            type="button"
            aria-label="取消收藏"
            @click.stop="confirmRemove(item)"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="shelf-info">
          <h3>{{ item.bookTitle }}</h3>
          <p class="shelf-author">{{ item.author }}</p>
          <p v-if="item.category" class="shelf-meta">{{ item.category }}</p>
          <div class="shelf-actions" @click.stop>
            <select
              :value="item.readingStatus || ''"
              @change="updateStatus(item.bookId, ($event.target as HTMLSelectElement).value)"
            >
              <option value="">标记状态</option>
              <option value="WANT_TO_READ">想读</option>
              <option value="READING">在读</option>
              <option value="READ">已读</option>
            </select>
          </div>
          <p v-if="item.notes" class="shelf-notes">{{ item.notes }}</p>
        </div>
      </article>
    </div>

    <ConfirmDialog
      :open="removeDialog.open"
      eyebrow="Remove"
      title="取消收藏"
      :message="`确定要将「${removeDialog.bookTitle}」从书架移除吗？`"
      confirm-text="确认移除"
      cancel-text="再想想"
      @confirm="doRemove"
      @cancel="removeDialog.open = false"
    />

    <FeedbackToast :message="toast.message" :type="toast.type" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi, type FavoriteInfo } from '../api/favoriteApi'
import { readingStatusApi, type ReadingStatusEnum, type ReadingStatusInfo } from '../api/favoriteApi'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'
import FeedbackToast from '../components/common/FeedbackToast.vue'
import PageHeader from '../components/layout/PageHeader.vue'
import { handleImageError } from '../utils/imageHelpers'

const router = useRouter()

interface ShelfItem {
  bookId: number
  bookTitle: string
  author: string
  coverUrl: string | null
  category: string | null
  availableCopies: number | null
  readingStatus: string | null
  notes: string | null
  favoritedAt: string
}

const loading = ref(true)
const shelfItems = ref<ShelfItem[]>([])
const statusCounts = reactive<Record<string, number>>({
  WANT_TO_READ: 0,
  READING: 0,
  READ: 0,
})
const activeTab = ref('all')
const toast = reactive<{ message: string; type: 'success' | 'error' | 'info' }>({
  message: '',
  type: 'info',
})

const removeDialog = reactive({
  open: false,
  bookId: 0,
  bookTitle: '',
})

const tabs = computed(() => [
  { key: 'all', label: '全部', count: shelfItems.value.length },
  { key: 'WANT_TO_READ', label: '想读', count: statusCounts.WANT_TO_READ },
  { key: 'READING', label: '在读', count: statusCounts.READING },
  { key: 'READ', label: '已读', count: statusCounts.READ },
  { key: 'fav_only', label: '仅收藏', count: shelfItems.value.filter(i => !i.readingStatus).length },
])

const filteredItems = computed(() => {
  if (activeTab.value === 'all') return shelfItems.value
  if (activeTab.value === 'fav_only') return shelfItems.value.filter(i => !i.readingStatus)
  return shelfItems.value.filter(i => i.readingStatus === activeTab.value)
})

const emptyMessage = computed(() => {
  if (activeTab.value === 'all') return '书架空空如也，去发现你感兴趣的图书吧。'
  if (activeTab.value === 'fav_only') return '还没有只收藏但未标记状态的图书。'
  return `还没有标记为「${statusLabel(activeTab.value)}」的图书。`
})

onMounted(() => {
  void loadShelf()
})

async function loadShelf() {
  loading.value = true
  try {
    const [favRes, statusRes] = await Promise.allSettled([
      favoriteApi.getFavorites(),
      readingStatusApi.getReadingStatuses(),
    ])

    const favorites: FavoriteInfo[] =
      favRes.status === 'fulfilled' && favRes.value.data.success ? favRes.value.data.data : []
    const statuses: ReadingStatusInfo[] =
      statusRes.status === 'fulfilled' && statusRes.value.data.success ? statusRes.value.data.data : []

    const statusMap = new Map<number, ReadingStatusInfo>()
    statuses.forEach(s => statusMap.set(s.bookId, s))

    // 合并收藏列表和阅读状态
    const items: ShelfItem[] = favorites.map(fav => {
      const status = statusMap.get(fav.bookId)
      return {
        bookId: fav.bookId,
        bookTitle: fav.bookTitle,
        author: fav.author,
        coverUrl: fav.coverUrl,
        category: fav.category,
        availableCopies: fav.availableCopies,
        readingStatus: status?.status || null,
        notes: status?.notes || null,
        favoritedAt: fav.favoritedAt,
      }
    })

    // 加入有阅读状态但未收藏的书
    const favBookIds = new Set(favorites.map(f => f.bookId))
    statuses.forEach(s => {
      if (!favBookIds.has(s.bookId)) {
        items.push({
          bookId: s.bookId,
          bookTitle: s.bookTitle,
          author: s.author,
          coverUrl: s.coverUrl,
          category: s.category,
          availableCopies: null,
          readingStatus: s.status,
          notes: s.notes,
          favoritedAt: s.createdAt,
        })
      }
    })

    shelfItems.value = items

    // 计算状态计数
    let want = 0, reading = 0, read = 0
    items.forEach(i => {
      if (i.readingStatus === 'WANT_TO_READ') want++
      else if (i.readingStatus === 'READING') reading++
      else if (i.readingStatus === 'READ') read++
    })
    statusCounts.WANT_TO_READ = want
    statusCounts.READING = reading
    statusCounts.READ = read
  } catch {
    shelfItems.value = []
  } finally {
    loading.value = false
  }
}

async function updateStatus(bookId: number, status: string) {
  try {
    if (!status) {
      await readingStatusApi.removeReadingStatus(bookId)
      showToast('已清除阅读状态。', 'info')
    } else {
      await readingStatusApi.upsertReadingStatus(bookId, status as ReadingStatusEnum)
      showToast('阅读状态已更新。', 'success')
    }
    await loadShelf()
  } catch (error: any) {
    showToast(error.response?.data?.message || '更新失败。', 'error')
  }
}

function confirmRemove(item: ShelfItem) {
  removeDialog.bookId = item.bookId
  removeDialog.bookTitle = item.bookTitle
  removeDialog.open = true
}

async function doRemove() {
  removeDialog.open = false
  try {
    await favoriteApi.removeFavorite(removeDialog.bookId)
    showToast('已从书架移除。', 'info')
    await loadShelf()
  } catch (error: any) {
    showToast(error.response?.data?.message || '移除失败。', 'error')
  }
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    WANT_TO_READ: '想读',
    READING: '在读',
    READ: '已读',
  }
  return map[status] || status
}

function goToBook(bookId: number) {
  router.push({ name: 'BookDetail', params: { id: bookId } }).catch(() => {})
}

function goToSearch() {
  router.push({ name: 'BookSearch' }).catch(() => {})
}

const onImgError = (event: Event) => handleImageError(event, '/logo-photo.jpg')

function showToast(message: string, type: 'success' | 'error' | 'info') {
  toast.message = message
  toast.type = type
  window.setTimeout(() => {
    toast.message = ''
  }, 2600)
}
</script>

<style scoped>
.my-bookshelf {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.tabs {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.tabs button {
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  border: 1px solid rgba(133, 122, 107, 0.15);
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--on-surface-variant);
  transition: all 0.2s ease;
}

.tabs button.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.tabs button:hover:not(.active) {
  background: rgba(0, 83, 219, 0.06);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.2rem;
  height: 1.2rem;
  margin-left: 0.3rem;
  padding: 0 0.3rem;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  font-size: 0.72rem;
  font-weight: 700;
}

.tabs button.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
}

.state-card {
  min-height: 14rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid rgba(0, 83, 219, 0.16);
  border-top-color: var(--primary);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 2.5rem;
  color: rgba(133, 122, 107, 0.3);
}

.state-title {
  font-family: var(--font-headline);
  font-size: 1.1rem;
}

.shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-5);
}

.shelf-card {
  display: flex;
  flex-direction: column;
  border-radius: 1.35rem;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(133, 122, 107, 0.1);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.shelf-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.shelf-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  background: rgba(228, 230, 225, 0.85);
  overflow: hidden;
}

.shelf-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shelf-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(18, 59, 93, 0.4);
}

.status-tag {
  position: absolute;
  top: 0.6rem;
  left: 0.6rem;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  color: white;
}

.status-tag--want_to_read {
  background: #3182ce;
}

.status-tag--reading {
  background: #c05621;
}

.status-tag--read {
  background: #2f855a;
}

.shelf-remove {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s ease;
}

.shelf-card:hover .shelf-remove {
  opacity: 1;
}

.shelf-remove:hover {
  background: rgba(229, 62, 62, 0.12);
  color: #e53e3e;
}

.shelf-remove .material-symbols-outlined {
  font-size: 1rem;
}

.shelf-info {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.shelf-info h3 {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelf-author {
  margin: 0;
  font-size: 0.82rem;
  color: var(--on-surface-variant);
}

.shelf-meta {
  margin: 0;
  font-size: 0.76rem;
  color: var(--outline);
}

.shelf-actions {
  margin-top: 0.3rem;
}

.shelf-actions select {
  width: 100%;
  padding: 0.4rem 0.6rem;
  border-radius: 0.6rem;
  border: 1px solid rgba(133, 122, 107, 0.18);
  background: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  color: var(--on-surface);
  cursor: pointer;
  outline: none;
}

.shelf-actions select:focus {
  border-color: var(--primary);
}

.shelf-notes {
  margin: 0;
  margin-top: 0.25rem;
  font-size: 0.78rem;
  color: var(--on-surface-variant);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 767px) {
  .shelf-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
