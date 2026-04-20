<template>
  <slot v-if="!hasError" />
  <ErrorPage
    v-else
    :title="boundaryError?.title"
    :message="boundaryError?.message"
    :details="boundaryError?.details"
    :show-retry="showRetry"
    :retrying="retrying"
    @retry="handleRetry"
    @back="goBack"
    @home="goHome"
  />
</template>

<script setup lang="ts">
import { computed, onErrorCaptured, ref } from 'vue'
import { useRouter } from 'vue-router'
import ErrorPage from './ErrorPage.vue'
import { clearActiveError, useError } from '../../composables/useError'
import { errorCenter } from '../../services/ErrorCenter'

const props = withDefaults(defineProps<{
  retryable?: boolean
}>(), {
  retryable: true,
})

const emit = defineEmits<{
  retry: []
}>()

const router = useRouter()
const retrying = ref(false)
const localErrorId = ref<string | null>(null)
const { activeError } = useError()

const boundaryError = computed(() => {
  const current = activeError.value
  if (!current || localErrorId.value !== current.id) {
    return null
  }
  return current
})

const hasError = computed(() => boundaryError.value !== null)
const showRetry = computed(() => props.retryable && Boolean(boundaryError.value?.retryable))

onErrorCaptured((error, instance, info) => {
  const captured = errorCenter.capture(error as Error, {
    page: window.location.pathname,
    info,
    component: instance?.$options?.name || instance?.$options?.__name || 'UnknownComponent',
    retryable: true,
  })
  localErrorId.value = captured.id
  return false
})

function handleRetry() {
  retrying.value = true
  clearActiveError()
  localErrorId.value = null
  emit('retry')
  window.requestAnimationFrame(() => {
    retrying.value = false
  })
}

function goBack() {
  router.back()
}

function goHome() {
  router.push({ name: 'Home' }).catch(() => {})
}
</script>
