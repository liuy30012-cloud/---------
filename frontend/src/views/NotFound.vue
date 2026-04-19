<template>
  <div class="not-found page-stack">
    <div class="not-found__inner">
      <div class="not-found__bubble">{{ t('notFound.bubble') }}</div>
      <div class="not-found__cat pet-anim-search">
        <img src="/pet/searching.png" alt="searching cat" draggable="false" />
      </div>
      <div class="not-found__title">{{ t('notFound.title') }}</div>
      <p class="not-found__desc">{{ t('notFound.description') }}</p>
      <div class="not-found__actions">
        <LibraryButton type="primary" @click="goHome">
          {{ t('notFound.goHome') }}
        </LibraryButton>
        <LibraryButton type="secondary" @click="goBack">
          {{ t('notFound.goBack') }}
        </LibraryButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import LibraryButton from '@/components/common/LibraryButton.vue'

const router = useRouter()
const { t } = useI18n()

const goHome = () => router.push({ name: 'Home' })
const goBack = () => router.back()
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}

.not-found__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.not-found__bubble {
  background: rgba(250, 240, 225, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(199, 160, 103, 0.2);
  border-radius: 16px 16px 16px 4px;
  padding: 0.5rem 1.25rem;
  color: var(--on-surface);
  font-size: 0.875rem;
  line-height: 1.5;
  max-width: 280px;
}

.not-found__cat {
  width: 160px;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.not-found__cat img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.25));
}

.not-found__title {
  font-size: 3.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dim) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.not-found__desc {
  color: var(--on-surface-variant);
  font-size: 0.875rem;
  margin: 0;
  max-width: 260px;
  line-height: 1.5;
}

.not-found__actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

@media (max-width: 640px) {
  .not-found__cat {
    width: 120px;
    height: 120px;
  }

  .not-found__title {
    font-size: 2.75rem;
  }

  .not-found__actions {
    flex-direction: column;
    width: 100%;
    max-width: 260px;
  }
}
</style>
