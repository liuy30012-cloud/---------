import { ref, computed, onUnmounted } from 'vue'

export function useInkCanvas() {
  const inkCanvas = ref<HTMLCanvasElement | null>(null)
  let animFrameId = 0
  let inkResizeHandler: (() => void) | null = null

  interface InkBlob {
    x: number; y: number; radius: number; maxRadius: number
    opacity: number; speed: number; driftX: number; phase: number
  }

  function initInkCanvas() {
    const canvas = inkCanvas.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    function resize() {
      canvas!.width = window.innerWidth
      canvas!.height = window.innerHeight
    }
    inkResizeHandler = resize
    resize()
    window.addEventListener('resize', inkResizeHandler)

    const blobs: InkBlob[] = []
    for (let i = 0; i < 5; i++) {
      blobs.push({
        x: 0.1 + Math.random() * 0.8, y: 0.7 + Math.random() * 0.25,
        radius: 0, maxRadius: 80 + Math.random() * 120,
        opacity: 0.15 + Math.random() * 0.2,
        speed: 0.15 + Math.random() * 0.25,
        driftX: (Math.random() - 0.5) * 0.3,
        phase: Math.random() * Math.PI * 2
      })
    }

    let time = 0
    function draw() {
      const w = canvas!.width
      const h = canvas!.height
      ctx!.clearRect(0, 0, w, h)
      time += 0.008

      for (const blob of blobs) {
        blob.radius += blob.speed
        if (blob.radius > blob.maxRadius) {
          blob.radius = 0
          blob.x = 0.1 + Math.random() * 0.8
          blob.y = 0.7 + Math.random() * 0.25
          blob.maxRadius = 80 + Math.random() * 120
          blob.phase = Math.random() * Math.PI * 2
        }

        const cx = blob.x * w + Math.sin(time + blob.phase) * 30 * blob.driftX
        const cy = blob.y * h
        const r = blob.radius
        const lifeRatio = r / blob.maxRadius
        const alpha = blob.opacity * (1 - lifeRatio * lifeRatio)

        for (let j = 0; j < 5; j++) {
          const angle = (j / 5) * Math.PI * 2 + blob.phase
          const offsetX = Math.cos(angle) * r * 0.2 * Math.sin(time * 2 + j)
          const offsetY = Math.sin(angle) * r * 0.15 * Math.cos(time * 1.5 + j)

          const grad = ctx!.createRadialGradient(
            cx + offsetX, cy + offsetY, 0,
            cx + offsetX, cy + offsetY, r * (0.6 + j * 0.1)
          )
          grad.addColorStop(0, `rgba(15, 12, 25, ${alpha * 0.7})`)
          grad.addColorStop(0.4, `rgba(25, 20, 40, ${alpha * 0.4})`)
          grad.addColorStop(0.7, `rgba(45, 35, 65, ${alpha * 0.15})`)
          grad.addColorStop(1, `rgba(60, 50, 80, 0)`)

          ctx!.beginPath()
          ctx!.arc(cx + offsetX, cy + offsetY, r * (0.6 + j * 0.1), 0, Math.PI * 2)
          ctx!.fillStyle = grad
          ctx!.fill()
        }
      }

      animFrameId = requestAnimationFrame(draw)
    }
    draw()
  }

  // ===== Brush Stroke Canvas =====
  const brushCanvas = ref<HTMLCanvasElement | null>(null)
  let brushAnimId = 0
  let brushResizeHandler: (() => void) | null = null
  const brushPoints: { x: number; y: number; age: number; size: number }[] = []

  function initBrushCanvas() {
    const canvas = brushCanvas.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    function resize() {
      canvas!.width = window.innerWidth
      canvas!.height = window.innerHeight
    }
    brushResizeHandler = resize
    resize()
    window.addEventListener('resize', brushResizeHandler)

    function draw() {
      ctx!.clearRect(0, 0, canvas!.width, canvas!.height)

      for (let i = brushPoints.length - 1; i >= 0; i--) {
        brushPoints[i].age += 0.02
        if (brushPoints[i].age >= 1) brushPoints.splice(i, 1)
      }

      if (brushPoints.length > 1) {
        for (let i = 1; i < brushPoints.length; i++) {
          const p0 = brushPoints[i - 1]
          const p1 = brushPoints[i]
          const alpha = Math.max(0, 0.12 * (1 - p1.age))
          const width = p1.size * (1 - p1.age * 0.6)

          ctx!.beginPath()
          ctx!.moveTo(p0.x, p0.y)
          const mx = (p0.x + p1.x) / 2
          const my = (p0.y + p1.y) / 2
          ctx!.quadraticCurveTo(p0.x, p0.y, mx, my)
          ctx!.strokeStyle = `rgba(180, 160, 120, ${alpha})`
          ctx!.lineWidth = width
          ctx!.lineCap = 'round'
          ctx!.lineJoin = 'round'
          ctx!.stroke()
        }
      }

      brushAnimId = requestAnimationFrame(draw)
    }
    draw()
  }

  function addBrushPoint(x: number, y: number) {
    const size = 1.5 + Math.random() * 2.5
    brushPoints.push({ x, y, age: 0, size })
    if (brushPoints.length > 80) brushPoints.splice(0, brushPoints.length - 80)
  }

  // Mouse glow (Vue reflow bypass)
  function updateMouseGlow(clientX: number, clientY: number, glowEl: HTMLElement | null) {
    if (glowEl) {
      glowEl.style.transform = `translate3d(${clientX}px, ${clientY}px, 0)`
    }
  }

  // 3D Card Tilt
  const cardTiltX = ref(0)
  const cardTiltY = ref(0)
  const cardLightX = ref(50)
  const cardLightY = ref(50)
  const cardTiltStyle = computed(() => ({
    transform: `perspective(800px) rotateX(${cardTiltX.value}deg) rotateY(${cardTiltY.value}deg)`,
  }))
  const cardLightStyle = computed(() => ({
    background: `radial-gradient(circle at ${cardLightX.value}% ${cardLightY.value}%, rgba(200,170,110,0.06) 0%, transparent 60%)`,
  }))

  function onCardMouseMove(e: MouseEvent, cardEl: HTMLElement | null) {
    if (!cardEl) return
    const rect = cardEl.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width
    const y = (e.clientY - rect.top) / rect.height
    cardTiltX.value = (0.5 - y) * 6
    cardTiltY.value = (x - 0.5) * 6
    cardLightX.value = x * 100
    cardLightY.value = y * 100
  }

  function onCardMouseLeave() {
    cardTiltX.value = 0; cardTiltY.value = 0
    cardLightX.value = 50; cardLightY.value = 50
  }

  function cleanup() {
    if (animFrameId) cancelAnimationFrame(animFrameId)
    if (brushAnimId) cancelAnimationFrame(brushAnimId)
    if (inkResizeHandler) window.removeEventListener('resize', inkResizeHandler)
    if (brushResizeHandler) window.removeEventListener('resize', brushResizeHandler)
  }

  return {
    inkCanvas, brushCanvas,
    updateMouseGlow,
    cardTiltStyle, cardLightStyle,
    initInkCanvas, initBrushCanvas, addBrushPoint,
    onCardMouseMove, onCardMouseLeave, cleanup,
  }
}
