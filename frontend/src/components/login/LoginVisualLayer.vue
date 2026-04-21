<template>
  <div class="rice-paper-texture"></div>
  <div class="zen-vignette"></div>
  <div class="lantern-bloom"></div>

  <MountainLayer class="mountain-layer" />
  <MistLayer class="mist-shell" />
  <BambooLayer class="bamboo-shell" />

  <div class="ink-splatters">
    <span
      v-for="dot in props.inkDots"
      :key="dot.id"
      class="ink-dot"
      :style="dot.style"
    ></span>
  </div>

  <div class="ambient-dust">
    <span
      v-for="particle in props.ambientParticles"
      :key="particle.id"
      class="ambient-particle"
      :style="particle.style"
    ></span>
  </div>

  <span
    v-for="particle in props.mouseParticles"
    :key="particle.id"
    class="mouse-particle"
    :style="particle.style"
  ></span>

  <span
    v-for="ripple in props.clickRipples"
    :key="ripple.id"
    class="click-ripple"
    :style="ripple.style"
  ></span>

  <span
    v-for="dot in props.inkSplashDots"
    :key="dot.id"
    class="ink-splash-dot"
    :style="dot.style"
  ></span>

  <span
    v-for="particle in props.celebrationParticles"
    :key="particle.id"
    class="celebration-particle"
    :style="particle.style"
  ></span>

  <div class="book-pages">
    <div
      v-for="page in props.bookPages"
      :key="page.id"
      class="book-page"
      :style="page.style"
    >
      <div class="page-title">{{ page.title }}</div>
      <div class="page-author">{{ page.author }}</div>
      <div class="page-poem">{{ page.poem }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import BambooLayer from './BambooLayer.vue'
import MistLayer from './MistLayer.vue'
import MountainLayer from './MountainLayer.vue'

interface PositionedVisual {
  id: number
  style: Record<string, string>
}

interface FloatingIcon extends PositionedVisual {
  name: string
}

interface BookPage extends PositionedVisual {
  title: string
  author: string
  poem: string
}

const props = defineProps<{
  mouseParticles: PositionedVisual[]
  ambientParticles: PositionedVisual[]
  celebrationParticles: PositionedVisual[]
  clickRipples: PositionedVisual[]
  inkSplashDots: PositionedVisual[]
  floatingIcons: FloatingIcon[]
  bookPages: BookPage[]
  inkDots: PositionedVisual[]
}>()

void props.floatingIcons
</script>

<style scoped>
.rice-paper-texture,
.zen-vignette,
.lantern-bloom {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.rice-paper-texture {
  z-index: 0;
  opacity: 0.08;
  background-image:
    repeating-linear-gradient(0deg, transparent, transparent 4px, rgba(210, 198, 177, 0.22) 4px, rgba(210, 198, 177, 0.22) 5px),
    repeating-linear-gradient(90deg, transparent, transparent 5px, rgba(195, 204, 188, 0.12) 5px, rgba(195, 204, 188, 0.12) 6px);
}

.zen-vignette {
  z-index: 1;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 241, 212, 0.22) 0%, transparent 18%),
    radial-gradient(circle at 82% 16%, rgba(157, 181, 151, 0.14) 0%, transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, transparent 24%);
}

.lantern-bloom {
  z-index: 2;
  background:
    radial-gradient(circle at 72% 18%, rgba(245, 224, 181, 0.26) 0%, rgba(245, 224, 181, 0.08) 14%, transparent 28%);
}

.mountain-layer {
  z-index: 2;
  opacity: 0.8;
}

.mist-shell {
  z-index: 3;
  opacity: 0.38;
}

.bamboo-shell {
  z-index: 4;
  opacity: 0.55;
}

.ink-splatters {
  position: fixed;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.ink-dot {
  position: absolute;
  background: rgba(69, 77, 65, 0.28);
  animation: inkBreath 5.5s ease-in-out infinite alternate;
}

.ambient-dust {
  position: fixed;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  overflow: hidden;
}

.ambient-particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(197, 167, 113, 0.2);
  animation: ambientFloat 10s ease-in-out infinite alternate;
}

.mouse-particle {
  position: fixed;
  z-index: 5;
  pointer-events: none;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(166, 139, 96, 0.42) 0%, rgba(166, 139, 96, 0) 72%);
  animation: particleFade 0.8s ease-out forwards;
}

.click-ripple {
  position: fixed;
  z-index: 6;
  pointer-events: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(143, 119, 84, 0.34);
  animation: clickPulse 0.9s ease-out forwards;
}

.ink-splash-dot {
  position: fixed;
  z-index: 6;
  pointer-events: none;
  border-radius: 50%;
  background: rgba(109, 96, 74, 0.32);
  transform: translate(-50%, -50%);
  animation: inkDotFade 0.7s ease-out forwards;
}

.celebration-particle {
  position: fixed;
  z-index: 9;
  pointer-events: none;
  border-radius: 50%;
  background: rgba(196, 160, 103, 0.82);
  box-shadow: 0 0 8px rgba(196, 160, 103, 0.24);
  animation: celebrationBurst 0.9s ease-out forwards;
}

@keyframes inkBreath {
  0% { transform: scale(0.85); opacity: var(--dot-opacity, 0.08); }
  100% { transform: scale(1.1); opacity: calc(var(--dot-opacity, 0.08) * 1.45); }
}

@keyframes ambientFloat {
  0% {
    transform: translate(0, 0) scale(0.6);
    opacity: 0;
  }

  20%, 80% {
    opacity: 1;
  }

  100% {
    transform: translate(calc(var(--drift, 18px)), calc(-80px - var(--drift, 20px))) scale(1);
    opacity: 0;
  }
}

@keyframes particleFade {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }

  100% {
    opacity: 0;
    transform: translate(-50%, calc(-50% - 12px)) scale(0.25);
  }
}

@keyframes clickPulse {
  0% {
    width: 8px;
    height: 8px;
    opacity: 1;
  }

  100% {
    width: 70px;
    height: 70px;
    opacity: 0;
  }
}

@keyframes inkDotFade {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }

  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.28);
  }
}

@keyframes celebrationBurst {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) translate(0, 0) scale(1);
  }

  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translate(var(--dx, 80px), var(--dy, -80px)) scale(0);
  }
}

/* 诗页样式 */
.book-pages {
  position: fixed;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}

.book-page {
  position: absolute;
  top: -20%;
  background: linear-gradient(135deg, rgba(250, 245, 235, 0.95) 0%, rgba(245, 238, 220, 0.92) 100%);
  border: 1px solid rgba(180, 160, 130, 0.3);
  border-radius: 4px;
  padding: 16px 12px;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  writing-mode: vertical-rl;
  text-orientation: upright;
  font-family: 'KaiTi', 'STKaiti', serif;
  animation: bookPageFall var(--duration, 30s) linear var(--delay, 0s) forwards;
  transform: rotate(var(--rot-start, 0deg));
  will-change: transform, top;
}

.page-title {
  font-size: 11px;
  font-weight: 600;
  color: rgba(100, 80, 60, 0.9);
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.page-author {
  font-size: 9px;
  color: rgba(120, 100, 80, 0.75);
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.page-poem {
  font-size: var(--poem-font-size, 10px);
  line-height: 1.8;
  color: rgba(80, 70, 60, 0.85);
  letter-spacing: 1px;
  white-space: pre-line;
}

@keyframes bookPageFall {
  0% {
    top: -20%;
    transform: rotate(var(--rot-start, 0deg)) translateX(0);
    opacity: 0;
  }

  5% {
    opacity: 1;
  }

  50% {
    transform: rotate(var(--rot-mid, 180deg)) translateX(var(--sway, 30px));
  }

  95% {
    opacity: 1;
  }

  100% {
    top: 120%;
    transform: rotate(var(--rot-end, 360deg)) translateX(0);
    opacity: 0;
  }
}
</style>
