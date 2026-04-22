<template>
  <Transition name="loading-overlay-fade">
    <div v-if="visible" class="loading-overlay" role="status" aria-live="polite">
      <div class="loading-panel">
        <span class="material-symbols-outlined loading-icon" aria-hidden="true">progress_activity</span>
        <p class="loading-text">{{ message }}</p>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  message?: string
}>()
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: rgba(28, 32, 35, 0.38);
  backdrop-filter: blur(10px);
}

.loading-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.85rem;
  min-width: 12rem;
  padding: 1.25rem 1.5rem;
  border-radius: 1rem;
  background: rgba(255, 250, 243, 0.94);
  box-shadow: 0 20px 48px rgba(26, 31, 29, 0.18);
  color: #355348;
}

.loading-icon {
  font-size: 2rem;
  animation: loading-overlay-spin 1s linear infinite;
}

.loading-text {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #3f5248;
}

.loading-overlay-fade-enter-active,
.loading-overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.loading-overlay-fade-enter-from,
.loading-overlay-fade-leave-to {
  opacity: 0;
}

@keyframes loading-overlay-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-icon {
    animation: none !important;
  }
}
</style>
