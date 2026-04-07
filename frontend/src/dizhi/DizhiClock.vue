<template>
  <div class="dizhi-clock" :class="{ 'dizhi-clock--subtle': subtle }">
    <div class="dizhi-ring">
      <svg class="dizhi-track" viewBox="0 0 100 100" aria-hidden="true">
        <circle cx="50" cy="50" r="41.2" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="0.2" />
      </svg>
    </div>

    <div
      v-for="(branch, index) in branches"
      :key="branch.key"
      class="dizhi-orb-slot"
      :style="orbPosition(index)"
    >
      <div
        class="dz-orb"
        :class="[
          `dz-orb--${branch.key}`,
          { 'dz-orb--active': activeBranch === index && hoveredBranch === null }
        ]"
        @mouseenter="hoveredBranch = index"
        @mouseleave="hoveredBranch = null"
      >
        <span class="dizhi-orb-char">{{ branch.char }}</span>
      </div>
    </div>

    <div class="dizhi-center">
      <span class="dizhi-center-char" :class="`dz-text-${displayBranch.key}`">
        {{ displayBranch.char }}
      </span>
      <span class="dizhi-center-animal">{{ displayBranch.animal }}</span>
      <span class="dizhi-center-time">{{ displayBranch.timeLabel }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

interface Branch {
  key: string
  char: string
  animal: string
  startHour: number
  endHour: number
  timeLabel: string
}

const branches: Branch[] = [
  { key: 'zi', char: '子', animal: '鼠', startHour: 23, endHour: 1, timeLabel: '子时 23:00-01:00' },
  { key: 'chou', char: '丑', animal: '牛', startHour: 1, endHour: 3, timeLabel: '丑时 01:00-03:00' },
  { key: 'yin', char: '寅', animal: '虎', startHour: 3, endHour: 5, timeLabel: '寅时 03:00-05:00' },
  { key: 'mao', char: '卯', animal: '兔', startHour: 5, endHour: 7, timeLabel: '卯时 05:00-07:00' },
  { key: 'chen', char: '辰', animal: '龙', startHour: 7, endHour: 9, timeLabel: '辰时 07:00-09:00' },
  { key: 'si', char: '巳', animal: '蛇', startHour: 9, endHour: 11, timeLabel: '巳时 09:00-11:00' },
  { key: 'wu', char: '午', animal: '马', startHour: 11, endHour: 13, timeLabel: '午时 11:00-13:00' },
  { key: 'wei', char: '未', animal: '羊', startHour: 13, endHour: 15, timeLabel: '未时 13:00-15:00' },
  { key: 'shen', char: '申', animal: '猴', startHour: 15, endHour: 17, timeLabel: '申时 15:00-17:00' },
  { key: 'you', char: '酉', animal: '鸡', startHour: 17, endHour: 19, timeLabel: '酉时 17:00-19:00' },
  { key: 'xu', char: '戌', animal: '狗', startHour: 19, endHour: 21, timeLabel: '戌时 19:00-21:00' },
  { key: 'hai', char: '亥', animal: '猪', startHour: 21, endHour: 23, timeLabel: '亥时 21:00-23:00' }
]

defineProps<{
  subtle?: boolean
}>()

const activeBranch = ref(0)
const hoveredBranch = ref<number | null>(null)
let timer: ReturnType<typeof setTimeout> | null = null

const detectCurrentBranch = () => {
  const hour = new Date().getHours()
  for (let i = 0; i < branches.length; i++) {
    const branch = branches[i]
    if (branch.key === 'zi') {
      if (hour >= 23 || hour < 1) {
        activeBranch.value = i
        return
      }
    } else if (hour >= branch.startHour && hour < branch.endHour) {
      activeBranch.value = i
      return
    }
  }
}

const displayBranch = computed(() => {
  const index = hoveredBranch.value !== null ? hoveredBranch.value : activeBranch.value
  return branches[index] || branches[0]
})

const orbPosition = (index: number) => {
  const angleOffset = 0
  const angleDeg = angleOffset + index * 30
  const angleRad = (angleDeg * Math.PI) / 180
  const radius = 41.2
  const cx = 50 + radius * Math.cos(angleRad)
  const cy = 50 + radius * Math.sin(angleRad)

  return {
    left: `${cx}%`,
    top: `${cy}%`
  }
}

const scheduleNextUpdate = () => {
  const now = Date.now()
  const delay = 60000 - (now % 60000)
  timer = setTimeout(() => {
    detectCurrentBranch()
    scheduleNextUpdate()
  }, delay)
}

onMounted(() => {
  detectCurrentBranch()
  scheduleNextUpdate()
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped>
.dizhi-clock {
  --clock-size: clamp(17rem, 24vw, 22rem);
  --orb-size: clamp(3.4rem, calc(var(--clock-size) * 0.16), 4.5rem);
  --center-char-size: clamp(3rem, calc(var(--clock-size) * 0.19), 4.35rem);
  position: relative;
  width: min(100%, var(--clock-size));
  aspect-ratio: 1;
  margin: 0 auto;
  flex-shrink: 0;
}

.dizhi-ring {
  position: absolute;
  inset: 0;
}

.dizhi-track {
  width: 100%;
  height: 100%;
}

.dizhi-orb-slot {
  position: absolute;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.dizhi-orb-char {
  position: relative;
  z-index: 2;
  font-family: 'Manrope', sans-serif;
  pointer-events: none;
}

.dizhi-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
  gap: 0;
  width: 44%;
  text-align: center;
}

.dizhi-center-char {
  font-size: var(--center-char-size);
  font-weight: 800;
  font-family: var(--font-headline);
  line-height: 1;
  transition: color 0.3s;
}

.dizhi-center-animal {
  font-size: clamp(0.84rem, calc(var(--clock-size) * 0.045), 0.98rem);
  color: #666;
  font-family: var(--font-body);
  margin-top: 0.3rem;
}

.dizhi-center-time {
  font-size: clamp(0.68rem, calc(var(--clock-size) * 0.033), 0.8rem);
  color: #8f8f8f;
  font-family: var(--font-label);
  margin-top: 0.65rem;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.dizhi-clock--subtle {
  opacity: 0.74;
  transition: opacity 0.3s ease;
}

.dizhi-clock--subtle:hover {
  opacity: 1;
}

:deep(.dz-orb) {
  width: var(--orb-size);
  height: var(--orb-size);
  font-size: clamp(0.95rem, calc(var(--clock-size) * 0.05), 1.2rem);
}

@media (max-width: 1024px) {
  .dizhi-clock {
    --clock-size: min(100%, 24rem);
  }
}

@media (max-width: 767px) {
  .dizhi-clock {
    --clock-size: min(100%, 21rem);
  }
}
</style>
