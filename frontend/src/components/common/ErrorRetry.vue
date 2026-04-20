<template>
  <button
    class="error-retry"
    type="button"
    :disabled="loading"
    @click="$emit('retry')"
  >
    <span class="material-symbols-outlined" aria-hidden="true">refresh</span>
    <span>{{ loading ? t('errorCenter.retrying') : resolvedLabel }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  label?: string
  loading?: boolean
}>(), {
  label: '',
  loading: false,
})

defineEmits<{
  retry: []
}>()

const { t } = useI18n()
const resolvedLabel = computed(() => props.label || t('errorCenter.retry'))
</script>

<style scoped>
.error-retry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-width: 9rem;
  padding: 0.8rem 1.1rem;
  border: 1px solid rgba(148, 92, 69, 0.2);
  border-radius: 999px;
  background: linear-gradient(135deg, #b86e53 0%, #8c503e 100%);
  color: #fff7ef;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 18px 30px rgba(118, 63, 47, 0.18);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.error-retry:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 36px rgba(118, 63, 47, 0.24);
}

.error-retry:disabled {
  opacity: 0.72;
  cursor: wait;
}
</style>
