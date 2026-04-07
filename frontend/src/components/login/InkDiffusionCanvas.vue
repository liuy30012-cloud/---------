<template>
  <!-- Layer 1: Ink diffusion canvas -->
  <canvas ref="inkCanvas" class="ink-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const inkCanvas = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D | null = null
let inkDrops: InkDrop[] = []
let animationId: number | null = null

interface InkDrop {
  x: number
  y: number
  radius: number
  maxRadius: number
  opacity: number
  speed: number
}

onMounted(() => {
  if (!inkCanvas.value) return

  const canvas = inkCanvas.value
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  ctx = canvas.getContext('2d')

  // 创建初始墨滴
  for (let i = 0; i < 5; i++) {
    createInkDrop(Math.random() * canvas.width, Math.random() * canvas.height)
  }

  animate()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})

const createInkDrop = (x: number, y: number) => {
  inkDrops.push({
    x,
    y,
    radius: 0,
    maxRadius: 80 + Math.random() * 120,
    opacity: 0.03 + Math.random() * 0.02,
    speed: 0.3 + Math.random() * 0.5
  })
}

const animate = () => {
  if (!ctx || !inkCanvas.value) return

  ctx.clearRect(0, 0, inkCanvas.value.width, inkCanvas.value.height)

  inkDrops = inkDrops.filter(drop => {
    drop.radius += drop.speed

    if (drop.radius < drop.maxRadius) {
      ctx!.beginPath()
      ctx!.arc(drop.x, drop.y, drop.radius, 0, Math.PI * 2)
      ctx!.fillStyle = `rgba(45, 55, 72, ${drop.opacity * (1 - drop.radius / drop.maxRadius)})`
      ctx!.fill()
      return true
    }
    return false
  })

  // 随机添加新墨滴
  if (Math.random() < 0.01 && inkDrops.length < 8) {
    createInkDrop(
      Math.random() * inkCanvas.value.width,
      Math.random() * inkCanvas.value.height
    )
  }

  animationId = requestAnimationFrame(animate)
}

const handleResize = () => {
  if (!inkCanvas.value) return
  inkCanvas.value.width = window.innerWidth
  inkCanvas.value.height = window.innerHeight
}
</script>

<style scoped>
.ink-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
</style>
