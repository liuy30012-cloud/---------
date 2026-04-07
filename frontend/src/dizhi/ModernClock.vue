<template>
  <div class="digital-clock">
    <span class="digital-time">{{ timeString }}</span>
    <span class="digital-date">{{ dateString }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

const timeString = computed(() => {
  const h = String(now.value.getHours()).padStart(2, '0')
  const m = String(now.value.getMinutes()).padStart(2, '0')
  const s = String(now.value.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
})

const dateString = computed(() => {
  const y = now.value.getFullYear()
  const m = String(now.value.getMonth() + 1).padStart(2, '0')
  const d = String(now.value.getDate()).padStart(2, '0')
  const days = ['日', '一', '二', '三', '四', '五', '六']
  const w = days[now.value.getDay()]
  return `${y}.${m}.${d}  周${w}`
})

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.digital-clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 0;
}

.digital-time {
  font-family: 'Manrope', monospace;
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(40, 50, 70, 0.6);
  letter-spacing: 0.18em;
}

.digital-date {
  font-family: 'Inter', sans-serif;
  font-size: 0.7rem;
  color: rgba(100, 110, 130, 0.45);
  margin-top: 4px;
  letter-spacing: 0.1em;
}

@media (max-width: 1024px) {
  .digital-clock {
    display: none;
  }
}
</style>
