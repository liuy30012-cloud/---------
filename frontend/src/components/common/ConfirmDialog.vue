<template>
  <Transition name="dialog-fade">
    <div v-if="open" class="dialog-overlay" @click.self="$emit('cancel')">
      <div
        class="dialog-panel"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="dialogTitleId"
        :aria-describedby="dialogMessageId"
        tabindex="-1"
      >
        <div class="dialog-copy">
          <p v-if="eyebrow" class="dialog-eyebrow">{{ eyebrow }}</p>
          <h3 :id="dialogTitleId" class="dialog-title">{{ title }}</h3>
          <p :id="dialogMessageId" class="dialog-message">{{ message }}</p>
        </div>
        <div class="dialog-actions">
          <button class="dialog-btn dialog-btn--secondary" type="button" @click="$emit('cancel')">
            {{ cancelText }}
          </button>
          <button class="dialog-btn dialog-btn--primary" type="button" @click="$emit('confirm')">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
const props = defineProps<{
  open: boolean
  title: string
  message: string
  eyebrow?: string
  confirmText?: string
  cancelText?: string
}>()

defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const dialogIdBase = `dialog-${Math.random().toString(36).slice(2)}`
const dialogTitleId = `${dialogIdBase}-title`
const dialogMessageId = `${dialogIdBase}-message`

void props
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: rgba(23, 28, 22, 0.38);
  backdrop-filter: blur(18px) saturate(112%);
}

.dialog-panel {
  width: min(100%, 31rem);
  padding: 1.8rem;
  border-radius: 1.5rem;
  background:
    linear-gradient(180deg, rgba(248, 245, 238, 0.96) 0%, rgba(237, 231, 220, 0.92) 100%);
  border: 1px solid rgba(108, 123, 102, 0.18);
  box-shadow: 0 30px 72px rgba(33, 39, 33, 0.18);
}

.dialog-eyebrow {
  margin: 0 0 0.45rem;
  font-size: 0.72rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(126, 95, 57, 0.86);
}

.dialog-title {
  margin: 0;
  font-family: var(--font-headline);
  font-size: 1.45rem;
  line-height: 1.1;
  color: var(--home-ink);
}

.dialog-message {
  margin: 0.8rem 0 0;
  line-height: 1.75;
  color: rgba(60, 70, 60, 0.78);
}

.dialog-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.35rem;
}

.dialog-btn {
  flex: 1;
  min-height: 2.9rem;
  border-radius: 999px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.dialog-btn:hover {
  transform: translateY(-1px);
}

.dialog-btn:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 3px;
}

.dialog-btn--primary {
  color: #1a130d;
  border: none;
  background: linear-gradient(135deg, #d7b37a 0%, #ba8850 50%, #efd0a6 100%);
  box-shadow: 0 14px 28px rgba(121, 93, 57, 0.18);
}

.dialog-btn--secondary {
  color: var(--home-ink);
  background: rgba(255, 255, 255, 0.48);
  border: 1px solid rgba(108, 123, 102, 0.18);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

@media (max-width: 767px) {
  .dialog-actions {
    flex-direction: column-reverse;
  }
}
</style>
