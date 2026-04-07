<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2>快速续借</h2>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="renewableBooks.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"></path>
          </svg>
          <p>暂无可续借书籍</p>
          <small>当前没有符合续借条件的借阅记录。</small>
        </div>

        <div v-else class="books-list">
          <p v-if="message" :class="['inline-message', `inline-message--${messageTone}`]">{{ message }}</p>

          <div
            v-for="book in renewableBooks"
            :key="book.id"
            class="book-item"
            :class="{ renewing: renewingBooks.has(book.id) }"
          >
            <div class="book-info">
              <h4>{{ book.bookTitle }}</h4>
              <p class="author">{{ book.bookIsbn || '暂无 ISBN' }}</p>
              <p class="due-date">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                归还日期: {{ formatDate(book.dueDate) }}
              </p>
              <p class="renewal-count">
                <span class="badge">可续借</span>
              </p>
            </div>
            <button
              class="renew-btn"
              :disabled="renewingBooks.has(book.id)"
              @click="renewBook(book.id)"
            >
              <span v-if="!renewingBooks.has(book.id)">续借</span>
              <span v-else>
                <svg class="spinner-small" viewBox="0 0 50 50">
                  <circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="3" stroke-dasharray="31.4 94.2" />
                </svg>
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <p class="info-text">每本书最多可续借一次，续借后会自动刷新本列表。</p>
        <button class="btn-close" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { borrowApi, type BorrowRecord } from '@/api/borrowApi'

defineEmits<{
  (e: 'close'): void
}>()

const loading = ref(true)
const renewableBooks = ref<BorrowRecord[]>([])
const renewingBooks = ref<Set<number>>(new Set())
const message = ref('')
const messageTone = ref<'success' | 'error'>('success')

onMounted(async () => {
  await loadRenewableBooks()
})

async function loadRenewableBooks() {
  try {
    loading.value = true
    const response = await borrowApi.getCurrentBorrows()
    const records = Array.isArray(response.data?.data) ? (response.data.data as BorrowRecord[]) : []
    renewableBooks.value = records.filter((record) => (
      record.status === 'BORROWED' &&
      record.renewCount < 1 &&
      Boolean(record.dueDate)
    ))
  } catch (error) {
    console.error('加载续借列表失败:', error)
    showMessage('加载续借列表失败', 'error')
  } finally {
    loading.value = false
  }
}

async function renewBook(recordId: number) {
  const nextSet = new Set(renewingBooks.value)
  nextSet.add(recordId)
  renewingBooks.value = nextSet

  try {
    await borrowApi.renewBorrow(recordId)
    showMessage('续借成功', 'success')
    renewableBooks.value = renewableBooks.value.filter((book) => book.id !== recordId)
  } catch (error) {
    console.error('续借失败:', error)
    showMessage('续借失败，请稍后重试', 'error')
  } finally {
    const updatedSet = new Set(renewingBooks.value)
    updatedSet.delete(recordId)
    renewingBooks.value = updatedSet
  }
}

function formatDate(dateString: string | null) {
  if (!dateString) {
    return '-'
  }
  const date = new Date(dateString)
  return Number.isNaN(date.getTime()) ? dateString : date.toLocaleDateString('zh-CN')
}

function showMessage(text: string, tone: 'success' | 'error') {
  message.value = text
  messageTone.value = tone
  window.setTimeout(() => {
    if (message.value === text) {
      message.value = ''
    }
  }, 2500)
}
</script>

<style scoped lang="scss">
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.loading,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  gap: 12px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f0f0f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.empty-state {
  color: #999;
}

.empty-state svg {
  width: 60px;
  height: 60px;
  opacity: 0.3;
}

.books-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inline-message {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.inline-message--success {
  background: #e6f8ee;
  color: #1f7a4d;
}

.inline-message--error {
  background: #fdebec;
  color: #a63d40;
}

.book-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9f9f9;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.book-item.renewing {
  opacity: 0.65;
}

.book-info {
  flex: 1;
}

.book-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.author,
.due-date,
.renewal-count {
  margin: 6px 0 0;
  font-size: 12px;
}

.author {
  color: #999;
}

.due-date {
  color: #e74c3c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  background: #27ae60;
  color: white;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.renew-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  min-width: 64px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.renew-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner-small {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.info-text {
  margin: 0;
  font-size: 12px;
  color: #999;
  flex: 1;
}

.btn-close {
  padding: 8px 24px;
  background: white;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
