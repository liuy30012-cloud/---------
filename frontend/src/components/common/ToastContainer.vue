<template>
  <div class="toast-container" aria-live="polite" aria-atomic="false">
    <FeedbackToast
      v-for="toast in toasts"
      :key="toast.id"
      :toast="toast"
      @close="hide"
    />
  </div>
</template>

<script setup lang="ts">
import FeedbackToast from './FeedbackToast.vue'
import { useFeedback } from '@/composables/useFeedback'

const { toasts, hide } = useFeedback()
</script>

<style scoped>
.toast-container {
  position: fixed;
  inset: 0;
  z-index: 140;
  pointer-events: none;
}

.toast-container :deep(.feedback-toast) {
  pointer-events: auto;
}
</style>
