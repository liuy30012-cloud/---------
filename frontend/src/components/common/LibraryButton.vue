<template>
  <el-tooltip
    :disabled="!disabled || !disabledReason"
    :content="disabledReason"
    placement="top"
  >
    <span class="library-button-wrapper" :class="{ 'is-block': block }">
      <el-button
        :type="elType"
        :size="size"
        :disabled="disabled"
        :loading="isLoading && !loadingText"
        :aria-label="ariaLabel || undefined"
        :class="[
          'library-button',
          `library-button--${type}`,
          { 'library-button--loading': isLoading },
          { 'library-button--success': isSuccess },
          { 'library-button--error': isError },
        ]"
        @click="handleClick"
      >
        <span class="library-button__content">
          <span
            v-if="showIcon"
            class="library-button__icon"
            aria-hidden="true"
          >
            <component :is="currentIcon" />
          </span>
          <span class="library-button__text">
            <template v-if="isLoading && loadingText">{{ loadingText }}</template>
            <template v-else-if="isSuccess">{{ successText }}</template>
            <template v-else-if="isError">{{ errorText }}</template>
            <slot v-else />
          </span>
        </span>
      </el-button>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { ElButton, ElTooltip } from 'element-plus'

type ButtonType = 'primary' | 'secondary' | 'danger' | 'ghost'
type ButtonSize = 'small' | 'default' | 'large'
type ButtonState = 'idle' | 'loading' | 'success' | 'error'

const props = withDefaults(defineProps<{
  type?: ButtonType
  size?: ButtonSize
  loading?: boolean
  loadingText?: string
  disabled?: boolean
  disabledReason?: string
  icon?: Component
  successText?: string
  errorText?: string
  state?: ButtonState
  block?: boolean
  ariaLabel?: string
}>(), {
  type: 'primary',
  size: 'default',
  loading: false,
  loadingText: '',
  disabled: false,
  disabledReason: '',
  successText: '操作成功',
  errorText: '操作失败',
  state: 'idle',
  block: false,
  ariaLabel: '',
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const isLoading = computed(() => props.loading || props.state === 'loading')
const isSuccess = computed(() => props.state === 'success')
const isError = computed(() => props.state === 'error')

const elType = computed(() => {
  if (props.type === 'danger') return 'danger'
  if (props.type === 'primary') return 'primary'
  return 'default'
})

const currentIcon = computed(() => {
  if (isSuccess.value) return undefined
  if (isError.value) return undefined
  return props.icon
})

const showIcon = computed(() => {
  if (isSuccess.value || isError.value) return false
  return !!props.icon
})

function handleClick(event: MouseEvent) {
  if (props.disabled || isLoading.value) return
  emit('click', event)
}
</script>

<style scoped>
.library-button-wrapper {
  display: inline-flex;
}
.library-button-wrapper.is-block {
  display: flex;
  width: 100%;
}
.library-button-wrapper.is-block :deep(.el-button) {
  width: 100%;
}

.library-button {
  transition:
    transform var(--motion-duration-fast, 0.24s) ease,
    box-shadow var(--motion-duration-fast, 0.24s) ease,
    filter var(--motion-duration-fast, 0.24s) ease,
    opacity var(--motion-duration-fast, 0.24s) ease !important;
}

.library-button:hover:not(:disabled):not(.library-button--loading):not(.library-button--success):not(.library-button--error) {
  transform: translateY(-2px) !important;
  filter: brightness(1.1);
}

.library-button:active:not(:disabled):not(.library-button--loading) {
  transform: scale(0.97) translateY(0) !important;
}

/* primary */
.library-button--primary:not(:disabled):not(.library-button--success):not(.library-button--error) {
  background: linear-gradient(
    135deg,
    var(--el-color-primary) 0%,
    var(--el-color-primary-dark-2) 100%
  ) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* secondary */
.library-button--secondary:not(:disabled) {
  background: rgba(255, 255, 255, 0.9) !important;
  border: 1px solid rgba(115, 124, 129, 0.16) !important;
  color: var(--el-text-color-primary, #1b2821) !important;
}

/* ghost */
.library-button--ghost {
  background: transparent !important;
  border-color: transparent !important;
  color: var(--el-text-color-regular, #b4a692) !important;
  box-shadow: none !important;
}
.library-button--ghost:hover:not(:disabled) {
  background: var(--el-color-primary-light-9, rgba(199, 160, 103, 0.08)) !important;
}

/* danger */
.library-button--danger:not(:disabled):not(.library-button--success):not(.library-button--error) {
  background: var(--el-color-danger) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* disabled */
.library-button:disabled {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
}

/* loading */
.library-button--loading {
  cursor: wait !important;
  pointer-events: none;
}

/* success flash */
@keyframes btn-success-flash {
  0% { box-shadow: 0 0 0 0 rgba(91, 140, 90, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(91, 140, 90, 0); }
  100% { box-shadow: 0 0 0 0 rgba(91, 140, 90, 0); }
}
.library-button--success {
  animation: btn-success-flash 0.6s ease-out !important;
}
.library-button--success:not(:disabled) {
  background: var(--el-color-success) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* error shake */
@keyframes btn-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-4px); }
  40% { transform: translateX(4px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(3px); }
}
.library-button--error {
  animation: btn-shake 0.4s ease-out !important;
}
.library-button--error:not(:disabled) {
  background: var(--el-color-danger) !important;
  border-color: transparent !important;
  color: #fff !important;
}

.library-button__content {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
.library-button__icon {
  display: inline-flex;
  font-size: 1.1em;
}

@media (prefers-reduced-motion: reduce) {
  .library-button--success {
    animation: none !important;
  }
  .library-button--error {
    animation: none !important;
  }
}
</style>
