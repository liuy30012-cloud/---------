import { ref } from 'vue'

interface Particle { id: number; style: Record<string, string> }
interface BookPage { id: number; title: string; author: string; poem: string; style: Record<string, string> }
interface InkDot { id: number; style: Record<string, string> }
interface PoemEntry { title: string; author: string; poem: string }

let poemLibraryPromise: Promise<PoemEntry[]> | null = null

function loadPoemLibrary() {
  if (!poemLibraryPromise) {
    poemLibraryPromise = import('../data/poemLibrary').then(({ poemLibrary }) => poemLibrary)
  }

  return poemLibraryPromise
}

export function useLoginParticles() {
  // ===== Mouse tracking =====
  const mouseX = ref(0)
  const mouseY = ref(0)

  // ===== Mouse particle trail =====
  const mouseParticles = ref<Particle[]>([])
  let particleId = 0
  let lastParticleTime = 0

  // ===== Ambient particles =====
  const ambientParticles = ref<Particle[]>([])

  // ===== Celebration particles =====
  const celebrationParticles = ref<Particle[]>([])

  // ===== Click ripples =====
  const clickRipples = ref<Particle[]>([])
  const inkSplashDots = ref<Particle[]>([])
  let rippleCnt = 0

  // ===== Floating icons =====
  const iconNames = ['menu_book', 'auto_stories', 'library_books', 'bookmark', 'school', 'local_library', 'collections_bookmark', 'edit_note']
  const floatingIcons = iconNames.map((name, i) => ({
    id: i, name,
    style: {
      left: `${8 + (i * 12) % 90}%`,
      animationDelay: `${i * 1.8}s`,
      animationDuration: `${16 + (i % 5) * 3}s`,
      fontSize: `${20 + (i % 3) * 8}px`,
      opacity: `${0.04 + (i % 4) * 0.015}`,
    }
  }))

  // ===== Book pages =====
  const bookPages = ref<BookPage[]>([])

  // ===== Ink dots =====
  const inkDots = ref<InkDot[]>([])

  function onPageMouseMove(e: MouseEvent) {
    mouseX.value = e.clientX
    mouseY.value = e.clientY
    const now = Date.now()
    if (now - lastParticleTime > 50) {
      lastParticleTime = now
      const id = ++particleId
      const size = 2 + Math.random() * 4
      const hue = 30 + Math.random() * 20
      mouseParticles.value.push({
        id,
        style: {
          left: e.clientX + 'px', top: e.clientY + 'px',
          width: size + 'px', height: size + 'px',
          '--hue': hue + '',
          animationDuration: (0.6 + Math.random() * 0.4) + 's',
        }
      })
      if (mouseParticles.value.length > 20) mouseParticles.value.shift()
      setTimeout(() => { mouseParticles.value = mouseParticles.value.filter(p => p.id !== id) }, 1000)
    }
  }

  function onPageClick(e: MouseEvent) {
    const id = ++rippleCnt
    clickRipples.value.push({ id, style: { left: e.clientX + 'px', top: e.clientY + 'px' } })
    const dotCount = 3 + Math.floor(Math.random() * 3)
    for (let j = 0; j < dotCount; j++) {
      const did = id * 100 + j
      const angle = Math.random() * Math.PI * 2
      const dist = 15 + Math.random() * 30
      const dx = Math.cos(angle) * dist
      const dy = Math.sin(angle) * dist
      const sz = 2 + Math.random() * 4
      inkSplashDots.value.push({
        id: did,
        style: {
          left: (e.clientX + dx) + 'px', top: (e.clientY + dy) + 'px',
          width: sz + 'px', height: sz + 'px',
          animationDuration: (0.4 + Math.random() * 0.3) + 's',
        }
      })
      setTimeout(() => { inkSplashDots.value = inkSplashDots.value.filter(d => d.id !== did) }, 800)
    }
    setTimeout(() => { clickRipples.value = clickRipples.value.filter(r => r.id !== id) }, 800)
  }

  function createAmbientParticles() {
    const particles: Particle[] = []
    for (let i = 0; i < 30; i++) {
      const size = 1 + Math.random() * 3
      particles.push({
        id: i,
        style: {
          left: Math.random() * 100 + '%', top: Math.random() * 100 + '%',
          width: size + 'px', height: size + 'px',
          animationDuration: (8 + Math.random() * 12) + 's',
          animationDelay: (Math.random() * 8) + 's',
          opacity: (0.1 + Math.random() * 0.2) + '',
        }
      })
    }
    ambientParticles.value = particles
  }

  function triggerCelebration() {
    const particles: Particle[] = []
    const cx = window.innerWidth / 2
    const cy = window.innerHeight / 2
    for (let i = 0; i < 40; i++) {
      const angle = (Math.PI * 2 * i) / 40 + Math.random() * 0.2
      const speed = 80 + Math.random() * 200
      const dx = Math.cos(angle) * speed
      const dy = Math.sin(angle) * speed
      const size = 4 + Math.random() * 8
      const hue = Math.random() * 60 + 20
      particles.push({
        id: i,
        style: {
          left: cx + 'px', top: cy + 'px',
          width: size + 'px', height: size + 'px',
          '--dx': dx + 'px', '--dy': dy + 'px', '--hue': hue + '',
          animationDuration: (0.8 + Math.random() * 0.6) + 's',
        }
      })
    }
    celebrationParticles.value = particles
    setTimeout(() => { celebrationParticles.value = [] }, 1500)
  }

  async function loadBookPages() {
    const poemData = await loadPoemLibrary()
    const targetCount = Math.min(18, poemData.length)

    if (targetCount === 0) {
      bookPages.value = []
      return
    }

    const picked = new Set<number>()
    while (picked.size < targetCount) {
      picked.add(Math.floor(Math.random() * poemData.length))
    }
    const selectedPoems = [...picked].map(i => poemData[i])
    const pages: BookPage[] = []
    for (let i = 0; i < targetCount; i++) {
      const left = 3 + Math.random() * 88
      const duration = 22 + Math.random() * 16
      const delay = Math.random() * 24
      const rotStart = Math.random() * 20 - 10
      const p = selectedPoems[i]
      const lines = p.poem.split('\n').length
      const maxChars = Math.max(...p.poem.split('\n').map(l => l.length))
      const colWidth = 24
      const charHeight = 20
      const w = Math.max(110, lines * colWidth + 60)
      const h = Math.max(140, maxChars * charHeight + 90)
      const fontSize = maxChars > 8 ? 9 : maxChars > 6 ? 10 : 11
      pages.push({
        id: i, title: p.title, author: p.author, poem: p.poem,
        style: {
          left: `${left}%`, width: `${w}px`, height: `${h}px`,
          animationDuration: `${duration}s`, animationDelay: `${delay}s`,
          '--rot-start': `${rotStart}deg`, '--rot-mid': `${rotStart + 180}deg`, '--rot-end': `${rotStart + 360}deg`,
          '--sway': `${20 + Math.random() * 30}px`, '--poem-font-size': `${fontSize}px`,
        } as any
      })
    }
    bookPages.value = pages
  }

  function createInkDots() {
    const dots: InkDot[] = []
    for (let i = 0; i < 14; i++) {
      const size = 3 + Math.random() * 8
      const x = 5 + Math.random() * 90
      const y = 60 + Math.random() * 35
      const br = `${40 + Math.random() * 20}% ${50 + Math.random() * 30}% ${35 + Math.random() * 25}% ${45 + Math.random() * 30}%`
      dots.push({
        id: i,
        style: {
          width: `${size}px`, height: `${size}px`,
          left: `${x}%`, top: `${y}%`, borderRadius: br,
          animationDelay: `${Math.random() * 5}s`,
          animationDuration: `${3 + Math.random() * 4}s`,
          opacity: `${0.08 + Math.random() * 0.12}`,
        }
      })
    }
    inkDots.value = dots
  }

  return {
    mouseX, mouseY, mouseParticles, ambientParticles, celebrationParticles,
    clickRipples, inkSplashDots, floatingIcons, bookPages, inkDots,
    onPageMouseMove, onPageClick, createAmbientParticles,
    triggerCelebration, loadBookPages, createInkDots,
  }
}
