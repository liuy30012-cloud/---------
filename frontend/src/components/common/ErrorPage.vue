<template>
  <section class="error-page" role="alert" aria-live="assertive">
    <div class="error-page__panel">
      <p class="error-page__eyebrow">{{ t('errorCenter.pageEyebrow') }}</p>
      <h1 class="error-page__title">{{ title || t('errorCenter.pageTitle') }}</h1>
      <p class="error-page__message">{{ message || t('errorCenter.pageMessage') }}</p>
      <p v-if="details" class="error-page__details">{{ details }}</p>
      <div class="error-page__actions">
        <ErrorRetry v-if="showRetry" :loading="retrying" @retry="$emit('retry')" />
        <button class="error-page__secondary" type="button" @click="$emit('back')">
          {{ t('common.button.back') }}
        </button>
        <button class="error-page__secondary" type="button" @click="$emit('home')">
          {{ t('errorCenter.backHome') }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ErrorRetry from './ErrorRetry.vue'

withDefaults(defineProps<{
  title?: string
  message?: string
  details?: string
  showRetry?: boolean
  retrying?: boolean
}>(), {
  title: '',
  message: '',
  details: '',
  showRetry: true,
  retrying: false,
})

defineEmits<{
  retry: []
  back: []
  home: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.error-page {
  min-height: clamp(22rem, 52vh, 34rem);
  display: grid;
  place-items: center;
  padding: 2rem 1rem;
}

.error-page__panel {
  width: min(100%, 42rem);
  padding: clamp(1.75rem, 4vw, 2.75rem);
  border-radius: 1.75rem;
  background:
    radial-gradient(circle at top right, rgba(196, 126, 84, 0.18), transparent 32%),
    linear-gradient(160deg, rgba(255, 248, 240, 0.98) 0%, rgba(250, 241, 232, 0.94) 100%);
  border: 1px solid rgba(130, 93, 71, 0.12);
  box-shadow: 0 28px 56px rgba(76, 54, 42, 0.12);
}

.error-page__eyebrow {
  margin: 0 0 0.75rem;
  color: #9a6046;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.error-page__title {
  margin: 0;
  color: #3e2a21;
  font-size: clamp(1.7rem, 4vw, 2.6rem);
  line-height: 1.15;
}

.error-page__message,
.error-page__details {
  margin: 1rem 0 0;
  color: #684c3f;
  line-height: 1.7;
}

.error-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  margin-top: 1.75rem;
}

.error-page__secondary {
  min-width: 8rem;
  padding: 0.8rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(130, 93, 71, 0.18);
  background: rgba(255, 252, 248, 0.86);
  color: #4f372d;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 767px) {
  .error-page__actions {
    flex-direction: column;
  }

  .error-page__secondary,
  :deep(.error-retry) {
    width: 100%;
  }
}
</style>
