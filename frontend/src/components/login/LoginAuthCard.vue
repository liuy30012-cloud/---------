<template>
  <div class="login-container" :class="{ entered: props.hasEntered }">
    <div class="breathing-halo" aria-hidden="true"></div>

    <div v-if="props.isLogin" class="progress-ring-wrapper" :class="{ 'ring-complete': props.formProgress >= 100 }" aria-hidden="true">
      <svg class="progress-ring" viewBox="0 0 44 44">
        <circle class="progress-ring-bg" cx="22" cy="22" r="18" />
        <circle class="progress-ring-fill" cx="22" cy="22" r="18" :style="{ strokeDashoffset: props.progressOffset }" />
      </svg>
      <span class="progress-ring-text">{{ Math.round(props.formProgress) }}%</span>
    </div>

    <div class="greeting-section">
      <p class="greeting-text">{{ props.greetingEmoji }} {{ props.greetingText }}</p>
    </div>

    <div class="login-brand">
      <div class="badge-ring">
        <img src="/school-badge.png" :alt="t('login.schoolBadgeAlt')" class="brand-badge" width="80" height="80" />
      </div>
      <h2 class="brand-name">{{ t('login.brandName') }}</h2>
      <p class="brand-subtitle">{{ t('login.brandSubtitle') }}</p>
      <p class="brand-desc typing-text">
        <span
          v-for="(character, index) in props.typedChars"
          :key="`char-${index}`"
          class="typed-char"
          :style="{ animationDelay: `${index * 70}ms` }"
        >
          {{ character }}
        </span>
      </p>
    </div>

    <div
      ref="loginCardRef"
      class="login-card"
      :class="{ 'card-register': !props.isLogin, 'shake-error': props.shakeError }"
      :style="props.cardTiltStyle"
      @mousemove="handleCardMouseMove"
      @mouseleave="emit('card-mouse-leave')"
    >
      <div class="login-header">
        <div class="tab-switcher" role="tablist" :aria-label="t('login.tabSwitcherAria')">
          <button
            :class="['tab-btn', { active: props.isLogin }]"
            type="button"
            role="tab"
            :aria-selected="props.isLogin"
            @click="emit('switch-mode', true)"
          >
            <span class="material-symbols-outlined tab-icon" aria-hidden="true">login</span>
            {{ t('login.tabLogin') }}
          </button>
          <button
            :class="['tab-btn', { active: !props.isLogin }]"
            type="button"
            role="tab"
            :aria-selected="!props.isLogin"
            @click="emit('switch-mode', false)"
          >
            <span class="material-symbols-outlined tab-icon" aria-hidden="true">person_add</span>
            {{ t('login.tabRegister') }}
          </button>
          <div class="tab-indicator" :class="{ 'tab-right': !props.isLogin }" aria-hidden="true"></div>
        </div>
      </div>

      <Transition name="form-flip" mode="out-in">
        <form class="login-form" :key="props.isLogin ? 'login' : 'register'" @submit.prevent="emit('submit')">
          <FormInput
            v-model="props.formData.studentId"
            icon="badge"
            :label="t('login.studentId')"
            name="student_id"
            autocomplete="username"
            inputmode="numeric"
            :required="true"
            :is-valid="props.formData.studentId.length >= 4"
            :is-typing="props.typingField === 'studentId'"
            @type="event => emit('input-typing', event, 'studentId')"
          >
            <template #trailing>
              <span v-if="props.formData.studentId.length >= 4" class="input-check material-symbols-outlined">check_circle</span>
            </template>
          </FormInput>

          <FormInput
            v-if="!props.isLogin"
            v-model="props.formData.username"
            icon="person"
            :label="t('login.username')"
            name="username"
            autocomplete="nickname"
            :required="true"
          />

          <FormInput
            v-model="props.formData.password"
            :type="props.showPassword ? 'text' : 'password'"
            icon="lock"
            :label="props.isLogin ? t('login.loginPassword') : t('login.setPassword')"
            name="password"
            :autocomplete="props.isLogin ? 'current-password' : 'new-password'"
            :required="true"
            :is-typing="props.typingField === 'password'"
            @type="event => emit('input-typing', event, 'password')"
          >
            <template #trailing>
              <button type="button" class="toggle-pass" :aria-label="props.showPassword ? t('login.hidePassword') : t('login.showPassword')" @click="emit('toggle-password-visibility')">
                <span class="material-symbols-outlined toggle-eye" :class="{ 'eye-open': props.showPassword }">
                  {{ props.showPassword ? 'visibility_off' : 'visibility' }}
                </span>
              </button>
            </template>
          </FormInput>

          <div v-if="!props.isLogin && props.formData.password" class="password-strength" aria-live="polite">
            <div class="strength-bars">
              <div
                v-for="level in 4"
                :key="level"
                class="strength-bar"
                :class="{ active: props.passwordStrength >= level, [`level-${props.passwordStrength}`]: props.passwordStrength >= level }"
              ></div>
            </div>
            <span class="strength-text" :class="`level-${props.passwordStrength}`">{{ props.strengthLabel }}</span>
          </div>

          <FormInput
            v-if="!props.isLogin"
            v-model="props.formData.confirmPassword"
            type="password"
            icon="lock_reset"
            :label="t('login.confirmPassword')"
            name="confirm_password"
            autocomplete="new-password"
            :required="true"
          >
            <template #trailing>
              <span
                v-if="props.formData.confirmPassword"
                class="match-indicator material-symbols-outlined"
                :class="props.passwordsMatch ? 'match' : 'no-match'"
              >
                {{ props.passwordsMatch ? 'check_circle' : 'cancel' }}
              </span>
            </template>
          </FormInput>

          <div v-if="!props.isLogin" class="register-grid">
            <FormInput v-model="props.formData.email" type="email" icon="mail" :label="t('login.email')" name="email" autocomplete="email" />
            <FormInput v-model="props.formData.phone" type="tel" icon="phone_android" :label="t('login.phone')" name="phone" autocomplete="tel" inputmode="tel" />
          </div>

          <div v-if="props.isLogin" class="form-extras">
            <label class="checkbox-label">
              <input
                type="checkbox"
                class="visually-hidden"
                :checked="props.formData.rememberMe"
                @change="toggleRememberMe"
              />
              <div class="custom-check-box" :class="{ checked: props.formData.rememberMe }" aria-hidden="true">
                <span class="material-symbols-outlined check-icon">check</span>
              </div>
              <span>{{ t('login.rememberMe') }}</span>
            </label>
            <a href="#" class="forgot-link" @click.prevent="router.push({ name: 'ForgotPassword' })">{{ t('login.forgotPassword') }}</a>
          </div>

          <Transition name="toast">
            <div v-if="props.errorMessage" class="error-message" role="alert">
              <span class="material-symbols-outlined error-icon" aria-hidden="true">error</span>
              {{ props.errorMessage }}
            </div>
          </Transition>

          <Transition name="toast">
            <div v-if="props.successMessage" class="success-message" role="status" aria-live="polite">
              <span class="material-symbols-outlined success-icon" aria-hidden="true">check_circle</span>
              {{ props.successMessage }}
            </div>
          </Transition>

          <LibraryButton
            ref="submitBtnRef"
            type="primary"
            size="large"
            block
            :loading="props.isLoading"
            :loading-text="props.isLogin ? t('login.loggingIn') : t('login.submitting')"
            :style="props.magneticBtnStyle"
            @click="emit('submit')"
            @mouseenter="emit('submit-hover')"
            @mouseleave="emit('submit-leave')"
            @mousemove="handleSubmitMouseMove"
          >
            <span v-if="!props.isLoading" class="btn-content">
              <span class="material-symbols-outlined btn-icon" aria-hidden="true">{{ props.isLogin ? 'login' : 'person_add' }}</span>
              {{ props.isLogin ? t('login.enterLibrary') : t('login.createAccount') }}
            </span>
          </LibraryButton>

          <div v-if="props.isLogin && !props.isLoading" class="keyboard-hint">
            <span class="kbd">Enter</span>
            <span class="kbd-text">{{ t('login.enterToLogin') }}</span>
          </div>

          <div v-if="props.isLogin" class="quick-actions">
            <div class="divider-line"><span>{{ t('login.portal') }}</span></div>
            <div class="social-btns">
              <div v-for="(button, index) in props.socialButtons" :key="index" class="social-btn-wrapper">
                <button
                  type="button"
                  class="social-btn"
                  :title="t(button.titleKey)"
                  @mouseenter="button.hovered = true"
                  @mouseleave="button.hovered = false"
                >
                  <span class="material-symbols-outlined" aria-hidden="true">{{ button.icon }}</span>
                </button>
                <Transition name="tooltip">
                  <span v-if="button.hovered" class="social-tooltip">{{ t(button.titleKey) }}</span>
                </Transition>
              </div>
            </div>
          </div>
        </form>
      </Transition>

      <button class="seal-stamp" :class="{ 'seal-clicked': props.sealClicked }" type="button" :aria-label="t('login.sealAriaLabel')" @click.stop="emit('seal-click')">
        <span class="seal-text">{{ props.sealText }}</span>
      </button>

      <div class="card-light" :style="props.cardLightStyle"></div>
    </div>

    <p class="login-footer-text">{{ t('login.copyright') }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import FormInput from '../common/FormInput.vue'
import LibraryButton from '../common/LibraryButton.vue'

interface SocialButton {
  icon: string
  titleKey: string
  hovered: boolean
}

interface LoginFormModel {
  studentId: string
  username: string
  password: string
  confirmPassword: string
  email: string
  phone: string
  rememberMe: boolean
}

const props = defineProps<{
  hasEntered: boolean
  isLogin: boolean
  isLoading: boolean
  showPassword: boolean
  errorMessage: string
  successMessage: string
  shakeError: boolean
  typingField: string
  submitHovered: boolean
  sealClicked: boolean
  sealText: string
  socialButtons: SocialButton[]
  formData: LoginFormModel
  greetingText: string
  greetingEmoji: string
  typedChars: string[]
  passwordStrength: number
  strengthLabel: string
  passwordsMatch: boolean
  formProgress: number
  progressOffset: number
  magneticBtnStyle: Record<string, string | number | undefined>
  cardTiltStyle: Record<string, string | number | undefined>
  cardLightStyle: Record<string, string | number | undefined>
}>()

const emit = defineEmits<{
  (e: 'switch-mode', value: boolean): void
  (e: 'input-typing', event: Event, field: string): void
  (e: 'toggle-password-visibility'): void
  (e: 'submit'): void
  (e: 'submit-hover'): void
  (e: 'submit-leave'): void
  (e: 'submit-mouse-move', event: MouseEvent, element: HTMLButtonElement | null): void
  (e: 'card-mouse-move', event: MouseEvent, element: HTMLDivElement | null): void
  (e: 'card-mouse-leave'): void
  (e: 'seal-click'): void
}>()

const { t } = useI18n()
const loginCardRef = ref<HTMLDivElement | null>(null)
const submitBtnRef = ref<InstanceType<typeof LibraryButton> | null>(null)
const router = useRouter()

function toggleRememberMe() {
  props.formData.rememberMe = !props.formData.rememberMe
}

function handleCardMouseMove(event: MouseEvent) {
  emit('card-mouse-move', event, loginCardRef.value)
}

function handleSubmitMouseMove(event: MouseEvent) {
  emit('submit-mouse-move', event, submitBtnRef.value?.$el as HTMLButtonElement | null)
}
</script>

<style scoped src="./login-auth-card.css"></style>
