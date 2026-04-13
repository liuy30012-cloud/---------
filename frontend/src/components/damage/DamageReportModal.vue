<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="report-modal" @click.stop>
      <div class="modal-header">
        <h2>报告书籍问题</h2>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <p class="book-context">《{{ bookTitle }}》</p>

        <!-- 损坏类型 -->
        <div class="form-group">
          <label class="form-label">损坏类型 *</label>
          <div class="type-tags">
            <button
              v-for="t in damageTypeOptions"
              :key="t.value"
              class="type-tag"
              :class="{ active: selectedTypes.includes(t.value) }"
              @click="toggleType(t.value)"
              type="button"
            >
              {{ t.label }}
            </button>
          </div>
        </div>

        <!-- 照片上传 -->
        <div class="form-group">
          <label class="form-label">破损照片 *（最多3张）</label>
          <div class="photo-grid">
            <div v-for="(file, idx) in photoPreviews" :key="idx" class="photo-item">
              <img :src="file.preview" alt="破损照片" />
              <button class="photo-remove" @click="removePhoto(idx)" type="button">✕</button>
            </div>
            <label v-if="photoFiles.length < 3" class="photo-add">
              <input type="file" accept="image/jpeg,image/png,image/webp" multiple @change="onPhotoChange" />
              <span class="add-icon">+</span>
            </label>
          </div>
          <p v-if="photoError" class="field-error">{{ photoError }}</p>
        </div>

        <!-- 描述 -->
        <div class="form-group">
          <label class="form-label">问题描述</label>
          <textarea
            v-model="description"
            class="form-textarea"
            placeholder="请描述损坏情况..."
            maxlength="500"
            rows="3"
          ></textarea>
        </div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <button class="btn-cancel" @click="$emit('close')" type="button">取消</button>
          <button
            class="btn-submit"
            :disabled="submitting || selectedTypes.length === 0 || photoFiles.length === 0"
            @click="submitReport"
            type="button"
          >
            {{ submitting ? '提交中...' : '提交报告' }}
          </button>
        </div>

        <p v-if="errorMessage" class="field-error">{{ errorMessage }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { damageReportApi } from '../../api/damageReportApi'
import { API_CONFIG } from '../../config'

const props = defineProps<{
  bookId: number
  bookTitle: string
}>()

const emit = defineEmits<{
  close: []
  submitted: []
}>()

const damageTypeOptions = [
  { value: 'COVER_TORN', label: '封面破损' },
  { value: 'PAGE_MISSING', label: '页面缺失' },
  { value: 'WATER_DAMAGE', label: '水渍' },
  { value: 'GRAFFITI', label: '涂写' },
  { value: 'BINDING_BROKEN', label: '装订脱落' },
  { value: 'OTHER', label: '其他' },
]

const selectedTypes = ref<string[]>([])
const photoFiles = ref<File[]>([])
const photoPreviews = ref<{ preview: string; file: File }[]>([])
const description = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const photoError = ref('')

function toggleType(type: string) {
  const idx = selectedTypes.value.indexOf(type)
  if (idx >= 0) {
    selectedTypes.value.splice(idx, 1)
  } else {
    selectedTypes.value.push(type)
  }
}

function onPhotoChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  photoError.value = ''
  const newFiles = Array.from(input.files)
  const remaining = 3 - photoFiles.value.length

  if (newFiles.length > remaining) {
    photoError.value = `最多上传3张照片，还可添加${remaining}张`
    return
  }

  for (const file of newFiles) {
    if (file.size > 5 * 1024 * 1024) {
      photoError.value = '单张照片不能超过 5MB'
      return
    }
    photoFiles.value.push(file)
    photoPreviews.value.push({ preview: URL.createObjectURL(file), file })
  }

  input.value = ''
}

function removePhoto(idx: number) {
  URL.revokeObjectURL(photoPreviews.value[idx].preview)
  photoFiles.value.splice(idx, 1)
  photoPreviews.value.splice(idx, 1)
}

async function submitReport() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('bookId', String(props.bookId))
    formData.append('damageTypes', selectedTypes.value.join(','))
    if (description.value.trim()) {
      formData.append('description', description.value.trim())
    }
    for (const file of photoFiles.value) {
      formData.append('photos', file)
    }

    const res = await damageReportApi.submitReport(formData)
    if (res.data.success) {
      emit('submitted')
      emit('close')
    } else {
      errorMessage.value = res.data.message || '提交失败，请重试'
    }
  } catch (err: any) {
    errorMessage.value = err?.response?.data?.message || '网络错误，请重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.report-modal {
  background: linear-gradient(180deg, rgba(249, 246, 239, 0.96) 0%, rgba(239, 234, 223, 0.92) 100%);
  border: 1px solid rgba(110, 124, 104, 0.14);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 40px 90px rgba(47, 58, 48, 0.18);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(88, 100, 81, 0.12);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: #1b2821;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: rgba(50, 67, 56, 0.5);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.modal-body {
  padding: 1.25rem 1.5rem 1.5rem;
}

.book-context {
  color: rgba(57, 68, 58, 0.78);
  font-size: 0.85rem;
  margin: 0 0 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(118, 91, 56, 0.88);
  margin-bottom: 0.4rem;
}

.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.type-tag {
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(110, 122, 102, 0.16);
  border-radius: 20px;
  color: rgba(50, 67, 56, 0.78);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s;
}

.type-tag:hover {
  border-color: rgba(133, 160, 131, 0.3);
}

.type-tag.active {
  background: rgba(133, 160, 131, 0.18);
  border-color: rgba(133, 160, 131, 0.32);
  color: #3d5a3a;
}

.photo-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.photo-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(110, 124, 104, 0.14);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: rgba(184, 92, 56, 0.85);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-add {
  width: 80px;
  height: 80px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.92) 0%, rgba(246, 241, 231, 0.88) 100%);
  border: 2px dashed rgba(110, 122, 102, 0.22);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.15s;
}

.photo-add:hover {
  border-color: rgba(133, 160, 131, 0.4);
}

.photo-add input {
  display: none;
}

.add-icon {
  color: rgba(96, 106, 95, 0.46);
  font-size: 1.5rem;
}

.form-textarea {
  width: 100%;
  padding: 10px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.92) 0%, rgba(246, 241, 231, 0.88) 100%);
  border: 1px solid rgba(110, 122, 102, 0.16);
  border-radius: 8px;
  color: #1b2821;
  font-size: 0.85rem;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: rgba(113, 138, 107, 0.34);
  box-shadow: 0 0 0 4px rgba(133, 160, 131, 0.12);
}

.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 1.25rem;
}

.btn-cancel {
  flex: 1;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.48);
  border: 1px solid rgba(108, 123, 102, 0.18);
  border-radius: 10px;
  color: #1b2821;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-submit {
  flex: 2;
  padding: 10px 16px;
  background: linear-gradient(135deg, #335c67, #537072);
  border: none;
  border-radius: 10px;
  color: #fffdf5;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(51, 92, 103, 0.22);
  transition: opacity 0.15s;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.field-error {
  color: #8b482f;
  font-size: 0.8rem;
  margin: 0.5rem 0 0;
}
</style>
