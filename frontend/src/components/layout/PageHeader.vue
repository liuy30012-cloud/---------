<template>
  <header class="page-header">
    <div class="page-header__copy">
      <p v-if="eyebrow" ref="eyebrowRef" class="page-header__eyebrow">{{ eyebrow }}</p>
      <h1 ref="titleRef" class="page-header__title">{{ title }}</h1>
      <p v-if="description" ref="descriptionRef" class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" ref="actionsRef" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { gsap, prefersReducedMotion } from '../../motion'

defineProps<{
  title: string
  description?: string
  eyebrow?: string
}>()

const eyebrowRef = ref<HTMLElement | null>(null)
const titleRef = ref<HTMLElement | null>(null)
const descriptionRef = ref<HTMLElement | null>(null)
const actionsRef = ref<HTMLElement | null>(null)

onMounted(() => {
  if (prefersReducedMotion()) {
    return
  }

  const targets = [eyebrowRef.value, titleRef.value, descriptionRef.value, actionsRef.value].filter(Boolean)
  if (targets.length === 0) {
    return
  }

  gsap.set(targets, { willChange: 'transform, opacity' })

  gsap.fromTo(
    targets,
    {
      autoAlpha: 0,
      y: 20,
      filter: 'blur(10px)',
    },
    {
      autoAlpha: 1,
      y: 0,
      filter: 'blur(0px)',
      duration: 0.62,
      ease: 'power2.out',
      stagger: 0.08,
      delay: 0.04,
      clearProps: 'filter',
      onComplete: () => {
        gsap.set(targets, { willChange: 'auto' })
      },
    }
  )
})
</script>

<style scoped>
.page-header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-7);
  padding: var(--space-7) 0 var(--space-6);
  border-bottom: 1px solid rgba(112, 127, 107, 0.16);
}

.page-header::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(248, 236, 203, 0.18) 0%, transparent 24%),
    radial-gradient(circle at top right, rgba(163, 187, 157, 0.16) 0%, transparent 28%);
  filter: blur(8px);
}

.page-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 8rem;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(188, 148, 93, 0.94) 0%, rgba(103, 132, 102, 0.72) 100%);
}

.page-header__copy {
  min-width: 0;
}

.page-header__eyebrow {
  margin: 0 0 var(--space-2);
  font-size: 0.75rem;
  font-family: var(--font-label);
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(127, 95, 55, 0.78);
}

.page-header__title {
  margin: 0;
  font-family: var(--font-headline);
  font-size: clamp(2.1rem, 3vw, 3rem);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.04em;
  color: var(--home-ink);
  text-wrap: balance;
}

.page-header__description {
  max-width: 48rem;
  margin: var(--space-3) 0 0;
  font-size: 1rem;
  line-height: 1.8;
  color: rgba(57, 68, 58, 0.78);
}

.page-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  flex-wrap: wrap;
  flex-shrink: 0;
  padding-top: var(--space-2);
  position: relative;
  z-index: 1;
}

@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
    margin-bottom: var(--space-6);
    padding-bottom: var(--space-4);
  }

  .page-header__actions {
    justify-content: flex-start;
  }

  .page-header__description {
    font-size: 0.95rem;
  }
}
</style>
