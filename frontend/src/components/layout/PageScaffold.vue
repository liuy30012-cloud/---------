<template>
  <section ref="scaffoldRef" class="page-scaffold" :class="shellClass">
    <div class="page-atmosphere" aria-hidden="true">
      <div ref="mistLeftRef" class="page-mist page-mist--left"></div>
      <div ref="mistRightRef" class="page-mist page-mist--right"></div>
      <div ref="glowRef" class="page-glow"></div>
    </div>
    <div class="page-shell">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { gsap, prefersReducedMotion } from '../../motion'

const props = withDefaults(defineProps<{
  shell?: 'default' | 'wide'
}>(), {
  shell: 'default'
})

const shellClass = `page-scaffold--${props.shell}`
const scaffoldRef = ref<HTMLElement | null>(null)
const mistLeftRef = ref<HTMLElement | null>(null)
const mistRightRef = ref<HTMLElement | null>(null)
const glowRef = ref<HTMLElement | null>(null)
let cleanupMotion: (() => void) | undefined

onMounted(() => {
  if (prefersReducedMotion() || !scaffoldRef.value) {
    return
  }

  const context = gsap.context(() => {
    if (mistLeftRef.value) {
      gsap.to(mistLeftRef.value, {
        x: 18,
        y: -10,
        duration: 11,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
      })
    }

    if (mistRightRef.value) {
      gsap.to(mistRightRef.value, {
        x: -16,
        y: 12,
        duration: 13,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
      })
    }

    if (glowRef.value) {
      gsap.to(glowRef.value, {
        y: 14,
        scale: 1.03,
        duration: 15,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
      })
    }
  }, scaffoldRef)

  cleanupMotion = () => context.revert()
})

onUnmounted(() => {
  cleanupMotion?.()
})
</script>

<style scoped>
.page-scaffold {
  position: relative;
  min-height: 100vh;
  padding-top: calc(var(--nav-height) + var(--space-8));
  padding-bottom: var(--space-10);
  isolation: isolate;
}

.page-scaffold::before,
.page-scaffold::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.page-scaffold::before {
  background:
    linear-gradient(180deg, rgba(249, 246, 239, 0.7) 0%, rgba(235, 239, 229, 0.18) 26%, transparent 66%);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.88) 0%, transparent 90%);
}

.page-scaffold::after {
  opacity: 0.28;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 5px, rgba(255, 255, 255, 0.16) 5px, rgba(255, 255, 255, 0.16) 6px),
    repeating-linear-gradient(90deg, transparent, transparent 6px, rgba(105, 123, 104, 0.04) 6px, rgba(105, 123, 104, 0.04) 7px);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.9) 0%, transparent 86%);
}

.page-atmosphere {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.page-mist,
.page-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(20px);
}

.page-mist {
  background: radial-gradient(circle, rgba(244, 242, 235, 0.8) 0%, rgba(225, 232, 221, 0.3) 46%, transparent 78%);
}

.page-mist--left {
  top: 6rem;
  left: -8rem;
  width: 26rem;
  height: 10rem;
}

.page-mist--right {
  top: 10rem;
  right: -6rem;
  width: 24rem;
  height: 9rem;
}

.page-glow {
  top: 2rem;
  left: 50%;
  width: min(70rem, 86vw);
  height: 18rem;
  transform: translateX(-50%);
  background: radial-gradient(circle, rgba(241, 223, 186, 0.2) 0%, rgba(180, 198, 171, 0.16) 34%, transparent 70%);
}

.page-shell {
  position: relative;
  z-index: 1;
  width: min(100% - 2 * var(--page-gutter), var(--page-width));
  margin: 0 auto;
}

.page-scaffold--wide .page-shell {
  width: min(100% - 2 * var(--page-gutter), var(--page-width-wide));
}

@media (max-width: 767px) {
  .page-scaffold {
    padding-top: calc(var(--nav-height) + var(--space-6));
    padding-bottom: var(--space-8);
  }

  .page-mist--left {
    left: -10rem;
    width: 22rem;
  }

  .page-mist--right {
    right: -9rem;
    width: 20rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .page-scaffold,
  .page-atmosphere,
  .page-mist,
  .page-glow {
    transition: none !important;
    animation: none !important;
  }
}
</style>
