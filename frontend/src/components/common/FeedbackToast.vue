<template>
  <Transition name="toast-slide">
    <div
      v-if="message"
      class="feedback-toast"
      :class="`feedback-toast--${type}`"
      role="status"
      aria-live="polite"
    >
      <span class="material-symbols-outlined toast-icon" aria-hidden="true">
        {{ type === 'success' ? 'check_circle' : type === 'error' ? 'warning' : 'info' }}
      </span>
      <p class="toast-message">{{ message }}</p>
    </div>
  </Transition>
</template>

<script setup lang="ts">
defineProps<{
  message: string
  type?: 'success' | 'error' | 'info'
}>()
</script>

<style scoped>
.feedback-toast {
  position: fixed;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 130;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  max-width: min(26rem, calc(100vw - 3rem));
  padding: 0.95rem 1.1rem;
  border-radius: 1rem;
  color: #fff8ef;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 24px 56px rgba(36, 42, 36, 0.22);
  backdrop-filter: blur(18px);
}

.feedback-toast--success {
  background: linear-gradient(135deg, rgba(54, 96, 74, 0.95) 0%, rgba(88, 129, 103, 0.92) 100%);
}

.feedback-toast--error {
  background: linear-gradient(135deg, rgba(133, 69, 57, 0.95) 0%, rgba(176, 103, 90, 0.92) 100%);
}

.feedback-toast--info {
  background: linear-gradient(135deg, rgba(88, 108, 97, 0.96) 0%, rgba(132, 150, 131, 0.9) 100%);
}

.toast-icon {
  font-size: 1.2rem;
}

.toast-message {
  margin: 0;
  line-height: 1.5;
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

@media (max-width: 767px) {
  .feedback-toast {
    right: 1rem;
    left: 1rem;
    bottom: 1rem;
    max-width: none;
  }
}
</style>
