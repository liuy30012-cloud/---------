<template>
  <div class="forgot-page">
    <div class="forgot-container" :class="{ entered: hasEntered }">
      <div class="breathing-halo" aria-hidden="true"></div>

      <div class="forgot-brand">
        <div class="badge-ring">
          <img src="/school-badge.png" alt="中国劳动关系学院校徽" class="brand-badge" />
        </div>
        <h2 class="brand-name">中国劳动关系学院图书馆</h2>
      </div>

      <div class="forgot-card">
        <button type="button" class="back-btn" @click="goBack">
          <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
          返回登录
        </button>

        <h3 class="forgot-title">
          <span class="material-symbols-outlined title-icon" aria-hidden="true">lock_reset</span>
          忘记密码
        </h3>

        <p class="forgot-desc">请输入您的学工号，查询账号状态并获取帮助。</p>

        <form class="forgot-form" @submit.prevent="handleQuery">
          <div class="input-group">
            <span class="material-symbols-outlined input-icon" aria-hidden="true">badge</span>
            <input
              v-model="studentId"
              type="text"
              class="forgot-input"
              placeholder="请输入学工号"
              inputmode="numeric"
              autocomplete="username"
              :disabled="isLoading"
            />
          </div>

          <button type="submit" class="query-btn" :disabled="isLoading || !studentId.trim()">
            <span v-if="!isLoading" class="btn-content">
              <span class="material-symbols-outlined btn-icon" aria-hidden="true">search</span>
              查询账号
            </span>
            <span v-else class="btn-loading">
              <span class="spinner"></span>
              查询中...
            </span>
          </button>
        </form>

        <Transition name="result-fade">
          <div v-if="result" class="result-section">
            <!-- 账号不存在 -->
            <div v-if="!result.exists" class="result-card result-error">
              <span class="material-symbols-outlined result-icon" aria-hidden="true">person_off</span>
              <div class="result-body">
                <p class="result-title">未找到该账号</p>
                <p class="result-desc">未找到学工号 <strong>{{ queriedId }}</strong> 对应的账号，请确认后重试。</p>
              </div>
            </div>

            <!-- 账号已停用 -->
            <div v-else-if="!result.active" class="result-card result-warning">
              <span class="material-symbols-outlined result-icon" aria-hidden="true">block</span>
              <div class="result-body">
                <p class="result-title">账号已停用</p>
                <p class="result-desc">该账号已被停用，请联系图书馆管理员恢复账号。</p>
              </div>
            </div>

            <!-- 账号正常 -->
            <div v-else class="result-card result-success">
              <span class="material-symbols-outlined result-icon" aria-hidden="true">account_circle</span>
              <div class="result-body">
                <p class="result-title">账号状态正常</p>
                <p class="result-desc">请联系图书馆管理员重置密码，携带有效证件前往以下地点：</p>
              </div>

              <div class="contact-cards">
                <div class="contact-item">
                  <span class="material-symbols-outlined contact-icon" aria-hidden="true">location_on</span>
                  <div>
                    <p class="contact-label">咨询台</p>
                    <p class="contact-value">图书馆一楼大厅服务台</p>
                  </div>
                </div>
                <div class="contact-item">
                  <span class="material-symbols-outlined contact-icon" aria-hidden="true">call</span>
                  <div>
                    <p class="contact-label">联系电话</p>
                    <p class="contact-value">010-8856-1988</p>
                  </div>
                </div>
                <div class="contact-item">
                  <span class="material-symbols-outlined contact-icon" aria-hidden="true">schedule</span>
                  <div>
                    <p class="contact-label">工作时间</p>
                    <p class="contact-value">周一至周五 8:30 - 17:00</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <Transition name="result-fade">
          <div v-if="errorMessage" class="error-toast" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">error</span>
            {{ errorMessage }}
          </div>
        </Transition>
      </div>

      <p class="footer-text">&copy; 2026 中国劳动关系学院图书馆</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import baseHttp from '../api/baseHttp'

interface AccountStatus {
  exists: boolean
  active: boolean
  hasEmail: boolean
  hasPhone: boolean
}

const router = useRouter()
const studentId = ref('')
const queriedId = ref('')
const isLoading = ref(false)
const result = ref<AccountStatus | null>(null)
const errorMessage = ref('')
const hasEntered = ref(false)

let errorTimer: ReturnType<typeof setTimeout> | null = null

async function handleQuery() {
  const id = studentId.value.trim()
  if (!id) return

  isLoading.value = true
  result.value = null
  errorMessage.value = ''
  queriedId.value = id

  try {
    const response = await baseHttp.post('/api/auth/account-status', { studentId: id })
    if (response.data.success) {
      result.value = {
        exists: response.data.exists,
        active: response.data.active ?? false,
        hasEmail: response.data.hasEmail ?? false,
        hasPhone: response.data.hasPhone ?? false,
      }
    } else {
      errorMessage.value = response.data.message || '查询失败，请稍后重试。'
    }
  } catch {
    errorMessage.value = '网络异常，请检查网络连接后重试。'
  } finally {
    isLoading.value = false
  }
}

function goBack() {
  router.push({ name: 'Login' })
}

onMounted(() => {
  setTimeout(() => { hasEntered.value = true }, 100)
})

onUnmounted(() => {
  if (errorTimer) clearTimeout(errorTimer)
})
</script>

<style scoped>
.forgot-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(243, 239, 229, 0.96) 0%, rgba(221, 230, 219, 0.94) 42%, rgba(197, 210, 196, 0.96) 100%);
}

.forgot-page::before,
.forgot-page::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.forgot-page::before {
  background:
    radial-gradient(circle at 18% 16%, rgba(246, 231, 194, 0.26) 0%, transparent 18%),
    radial-gradient(circle at 78% 18%, rgba(154, 179, 151, 0.22) 0%, transparent 20%);
}

.forgot-page::after {
  opacity: 0.22;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 6px, rgba(255, 255, 255, 0.16) 6px, rgba(255, 255, 255, 0.16) 7px),
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(112, 127, 108, 0.04) 7px, rgba(112, 127, 108, 0.04) 8px);
}

.forgot-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 30rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  opacity: 0;
  transform: translateY(28px) scale(0.98);
  transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

.forgot-container.entered {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.breathing-halo {
  position: absolute;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(221, 198, 154, 0.12) 0%, transparent 70%);
  animation: breathe 6s ease-in-out infinite;
  pointer-events: none;
  top: -80px;
}

@keyframes breathe {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.08); opacity: 1; }
}

.forgot-brand {
  text-align: center;
  margin-bottom: 1.5rem;
}

.badge-ring {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 4.5rem;
  height: 4.5rem;
  padding: 0.25rem;
  margin-bottom: 0.7rem;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(221, 198, 154, 0.82) 0%, rgba(204, 220, 196, 0.72) 100%);
  box-shadow: 0 12px 24px rgba(45, 54, 45, 0.1);
}

.brand-badge {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: contain;
}

.brand-name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(53, 63, 52, 0.92);
  letter-spacing: 0.04em;
}

.forgot-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(221, 198, 154, 0.28);
  border-radius: 1.25rem;
  padding: 2rem;
  box-shadow:
    0 8px 32px rgba(45, 54, 45, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.6) inset;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: none;
  color: rgba(127, 95, 55, 0.78);
  font-size: 0.85rem;
  cursor: pointer;
  padding: 0.3rem 0;
  margin-bottom: 1.2rem;
  transition: color 0.2s;
  font-family: inherit;
}

.back-btn:hover {
  color: rgba(98, 123, 92, 0.92);
}

.back-btn .material-symbols-outlined {
  font-size: 1.1rem;
}

.forgot-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(53, 63, 52, 0.92);
}

.title-icon {
  font-size: 1.4rem;
  color: rgba(127, 95, 55, 0.72);
}

.forgot-desc {
  margin: 0 0 1.4rem;
  font-size: 0.88rem;
  color: rgba(83, 94, 82, 0.72);
  line-height: 1.5;
}

.forgot-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.85rem;
  font-size: 1.15rem;
  color: rgba(127, 95, 55, 0.5);
  pointer-events: none;
}

.forgot-input {
  width: 100%;
  padding: 0.78rem 0.85rem 0.78rem 2.7rem;
  border: 1.5px solid rgba(221, 198, 154, 0.32);
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.6);
  font-size: 0.92rem;
  color: rgba(53, 63, 52, 0.92);
  outline: none;
  transition: border-color 0.25s, box-shadow 0.25s;
  font-family: inherit;
}

.forgot-input::placeholder {
  color: rgba(127, 95, 55, 0.4);
}

.forgot-input:focus {
  border-color: rgba(154, 179, 151, 0.6);
  box-shadow: 0 0 0 3px rgba(154, 179, 151, 0.12);
}

.forgot-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.query-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  width: 100%;
  padding: 0.78rem;
  border: none;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, rgba(154, 179, 151, 0.85) 0%, rgba(127, 148, 124, 0.9) 100%);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.25s, opacity 0.2s;
  font-family: inherit;
}

.query-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(127, 148, 124, 0.3);
}

.query-btn:active:not(:disabled) {
  transform: translateY(0);
}

.query-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-content {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-icon {
  font-size: 1.1rem;
}

.btn-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.spinner {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 结果区域 */
.result-section {
  margin-top: 0.5rem;
}

.result-card {
  border-radius: 0.85rem;
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.result-error {
  background: rgba(220, 80, 60, 0.06);
  border: 1px solid rgba(220, 80, 60, 0.15);
}

.result-warning {
  background: rgba(220, 170, 40, 0.06);
  border: 1px solid rgba(220, 170, 40, 0.15);
}

.result-success {
  background: rgba(127, 168, 124, 0.06);
  border: 1px solid rgba(127, 168, 124, 0.15);
}

.result-icon {
  font-size: 1.8rem;
}

.result-error .result-icon { color: rgba(200, 60, 50, 0.7); }
.result-warning .result-icon { color: rgba(200, 150, 30, 0.7); }
.result-success .result-icon { color: rgba(100, 150, 100, 0.7); }

.result-body {
  flex: 1;
}

.result-title {
  margin: 0 0 0.3rem;
  font-size: 1rem;
  font-weight: 600;
  color: rgba(53, 63, 52, 0.92);
}

.result-desc {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(83, 94, 82, 0.72);
  line-height: 1.55;
}

/* 联系信息卡片 */
.contact-cards {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-top: 0.4rem;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 0.65rem 0.8rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 0.6rem;
  border: 1px solid rgba(221, 198, 154, 0.18);
}

.contact-icon {
  font-size: 1.2rem;
  color: rgba(127, 95, 55, 0.65);
  margin-top: 0.1rem;
}

.contact-label {
  margin: 0;
  font-size: 0.75rem;
  color: rgba(83, 94, 82, 0.55);
  font-weight: 500;
}

.contact-value {
  margin: 0.1rem 0 0;
  font-size: 0.88rem;
  color: rgba(53, 63, 52, 0.88);
  font-weight: 500;
}

/* 错误提示 */
.error-toast {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.8rem;
  padding: 0.65rem 0.9rem;
  border-radius: 0.6rem;
  background: rgba(220, 80, 60, 0.08);
  border: 1px solid rgba(220, 80, 60, 0.15);
  color: rgba(180, 50, 40, 0.85);
  font-size: 0.85rem;
}

.error-toast .material-symbols-outlined {
  font-size: 1.1rem;
}

/* 过渡动画 */
.result-fade-enter-active {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.result-fade-leave-active {
  transition: all 0.2s ease-in;
}

.result-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.result-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.footer-text {
  margin: 1.2rem 0 0;
  font-size: 0.78rem;
  color: rgba(83, 94, 82, 0.45);
}

@media (max-width: 767px) {
  .forgot-page {
    padding: 18px;
  }

  .forgot-card {
    padding: 1.4rem;
  }

  .badge-ring {
    width: 3.5rem;
    height: 3.5rem;
  }

  .brand-name {
    font-size: 1rem;
  }
}
</style>
