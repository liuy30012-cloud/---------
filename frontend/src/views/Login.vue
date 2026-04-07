<template>
  <div class="login-page" @mousemove="enhancedMouseMove" @click="onPageClick" @keydown="onGlobalKeydown">
    <LoginVisualLayer
      v-if="showVisualLayer"
      :mouse-particles="mouseParticles"
      :ambient-particles="ambientParticles"
      :celebration-particles="celebrationParticles"
      :click-ripples="clickRipples"
      :ink-splash-dots="inkSplashDots"
      :floating-icons="floatingIcons"
      :book-pages="bookPages"
      :ink-dots="inkDots"
    />

    <canvas v-if="showVisualLayer" ref="inkCanvas" class="ink-canvas"></canvas>
    <div v-if="showVisualLayer" ref="mouseGlowRef" class="mouse-glow"></div>
    <canvas v-if="showVisualLayer" ref="brushCanvas" class="brush-canvas"></canvas>

    <LoginAuthCard
      :has-entered="hasEntered"
      :is-login="isLogin"
      :is-loading="isLoading"
      :show-password="showPassword"
      :error-message="errorMessage"
      :success-message="successMessage"
      :shake-error="shakeError"
      :typing-field="typingField"
      :submit-hovered="submitHovered"
      :seal-clicked="sealClicked"
      :seal-text="sealText"
      :social-buttons="socialButtons"
      :form-data="formData"
      :greeting-text="greetingText"
      :greeting-emoji="greetingEmoji"
      :typed-chars="typedChars"
      :password-strength="passwordStrength"
      :strength-label="strengthLabel"
      :passwords-match="passwordsMatch"
      :form-progress="formProgress"
      :progress-offset="progressOffset"
      :magnetic-btn-style="magneticBtnStyle"
      :card-tilt-style="cardTiltStyle"
      :card-light-style="cardLightStyle"
      @switch-mode="switchMode"
      @input-typing="onInputTyping"
      @toggle-password-visibility="togglePasswordVisibility"
      @submit="handleSubmit"
      @submit-hover="onSubmitHover"
      @submit-leave="onSubmitLeave"
      @submit-mouse-move="onSubmitMouseMove"
      @card-mouse-move="onCardMouseMove"
      @card-mouse-leave="onCardMouseLeave"
      @seal-click="onSealClick"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import LoginAuthCard from '../components/login/LoginAuthCard.vue'
import LoginVisualLayer from '../components/login/LoginVisualLayer.vue'
import { useInkCanvas } from '../composables/useInkCanvas'
import { useLoginForm } from '../composables/useLoginForm'
import { useLoginParticles } from '../composables/useLoginParticles'

const form = useLoginForm()
const particles = useLoginParticles()
const canvas = useInkCanvas()

const hasEntered = ref(false)
const showVisualLayer = ref(false)
const mouseGlowRef = ref<HTMLElement | null>(null)

const {
  isLogin, isLoading, showPassword, errorMessage, successMessage,
  shakeError, typingField, submitHovered, sealClicked,
  sealText, socialButtons, formData,
  greetingText, greetingEmoji, typedChars,
  passwordStrength, strengthLabel, passwordsMatch,
  formProgress, progressOffset, magneticBtnStyle,
  onInputTyping, togglePasswordVisibility,
  onSubmitHover, onSubmitLeave, onSealClick, switchMode,
} = form

const {
  mouseParticles, ambientParticles, celebrationParticles,
  clickRipples, inkSplashDots, floatingIcons, bookPages, inkDots,
} = particles

const {
  inkCanvas, brushCanvas,
  updateMouseGlow, cardTiltStyle, cardLightStyle,
} = canvas

function enhancedMouseMove(event: MouseEvent) {
  if (!showVisualLayer.value) return
  particles.onPageMouseMove(event)
  updateMouseGlow(event.clientX, event.clientY, mouseGlowRef.value)
  canvas.addBrushPoint(event.clientX, event.clientY)
}

function onPageClick(event: MouseEvent) {
  if (!showVisualLayer.value) return
  particles.onPageClick(event)
}

function onCardMouseMove(event: MouseEvent, element: HTMLDivElement | null) {
  canvas.onCardMouseMove(event, element)
}

function onCardMouseLeave() {
  canvas.onCardMouseLeave()
}

function onSubmitMouseMove(event: MouseEvent, element: HTMLButtonElement | null) {
  form.onSubmitMouseMove(event, element)
}

function handleSubmit() {
  form.handleSubmit(particles.triggerCelebration)
}

function onGlobalKeydown(_event: KeyboardEvent) {
  // Reserved for future keyboard shortcuts.
}

onMounted(() => {
  setTimeout(() => { hasEntered.value = true }, 100)
  window.setTimeout(() => {
    showVisualLayer.value = true
    particles.createInkDots()
    particles.createAmbientParticles()
    canvas.initInkCanvas()
    canvas.initBrushCanvas()
  }, 180)
})

onUnmounted(() => {
  canvas.cleanup()
  form.cleanup()
})
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(243, 239, 229, 0.96) 0%, rgba(221, 230, 219, 0.94) 42%, rgba(197, 210, 196, 0.96) 100%);
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-page::before {
  background:
    radial-gradient(circle at 18% 16%, rgba(246, 231, 194, 0.26) 0%, transparent 18%),
    radial-gradient(circle at 78% 18%, rgba(154, 179, 151, 0.22) 0%, transparent 20%);
}

.login-page::after {
  opacity: 0.22;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 6px, rgba(255, 255, 255, 0.16) 6px, rgba(255, 255, 255, 0.16) 7px),
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(112, 127, 108, 0.04) 7px, rgba(112, 127, 108, 0.04) 8px);
}

.ink-canvas,
.brush-canvas {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.ink-canvas {
  z-index: 1;
}

.brush-canvas {
  z-index: 4;
}

.mouse-glow {
  position: fixed;
  z-index: 4;
  pointer-events: none;
  width: 260px;
  height: 260px;
  left: 0;
  top: 0;
  margin-left: -130px;
  margin-top: -130px;
  background: radial-gradient(circle, rgba(240, 225, 191, 0.16) 0%, rgba(198, 167, 112, 0.06) 42%, transparent 74%);
  filter: blur(3px);
}

@media (max-width: 767px) {
  .login-page {
    padding: 18px;
  }

  .mouse-glow {
    width: 180px;
    height: 180px;
    margin-left: -90px;
    margin-top: -90px;
  }
}
</style>
