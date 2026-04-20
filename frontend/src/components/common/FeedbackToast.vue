<template>
  <Transition :name="`toast-${animation}`">
    <div
      v-if="visible"
      class="feedback-toast"
      :class="[`feedback-toast--${normalizedToast.type}`, `feedback-toast--${normalizedToast.position}`]"
      role="status"
      :aria-live="normalizedToast.type === 'error' ? 'assertive' : 'polite'"
    >
      <span class="material-symbols-outlined toast-icon" aria-hidden="true">
        {{ getIcon() }}
      </span>

      <div class="toast-content">
        <p class="toast-message">{{ normalizedToast.message }}</p>

        <div v-if="normalizedToast.actions && normalizedToast.actions.length" class="toast-actions">
          <button
            v-for="(action, index) in normalizedToast.actions"
            :key="index"
            class="toast-action-btn"
            @click="handleAction(action)"
          >
            {{ action.label }}
          </button>
        </div>
      </div>

      <button
        v-if="normalizedToast.closable"
        class="toast-close"
        @click="handleClose"
        aria-label="关闭"
      >
        <span class="material-symbols-outlined">close</span>
      </button>

      <div
        v-if="normalizedToast.showProgress && (normalizedToast.duration ?? 0) > 0"
        class="toast-progress"
        :style="{ animationDuration: `${normalizedToast.duration ?? 0}ms` }"
      ></div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Toast, ToastAction, ToastType } from '@/types/feedback'

type LegacyToast = {
  message: string
  type: Exclude<ToastType, 'warning' | 'loading'>
}

const props = defineProps<{
  toast: Toast | LegacyToast
}>()

const emit = defineEmits<{
  close: [id: string]
}>()

const visible = ref(false)
const animation = ref('slide')
const normalizedToast = computed((): Toast => ({
  id: 'id' in props.toast ? props.toast.id : 'legacy-toast',
  createdAt: 'createdAt' in props.toast ? props.toast.createdAt : 0,
  duration: 'duration' in props.toast ? props.toast.duration : 2600,
  closable: 'closable' in props.toast ? props.toast.closable : true,
  position: 'position' in props.toast ? props.toast.position : 'bottom-right',
  showProgress: 'showProgress' in props.toast ? props.toast.showProgress : false,
  actions: 'actions' in props.toast ? props.toast.actions : undefined,
  icon: 'icon' in props.toast ? props.toast.icon : undefined,
  message: props.toast.message,
  type: props.toast.type,
}))

onMounted(() => {
  visible.value = true
})

const getIcon = () => {
  if (normalizedToast.value.icon) return normalizedToast.value.icon

  const icons = {
    success: 'check_circle',
    error: 'error',
    warning: 'warning',
    info: 'info',
    loading: 'progress_activity',
  }
  return icons[normalizedToast.value.type]
}

const handleClose = () => {
  visible.value = false
  setTimeout(() => emit('close', normalizedToast.value.id), 300)
}

const handleAction = (action: ToastAction) => {
  action.onClick()
  handleClose()
}
</script>

<style scoped>
.feedback-toast {
  position: fixed;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 130;
  display: inline-flex;
  align-items: flex-start;
  gap: 0.75rem;
  max-width: min(28rem, calc(100vw - 3rem));
  padding: 0.95rem 1.1rem;
  border-radius: 1rem;
  color: #fff8ef;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 56px rgba(36, 42, 36, 0.22);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.feedback-toast--success {
  background: linear-gradient(135deg, rgba(54, 96, 74, 0.95) 0%, rgba(88, 129, 103, 0.92) 100%);
}

.feedback-toast--error {
  background: linear-gradient(135deg, rgba(133, 69, 57, 0.95) 0%, rgba(176, 103, 90, 0.92) 100%);
}

.feedback-toast--warning {
  background: linear-gradient(135deg, rgba(146, 108, 46, 0.95) 0%, rgba(191, 149, 77, 0.92) 100%);
}

.feedback-toast--info {
  background: linear-gradient(135deg, rgba(88, 108, 97, 0.96) 0%, rgba(132, 150, 131, 0.9) 100%);
}

.feedback-toast--loading {
  background: linear-gradient(135deg, rgba(68, 83, 98, 0.96) 0%, rgba(105, 124, 145, 0.9) 100%);
}

.feedback-toast--top-left {
  top: 1.5rem;
  right: auto;
  bottom: auto;
  left: 1.5rem;
}

.feedback-toast--top-center {
  top: 1.5rem;
  right: 50%;
  bottom: auto;
  left: auto;
  transform: translateX(50%);
}

.feedback-toast--bottom-left {
  right: auto;
  bottom: 1.5rem;
  left: 1.5rem;
}

.feedback-toast--bottom-center {
  right: 50%;
  bottom: 1.5rem;
  left: auto;
  transform: translateX(50%);
}

.toast-icon {
  flex-shrink: 0;
  margin-top: 0.1rem;
  font-size: 1.2rem;
}

.toast-content {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.65rem;
  min-width: 0;
}

.toast-message {
  margin: 0;
  line-height: 1.5;
}

.toast-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.toast-action-btn,
.toast-close {
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  color: inherit;
}

.toast-action-btn {
  padding: 0.38rem 0.7rem;
  border-radius: 999px;
  font: inherit;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.toast-action-btn:hover,
.toast-close:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.28);
}

.toast-action-btn:active,
.toast-close:active {
  transform: translateY(1px);
}

.toast-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.toast-close .material-symbols-outlined {
  font-size: 1rem;
}

.toast-progress {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  background: rgba(255, 248, 239, 0.8);
  transform-origin: left center;
  animation: toast-progress linear forwards;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes toast-progress {
  from {
    transform: scaleX(1);
  }

  to {
    transform: scaleX(0);
  }
}

@media (max-width: 767px) {
  .feedback-toast,
  .feedback-toast--top-left,
  .feedback-toast--top-center,
  .feedback-toast--top-right,
  .feedback-toast--bottom-left,
  .feedback-toast--bottom-center,
  .feedback-toast--bottom-right {
    right: 1rem;
    left: 1rem;
    max-width: none;
  }

  .feedback-toast,
  .feedback-toast--bottom-left,
  .feedback-toast--bottom-right {
    bottom: 1rem;
    top: auto;
  }

  .feedback-toast--top-left,
  .feedback-toast--top-center,
  .feedback-toast--top-right {
    top: 1rem;
    bottom: auto;
  }
}
</style>
