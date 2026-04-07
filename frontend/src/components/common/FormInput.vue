<template>
  <div class="form-group" :class="{ focused: isFocused, 'has-value': !!modelValue, 'input-valid': isValid }">
    <div class="input-wrapper">
      <span class="input-icon material-symbols-outlined" aria-hidden="true">{{ icon }}</span>
      <input
        :type="type"
        class="form-input"
        placeholder=" "
        :required="required"
        :value="modelValue"
        :name="name"
        :autocomplete="autocomplete"
        :inputmode="resolvedInputmode"
        :spellcheck="spellcheck"
        :aria-label="label"
        :aria-invalid="invalidState"
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
      />
      <label class="float-label">{{ label }}</label>
      <slot name="trailing"></slot>
      <div class="input-wash"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue'
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: [String, Number],
  icon: String,
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  name: { type: String, default: '' },
  autocomplete: { type: String, default: '' },
  inputmode: {
    type: String as PropType<'none' | 'text' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal' | 'search' | ''>,
    default: '',
  },
  spellcheck: { type: Boolean, default: false },
  required: Boolean,
  isValid: Boolean,
  isTyping: Boolean,
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur', 'type'])

const isFocused = ref(false)
const invalidState = computed(() => (props.required && !props.modelValue && !isFocused.value ? 'false' : undefined))
const resolvedInputmode = computed(() => props.inputmode || undefined)

const onFocus = () => {
  isFocused.value = true
  emit('focus')
}

const onBlur = () => {
  isFocused.value = false
  emit('blur')
}

const onInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  emit('update:modelValue', target.value)
  emit('type', e)
}
</script>

<style scoped>
.form-group {
  position: relative;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  z-index: 2;
  pointer-events: none;
  font-size: 20px;
  color: rgba(104, 92, 71, 0.66);
  transition: color 0.25s ease, transform 0.25s ease;
}

.form-input {
  width: 100%;
  padding: 18px 16px 12px 48px;
  border-radius: 16px;
  border: 1px solid rgba(110, 122, 102, 0.16);
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.92) 0%, rgba(246, 241, 231, 0.88) 100%);
  color: var(--home-ink);
  font-size: 14px;
  line-height: 1.4;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.float-label {
  position: absolute;
  left: 48px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  font-size: 14px;
  color: rgba(107, 95, 75, 0.62);
  transition: top 0.22s ease, transform 0.22s ease, font-size 0.22s ease, color 0.22s ease;
}

.form-group.focused .float-label,
.form-group.has-value .float-label {
  top: 7px;
  transform: translateY(0);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: rgba(118, 91, 56, 0.88);
}

.form-group.focused .form-input {
  border-color: rgba(113, 138, 107, 0.34);
  background: linear-gradient(180deg, rgba(255, 253, 249, 0.97) 0%, rgba(248, 244, 236, 0.92) 100%);
  box-shadow:
    0 0 0 4px rgba(133, 160, 131, 0.12),
    0 14px 28px rgba(50, 59, 50, 0.08);
}

.form-input:focus-visible {
  outline: 2px solid var(--home-focus);
  outline-offset: 3px;
}

.form-group.focused .input-icon {
  color: rgba(86, 114, 80, 0.9);
  transform: translateY(-1px);
}

.input-wash {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, transparent 36%),
    radial-gradient(circle at top right, rgba(201, 170, 122, 0.12) 0%, transparent 30%);
  opacity: 0.8;
}

.form-input::placeholder {
  color: rgba(96, 106, 95, 0.36);
}
</style>
