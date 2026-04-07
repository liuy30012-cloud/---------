import { ref, computed, onMounted, onUnmounted } from 'vue'

interface ParticleStyle { id: number; style: Record<string, string> }

// ---- Audio ----
let audioCtx: AudioContext | null = null
const getAudioCtx = () => {
  if (!audioCtx) audioCtx = new AudioContext()
  return audioCtx
}

export function useSuperButton() {
  // ---- 3D Tilt + Spotlight ----
  const btnTiltX = ref(0)
  const btnTiltY = ref(0)
  const btnSpotX = ref(50)
  const btnSpotY = ref(50)
  const btnSpotOpacity = ref(0)

  const btnSpotlightStyle = computed(() => ({
    background: `radial-gradient(circle at ${btnSpotX.value}% ${btnSpotY.value}%, rgba(255,255,255,0.2) 0%, transparent 60%)`,
    opacity: String(btnSpotOpacity.value)
  }))

  // ---- Magnetic ----
  const btnMagX = ref(0)
  const btnMagY = ref(0)
  const btnIsHovered = ref(false)

  const onMagneticMove = (e: MouseEvent) => {
    const area = e.currentTarget as HTMLElement
    const btn = area.querySelector('.view-all-btn') as HTMLElement
    if (!btn) return
    const rect = btn.getBoundingClientRect()
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2
    const dx = e.clientX - cx
    const dy = e.clientY - cy
    const dist = Math.sqrt(dx * dx + dy * dy)
    const maxDist = 150
    if (dist < maxDist) {
      const strength = 1 - dist / maxDist
      btnMagX.value = dx * strength * 0.3
      btnMagY.value = dy * strength * 0.2
    } else {
      btnMagX.value = 0
      btnMagY.value = 0
    }
  }

  const onMagneticLeave = () => {
    btnMagX.value = 0
    btnMagY.value = 0
  }

  // ---- Ripples ----
  const btnRipples = ref<ParticleStyle[]>([])
  let rippleId = 0

  // ---- Burst particles ----
  const btnBursts = ref<ParticleStyle[]>([])

  const onBtnClick = (e: MouseEvent) => {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const id = ++rippleId
    btnRipples.value.push({
      id,
      style: { left: `${x}px`, top: `${y}px`, animation: 'btnRippleExpand 0.7s ease-out forwards' }
    })
    for (let i = 0; i < 8; i++) {
      const burstId = ++rippleId
      const angle = (Math.PI * 2 * i) / 8 + (Math.random() - 0.5) * 0.5
      const dist = 40 + Math.random() * 60
      const tx = Math.cos(angle) * dist
      const ty = Math.sin(angle) * dist
      const size = 2 + Math.random() * 3
      btnBursts.value.push({
        id: burstId,
        style: {
          left: `${x}px`, top: `${y}px`, width: `${size}px`, height: `${size}px`,
          '--burst-tx': `${tx}px`, '--burst-ty': `${ty}px`,
          animation: 'burstFly 0.6s ease-out forwards'
        } as Record<string, string>
      })
      setTimeout(() => { btnBursts.value = btnBursts.value.filter(b => b.id !== burstId) }, 600)
    }
    setTimeout(() => { btnRipples.value = btnRipples.value.filter(r => r.id !== id) }, 700)
  }

  // ---- Trail particles ----
  const btnTrails = ref<ParticleStyle[]>([])
  let trailId = 0
  let lastTrailTime = 0

  const spawnTrail = (x: number, y: number) => {
    const now = Date.now()
    if (now - lastTrailTime < 50) return
    lastTrailTime = now
    const id = ++trailId
    const size = 2 + Math.random() * 3
    btnTrails.value.push({ id, style: { left: `${x}px`, top: `${y}px`, width: `${size}px`, height: `${size}px` } })
    setTimeout(() => { btnTrails.value = btnTrails.value.filter(t => t.id !== id) }, 600)
  }

  // ---- Press & Charge ----
  const btnPressing = ref(false)
  const btnChargeProgress = ref(0)
  let chargeInterval: ReturnType<typeof setInterval> | null = null

  const onBtnMouseDown = () => {
    btnPressing.value = true
    btnChargeProgress.value = 0
    chargeInterval = setInterval(() => {
      if (btnChargeProgress.value >= 100) {
        if (chargeInterval) { clearInterval(chargeInterval); chargeInterval = null }
        return
      }
      btnChargeProgress.value = Math.min(btnChargeProgress.value + 2, 100)
    }, 30)
  }

  const onBtnMouseUp = () => {
    btnPressing.value = false
    if (chargeInterval) { clearInterval(chargeInterval); chargeInterval = null }
    btnChargeProgress.value = 0
  }

  // ---- Shockwave ----
  const btnShockwaves = ref<ParticleStyle[]>([])
  let shockId = 0

  const onBtnDblClick = (e: MouseEvent) => {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const id = ++shockId
    btnShockwaves.value.push({ id, style: { left: `${x}px`, top: `${y}px` } })
    setTimeout(() => { btnShockwaves.value = btnShockwaves.value.filter(s => s.id !== id) }, 800)
  }

  // ---- Sound effects ----
  const playHoverSound = () => {
    try {
      const ctx = getAudioCtx()
      const osc = ctx.createOscillator(); const gain = ctx.createGain()
      osc.connect(gain); gain.connect(ctx.destination)
      osc.type = 'sine'
      osc.frequency.setValueAtTime(800, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(1200, ctx.currentTime + 0.08)
      gain.gain.setValueAtTime(0.04, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15)
      osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.15)
    } catch {}
  }
  const playClickSound = () => {
    try {
      const ctx = getAudioCtx()
      const osc = ctx.createOscillator(); const gain = ctx.createGain()
      osc.connect(gain); gain.connect(ctx.destination)
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(400, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(100, ctx.currentTime + 0.12)
      gain.gain.setValueAtTime(0.06, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2)
      osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.2)
    } catch {}
  }

  // ---- Ambient particles ----
  const ambientParticles = ref<ParticleStyle[]>([])
  let ambientId = 0
  let ambientInterval: ReturnType<typeof setInterval> | null = null
  const MAX_AMBIENT_PARTICLES = 20

  const startAmbientParticles = () => {
    if (ambientInterval) return
    ambientInterval = setInterval(() => {
      if (ambientParticles.value.length >= MAX_AMBIENT_PARTICLES) return
      const id = ++ambientId
      const x = Math.random() * 100
      const duration = 3 + Math.random() * 3
      const size = 1 + Math.random() * 2
      const delay = Math.random() * 0.5
      ambientParticles.value.push({
        id, style: { left: `${x}%`, bottom: '-4px', width: `${size}px`, height: `${size}px`, animationDuration: `${duration}s`, animationDelay: `${delay}s` }
      })
      setTimeout(() => { ambientParticles.value = ambientParticles.value.filter(p => p.id !== id) }, (duration + delay) * 1000)
    }, 400)
  }

  const stopAmbientParticles = () => {
    if (ambientInterval) { clearInterval(ambientInterval); ambientInterval = null }
  }

  // ---- Edge glow ----
  const btnEdgeGlowStyle = ref<Record<string, string>>({})
  const updateEdgeGlow = (xPct: number, yPct: number) => {
    const angle = Math.atan2(yPct - 50, xPct - 50) * (180 / Math.PI) + 180
    btnEdgeGlowStyle.value = {
      background: `conic-gradient(from ${angle - 30}deg, transparent 0%, rgba(255,255,255,0.5) 8%, rgba(255,255,255,0.8) 15%, rgba(255,255,255,0.5) 22%, transparent 30%, transparent 100%)`,
    }
  }

  // ---- Scroll parallax ----
  const scrollTiltX = ref(0)
  let lastScrollY = 0
  let scrollTicking = false
  const onScrollParallax = () => {
    if (!scrollTicking) {
      requestAnimationFrame(() => {
        const delta = window.scrollY - lastScrollY
        scrollTiltX.value = Math.max(-3, Math.min(3, delta * 0.3))
        lastScrollY = window.scrollY
        setTimeout(() => { scrollTiltX.value = 0 }, 300)
        scrollTicking = false
      })
      scrollTicking = true
    }
  }

  // ---- Keyboard focus ----
  const btnFocused = ref(false)
  const onBtnFocus = () => { btnFocused.value = true }
  const onBtnBlur = () => { btnFocused.value = false }

  // ---- Context menu vortex ----
  const btnVortexParticles = ref<ParticleStyle[]>([])
  let vortexId = 0

  const onBtnContextMenu = (e: MouseEvent) => {
    e.preventDefault()
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const cx = e.clientX - rect.left
    const cy = e.clientY - rect.top
    for (let i = 0; i < 12; i++) {
      const id = ++vortexId
      const angle = (Math.PI * 2 * i) / 12
      const radius = 50
      const startX = cx + Math.cos(angle) * radius
      const startY = cy + Math.sin(angle) * radius
      const delay = i * 40
      btnVortexParticles.value.push({
        id, style: { left: `${startX}px`, top: `${startY}px`, '--vortex-cx': `${cx - startX}px`, '--vortex-cy': `${cy - startY}px`, animationDelay: `${delay}ms` } as Record<string, string>
      })
      setTimeout(() => { btnVortexParticles.value = btnVortexParticles.value.filter(p => p.id !== id) }, 800 + delay)
    }
    try {
      const ctx = getAudioCtx()
      const osc = ctx.createOscillator(); const gain = ctx.createGain()
      osc.connect(gain); gain.connect(ctx.destination)
      osc.type = 'sawtooth'
      osc.frequency.setValueAtTime(200, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(800, ctx.currentTime + 0.3)
      gain.gain.setValueAtTime(0.03, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35)
      osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.35)
    } catch {}
  }

  // ---- Ghost afterimages ----
  const btnGhosts = ref<ParticleStyle[]>([])
  let ghostId = 0
  let lastGhostTime = 0

  const spawnGhost = () => {
    const now = Date.now()
    if (now - lastGhostTime < 100) return
    if (Math.abs(btnMagX.value) < 2 && Math.abs(btnMagY.value) < 2) return
    lastGhostTime = now
    const id = ++ghostId
    btnGhosts.value.push({ id, style: { transform: `translate(${-btnMagX.value * 0.5}px, ${-btnMagY.value * 0.5}px)` } })
    setTimeout(() => { btnGhosts.value = btnGhosts.value.filter(g => g.id !== id) }, 400)
  }

  // ---- Text glitch ----
  const btnGlitchText = ref('')
  const btnShowGlitch = ref(false)
  const glitchChars = '░▒▓█▀▄▌▐│┤╡╢╣╗╝║╚╔╩╦╠═╬'
  let glitchInterval: ReturnType<typeof setInterval> | null = null

  const triggerTextGlitch = (targetText: string) => {
    if (glitchInterval) clearInterval(glitchInterval)
    btnShowGlitch.value = true
    let frame = 0
    const maxFrames = 6
    glitchInterval = setInterval(() => {
      let result = ''
      for (let i = 0; i < targetText.length; i++) {
        if (frame >= maxFrames || Math.random() < frame / maxFrames) {
          result += targetText[i]
        } else {
          result += glitchChars[Math.floor(Math.random() * glitchChars.length)]
        }
      }
      btnGlitchText.value = result
      frame++
      if (frame > maxFrames) {
        if (glitchInterval) { clearInterval(glitchInterval); glitchInterval = null }
        setTimeout(() => { btnShowGlitch.value = false }, 100)
      }
    }, 50)
  }

  // ---- Heartbeat ----
  const btnHeartbeat = ref(false)
  let heartbeatInterval: ReturnType<typeof setInterval> | null = null

  const startHeartbeat = () => {
    if (heartbeatInterval) return
    heartbeatInterval = setInterval(() => {
      if (btnIsHovered.value) return
      btnHeartbeat.value = true
      setTimeout(() => { btnHeartbeat.value = false }, 600)
    }, 4000)
  }

  // ---- Elastic stretch ----
  const btnStretchX = ref(1)
  const btnStretchY = ref(1)
  let lastMouseX = 0
  let lastMouseY = 0
  let lastMoveTime = 0

  const updateStretch = (e: MouseEvent) => {
    const now = Date.now()
    const dt = Math.max(now - lastMoveTime, 1)
    const vx = (e.clientX - lastMouseX) / dt
    const vy = (e.clientY - lastMouseY) / dt
    const speed = Math.sqrt(vx * vx + vy * vy)
    if (speed > 0.3) {
      const stretchAmount = Math.min(speed * 0.03, 0.08)
      const angle = Math.atan2(vy, vx)
      const cosA = Math.abs(Math.cos(angle))
      const sinA = Math.abs(Math.sin(angle))
      btnStretchX.value = 1 + stretchAmount * cosA
      btnStretchY.value = 1 + stretchAmount * sinA
    }
    lastMouseX = e.clientX
    lastMouseY = e.clientY
    lastMoveTime = now
  }

  const resetStretch = () => { btnStretchX.value = 1; btnStretchY.value = 1 }

  // ---- Gravity bounce ----
  const btnBouncing = ref(false)
  const triggerGravityBounce = () => {
    btnBouncing.value = true
    setTimeout(() => { btnBouncing.value = false }, 500)
  }

  // ---- Warmth ----
  const btnWarmth = ref(0)
  let warmthTimer: ReturnType<typeof setInterval> | null = null

  const startWarmth = () => {
    btnWarmth.value = 0
    warmthTimer = setInterval(() => {
      if (btnWarmth.value >= 1) {
        if (warmthTimer) { clearInterval(warmthTimer); warmthTimer = null }
        return
      }
      btnWarmth.value = Math.min(1, btnWarmth.value + 0.02)
    }, 50)
  }

  const stopWarmth = () => {
    if (warmthTimer) { clearInterval(warmthTimer); warmthTimer = null }
    btnWarmth.value = 0
  }

  const btnWarmthStyle = computed(() => {
    if (btnWarmth.value <= 0) return {}
    const w = btnWarmth.value
    return {
      boxShadow: `inset 0 0 ${20 * w}px ${5 * w}px rgba(255, ${180 + 75 * w}, ${100 * w}, ${0.15 * w}), 0 0 ${30 * w}px ${8 * w}px rgba(255, ${200 * w}, ${80 * w}, ${0.1 * w})`,
    }
  })

  // ---- Triple-click confetti ----
  let clickTimestamps: number[] = []
  const btnConfetti = ref<ParticleStyle[]>([])
  let confettiId = 0
  const confettiColors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9']

  const spawnConfetti = (e: MouseEvent) => {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const cx = e.clientX - rect.left
    const cy = e.clientY - rect.top
    for (let i = 0; i < 20; i++) {
      const id = ++confettiId
      const angle = (Math.PI * 2 * i) / 20 + (Math.random() - 0.5) * 0.5
      const velocity = 60 + Math.random() * 80
      const dx = Math.cos(angle) * velocity
      const dy = Math.sin(angle) * velocity
      const color = confettiColors[Math.floor(Math.random() * confettiColors.length)]
      const size = 3 + Math.random() * 4
      const rotation = Math.random() * 720
      btnConfetti.value.push({
        id, style: {
          left: `${cx}px`, top: `${cy}px`, width: `${size}px`, height: `${size}px`, background: color,
          '--confetti-dx': `${dx}px`, '--confetti-dy': `${dy}px`, '--confetti-rot': `${rotation}deg`,
        } as Record<string, string>
      })
      setTimeout(() => { btnConfetti.value = btnConfetti.value.filter(c => c.id !== id) }, 1000)
    }
    try {
      const ctx = getAudioCtx()
      const osc = ctx.createOscillator(); const gain = ctx.createGain()
      osc.connect(gain); gain.connect(ctx.destination)
      osc.type = 'sine'
      osc.frequency.setValueAtTime(523, ctx.currentTime)
      osc.frequency.setValueAtTime(659, ctx.currentTime + 0.1)
      osc.frequency.setValueAtTime(784, ctx.currentTime + 0.2)
      gain.gain.setValueAtTime(0.05, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4)
      osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.4)
    } catch {}
  }

  const checkTripleClick = (e: MouseEvent) => {
    const now = Date.now()
    clickTimestamps.push(now)
    clickTimestamps = clickTimestamps.filter(t => now - t < 600)
    if (clickTimestamps.length >= 3) { clickTimestamps = []; spawnConfetti(e) }
  }

  // ---- Motion blur ----
  const btnMotionBlur = ref('none')

  const updateMotionBlur = (e: MouseEvent) => {
    const now = Date.now()
    const dt = Math.max(now - lastMoveTime, 1)
    const vx = (e.clientX - lastMouseX) / dt
    const vy = (e.clientY - lastMouseY) / dt
    const speed = Math.sqrt(vx * vx + vy * vy)
    if (speed > 0.8) {
      const blur = Math.min(speed * 1.5, 4)
      btnMotionBlur.value = `blur(${blur}px)`
    } else {
      btnMotionBlur.value = 'none'
    }
  }

  // ---- Composed full style ----
  const btnFullStyle = computed(() => {
    const pressing = btnPressing.value
    const scaleVal = pressing ? 0.96 : (btnTiltX.value || btnTiltY.value ? 1.03 : 1)
    const totalTiltX = btnTiltX.value + scrollTiltX.value
    const sx = btnStretchX.value
    const sy = btnStretchY.value
    const result: Record<string, string> = {
      transform: `translate(${btnMagX.value}px, ${btnMagY.value}px) perspective(600px) rotateX(${totalTiltX}deg) rotateY(${btnTiltY.value}deg) scale(${scaleVal}) scaleX(${sx}) scaleY(${sy})`,
      transition: pressing ? 'transform 0.1s ease' : (btnTiltX.value === 0 && btnTiltY.value === 0 ? 'transform 0.5s cubic-bezier(0.23, 1, 0.32, 1)' : 'transform 0.1s ease'),
    }
    const blur = btnMotionBlur.value
    if (blur && blur !== 'none') result.filter = blur
    const warmStyle = btnWarmthStyle.value
    if (warmStyle && (warmStyle as any).boxShadow) result.boxShadow = (warmStyle as any).boxShadow
    return result
  })

  // ---- Final composed handlers ----
  const onBtnEnterFinal = () => {
    btnIsHovered.value = true
    playHoverSound()
    startAmbientParticles()
    startWarmth()
  }

  const onBtnLeaveFinal = () => {
    btnTiltX.value = 0; btnTiltY.value = 0; btnSpotOpacity.value = 0
    btnIsHovered.value = false
    stopAmbientParticles()
    stopWarmth()
    resetStretch()
    triggerGravityBounce()
    btnMotionBlur.value = 'none'
    if (btnPressing.value) onBtnMouseUp()
  }

  let moveTicking = false
  const onBtnMoveFinal = (e: MouseEvent) => {
    if (!moveTicking) {
      requestAnimationFrame(() => {
        const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
        const x = (e.clientX - rect.left) / rect.width
        const y = (e.clientY - rect.top) / rect.height
        btnTiltY.value = (x - 0.5) * 12
        btnTiltX.value = -(y - 0.5) * 8
        btnSpotX.value = x * 100
        btnSpotY.value = y * 100
        btnSpotOpacity.value = 1
        spawnTrail(e.clientX - rect.left, e.clientY - rect.top)
        const xPct = x * 100
        const yPct = y * 100
        updateEdgeGlow(xPct, yPct)
        updateStretch(e)
        updateMotionBlur(e)
        moveTicking = false
      })
      moveTicking = true
    }
  }

  const onBtnClickFinal = (e: MouseEvent) => {
    onBtnClick(e)
    onBtnMouseDown()
    playClickSound()
    checkTripleClick(e)
  }

  const onMagneticMovePatched = (e: MouseEvent) => {
    onMagneticMove(e)
    spawnGhost()
  }

  // ---- Lifecycle ----
  startHeartbeat()

  onMounted(() => { window.addEventListener('scroll', onScrollParallax) })
  onUnmounted(() => {
    window.removeEventListener('scroll', onScrollParallax)
    if (heartbeatInterval) { clearInterval(heartbeatInterval); heartbeatInterval = null }
    if (glitchInterval) { clearInterval(glitchInterval); glitchInterval = null }
    stopAmbientParticles()
    stopWarmth()
    if (chargeInterval) { clearInterval(chargeInterval); chargeInterval = null }
  })

  return {
    // State
    btnIsHovered, btnPressing, btnFocused, btnHeartbeat, btnBouncing,
    btnChargeProgress, btnShowGlitch, btnGlitchText,
    // Particle arrays
    ambientParticles, btnGhosts, btnRipples, btnBursts, btnTrails,
    btnShockwaves, btnVortexParticles, btnConfetti,
    // Styles
    btnFullStyle, btnSpotlightStyle, btnEdgeGlowStyle,
    // Handlers
    onBtnEnterFinal, onBtnLeaveFinal, onBtnMoveFinal, onBtnClickFinal,
    onBtnMouseUp, onBtnDblClick, onBtnContextMenu, onBtnFocus, onBtnBlur,
    onMagneticMovePatched, onMagneticLeave: onMagneticLeave,
    triggerTextGlitch,
  }
}
