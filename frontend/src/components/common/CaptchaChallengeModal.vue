<template>
  <teleport to="body">
    <div v-if="challengeState.visible" class="captcha-overlay">
      <div class="captcha-modal" role="dialog" aria-modal="true" aria-labelledby="captcha-title" aria-describedby="captcha-copy">
        <div class="captcha-header">
          <div>
            <p class="captcha-eyebrow">Security Challenge</p>
            <h2 id="captcha-title">{{ t('captcha.title') }}</h2>
          </div>
          <button class="ghost-button" type="button" @click="handleCancel" :disabled="challengeState.loading">
            {{ t('captcha.cancel') }}
          </button>
        </div>

        <p id="captcha-copy" class="captcha-copy">
          {{ t('captcha.copy') }}
        </p>

        <div v-if="session" class="captcha-stage">
          <div class="captcha-artboard" :style="artboardStyle">
            <span
              v-for="(piece, index) in session.puzzlePieces"
              :key="`${piece.x}-${piece.y}-${index}`"
              class="puzzle-piece"
              :style="{ left: `${piece.x}px`, top: `${piece.y}px` }"
            />
            <span class="target-slot" :style="{ left: `${session.targetX}px`, top: `${session.targetY}px` }" />
            <span class="target-guide" :style="{ left: `${session.targetX + session.sliderWidth / 2}px` }" />
          </div>

          <div ref="trackRef" class="slider-track">
            <div class="slider-progress" :style="{ width: `${sliderX + session.sliderWidth / 2}px` }" />
            <div class="slider-label">{{ t('captcha.sliderLabel') }}</div>
            <button
              class="slider-handle"
              type="button"
              :style="{ width: `${session.sliderWidth}px`, transform: `translateX(${sliderX}px)` }"
              :disabled="challengeState.loading"
              @pointerdown="handlePointerDown"
            >
              »
            </button>
          </div>
        </div>

        <div v-else class="captcha-loading">
          {{ challengeState.loading ? t('captcha.loading') : t('captcha.unavailable') }}
        </div>

        <p v-if="challengeState.errorMessage" class="captcha-error" role="alert">
          {{ challengeState.errorMessage }}
        </p>

        <div class="captcha-actions">
          <button class="secondary-button" type="button" @click="reloadChallenge" :disabled="challengeState.loading">
            {{ t('captcha.refresh') }}
          </button>
          <button class="primary-button" type="button" @click="submitCaptcha" :disabled="!session || challengeState.loading">
            {{ challengeState.loading ? t('captcha.verifying') : t('captcha.submit') }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import baseHttp from '../../api/baseHttp'
import { setCaptchaPassToken } from '../../api/antiCrawler'
import {
  cancelCaptchaChallenge,
  resolveCaptchaChallenge,
  setCaptchaChallengeError,
  setCaptchaChallengeLoading,
  useSecurityChallengeState,
} from '../../state/securityChallenge'

interface PuzzlePiece {
  x: number
  y: number
}

interface CaptchaSessionResponse {
  sessionId: string
  backgroundWidth: number
  backgroundHeight: number
  sliderWidth: number
  targetX: number
  targetY: number
  puzzlePieces: PuzzlePiece[]
}

interface CaptchaVerifyResponse {
  success: boolean
  message?: string
  data?: {
    verified?: boolean
    passToken?: string
    expiresIn?: number
  }
}

interface DragPoint {
  x: number
  y: number
  t: number
}

const { t } = useI18n()
const challengeState = useSecurityChallengeState()
const session = ref<CaptchaSessionResponse | null>(null)
const sliderX = ref(0)
const trackRef = ref<HTMLElement | null>(null)

let dragStartTime = 0
let dragging = false
let dragTrail: DragPoint[] = []

const artboardStyle = computed(() => {
  if (!session.value) {
    return {}
  }

  return {
    width: `${session.value.backgroundWidth}px`,
    height: `${session.value.backgroundHeight}px`,
  }
})

watch(
  () => challengeState.requestSerial,
  () => {
    if (challengeState.visible) {
      loadChallenge()
    }
  },
)

onBeforeUnmount(() => {
  detachListeners()
})

async function loadChallenge(clearError: boolean = true) {
  session.value = null
  sliderX.value = 0
  dragTrail = []
  if (clearError) {
    setCaptchaChallengeError('')
  }
  setCaptchaChallengeLoading(true)

  try {
    const response = await baseHttp.get<{ success: boolean; data: CaptchaSessionResponse }>('/api/captcha/generate')
    session.value = response.data.data
  } catch {
    cancelCaptchaChallenge(t('captcha.errors.loadFailed'))
    return
  } finally {
    setCaptchaChallengeLoading(false)
  }
}

async function reloadChallenge() {
  if (!challengeState.visible) {
    return
  }
  await loadChallenge()
}

function handlePointerDown(event: PointerEvent) {
  if (!session.value || challengeState.loading) {
    return
  }

  dragging = true
  dragStartTime = performance.now()
  dragTrail = []
  appendTrail(event)

  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
}

function handlePointerMove(event: PointerEvent) {
  if (!dragging || !session.value || !trackRef.value) {
    return
  }

  const rect = trackRef.value.getBoundingClientRect()
  const maxX = rect.width - session.value.sliderWidth
  const nextX = clamp(event.clientX - rect.left - session.value.sliderWidth / 2, 0, maxX)
  sliderX.value = nextX
  appendTrail(event)
}

function handlePointerUp(event: PointerEvent) {
  if (!dragging) {
    return
  }

  appendTrail(event)
  dragging = false
  detachListeners()
}

function detachListeners() {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
}

function appendTrail(event: PointerEvent) {
  if (!trackRef.value) {
    return
  }

  const rect = trackRef.value.getBoundingClientRect()
  const elapsed = performance.now() - dragStartTime
  const yJitter = Math.round(Math.sin((elapsed + dragTrail.length) / 20) * 2)

  dragTrail.push({
    x: Math.round(sliderX.value),
    y: Math.round(event.clientY - rect.top + yJitter),
    t: Math.round(elapsed),
  })
}

async function submitCaptcha() {
  if (!session.value) {
    return
  }

  setCaptchaChallengeLoading(true)
  try {
    const payload = {
      sessionId: session.value.sessionId,
      sliderX: Math.round(sliderX.value),
      dragTime: Math.max(Math.round(performance.now() - dragStartTime), dragTrail.length > 0 ? dragTrail[dragTrail.length - 1].t : 0),
      dragTrail,
    }

    const { data } = await baseHttp.post<CaptchaVerifyResponse>('/api/captcha/verify', payload)
    if (!data.success || !data.data?.verified || !data.data?.passToken) {
      const message = data.message || t('captcha.errors.verifyFailed')
      await loadChallenge(false)
      setCaptchaChallengeError(message)
      return
    }

    setCaptchaPassToken(data.data.passToken, data.data.expiresIn ?? 600)
    resolveCaptchaChallenge(data.data.passToken)
  } catch {
    await loadChallenge(false)
    setCaptchaChallengeError(t('captcha.errors.verifyFailedLater'))
  } finally {
    setCaptchaChallengeLoading(false)
  }
}

function handleCancel() {
  cancelCaptchaChallenge(t('captcha.errors.cancelled'))
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}
</script>

<style scoped>
.captcha-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(31, 38, 30, 0.28);
  backdrop-filter: blur(10px);
}

.captcha-modal {
  width: min(100%, 560px);
  padding: 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.78), transparent 34%),
    linear-gradient(160deg, rgba(249, 246, 239, 0.98) 0%, rgba(236, 231, 220, 0.94) 100%);
  color: var(--home-ink);
  border: 1px solid rgba(111, 126, 106, 0.16);
  box-shadow: 0 28px 90px rgba(28, 34, 28, 0.18);
}

.captcha-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.captcha-header h2 {
  margin: 6px 0 0;
  font-size: 28px;
  line-height: 1.2;
  font-family: var(--font-headline);
}

.captcha-eyebrow {
  margin: 0;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 12px;
  color: rgba(126, 95, 57, 0.76);
  font-family: var(--font-label);
  font-weight: 800;
}

.captcha-copy {
  margin: 16px 0 20px;
  line-height: 1.7;
  color: rgba(68, 80, 69, 0.82);
}

.captcha-stage {
  display: grid;
  gap: 18px;
}

.captcha-artboard {
  position: relative;
  overflow: hidden;
  max-width: 100%;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(212, 224, 206, 0.7), rgba(241, 225, 193, 0.52)),
    repeating-linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.6) 0,
      rgba(255, 255, 255, 0.6) 14px,
      rgba(255, 255, 255, 0.22) 14px,
      rgba(255, 255, 255, 0.22) 28px
    );
}

.puzzle-piece,
.target-slot {
  position: absolute;
  width: 44px;
  height: 44px;
  border-radius: 14px;
}

.puzzle-piece {
  border: 1px solid rgba(33, 48, 58, 0.08);
  background: rgba(255, 255, 255, 0.48);
}

.target-slot {
  border: 2px dashed rgba(93, 118, 96, 0.55);
  background: rgba(93, 118, 96, 0.08);
}

.target-guide {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, rgba(153, 107, 55, 0), rgba(153, 107, 55, 0.9), rgba(153, 107, 55, 0));
}

.slider-track {
  position: relative;
  height: 64px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(111, 126, 106, 0.12);
  overflow: hidden;
}

.slider-progress {
  position: absolute;
  inset: 0 auto 0 0;
  background: linear-gradient(90deg, rgba(111, 143, 112, 0.12), rgba(196, 160, 103, 0.18));
}

.slider-label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(79, 92, 78, 0.72);
  font-size: 14px;
  letter-spacing: 0.02em;
}

.slider-handle {
  position: absolute;
  inset: 8px auto 8px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #7f9d7e, #b88d57);
  color: #fff;
  font-size: 22px;
  cursor: grab;
  box-shadow: 0 12px 28px rgba(93, 118, 96, 0.24);
}

.slider-handle:disabled,
.ghost-button:disabled,
.secondary-button:disabled,
.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.captcha-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.ghost-button,
.secondary-button,
.primary-button {
  border-radius: 999px;
  padding: 10px 18px;
  font-size: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.ghost-button:hover,
.secondary-button:hover,
.primary-button:hover {
  transform: translateY(-1px);
}

.ghost-button,
.secondary-button {
  border: 1px solid rgba(111, 126, 106, 0.14);
  background: rgba(255, 255, 255, 0.78);
  color: var(--home-ink);
}

.primary-button {
  border: none;
  background: linear-gradient(135deg, #d7b37a, #ba8850);
  color: #1a130d;
  box-shadow: 0 12px 26px rgba(121, 93, 57, 0.18);
}

.captcha-error {
  margin: 16px 0 0;
  color: #8f5d51;
}

.captcha-loading {
  padding: 20px;
  border-radius: 18px;
  text-align: center;
  background: rgba(255, 255, 255, 0.56);
  color: rgba(82, 97, 92, 0.82);
}

@media (max-width: 640px) {
  .captcha-modal {
    padding: 20px;
  }

  .captcha-header {
    flex-direction: column;
  }

  .captcha-header h2 {
    font-size: 24px;
  }

  .captcha-actions {
    flex-direction: column;
  }
}
</style>
