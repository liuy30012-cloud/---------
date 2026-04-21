import { ref } from 'vue'
import { poemLoader } from '../data/poemLoader'

interface Particle {
  id: number
  style: Record<string, string>
}

interface BookPage {
  id: number
  renderKey: number
  title: string
  author: string
  poem: string
  style: Record<string, string>
}

interface InkDot {
  id: number
  style: Record<string, string>
}

interface PoemEntry {
  title: string
  author: string
  poem: string
}

export function useLoginParticles() {
  const mouseX = ref(0)
  const mouseY = ref(0)

  const mouseParticles = ref<Particle[]>([])
  let particleId = 0
  let lastParticleTime = 0

  const ambientParticles = ref<Particle[]>([])
  const celebrationParticles = ref<Particle[]>([])
  const clickRipples = ref<Particle[]>([])
  const inkSplashDots = ref<Particle[]>([])
  let rippleCnt = 0

  const iconNames = ['menu_book', 'auto_stories', 'library_books', 'bookmark', 'school', 'local_library', 'collections_bookmark', 'edit_note']
  const floatingIcons = iconNames.map((name, i) => ({
    id: i,
    name,
    style: {
      left: `${8 + (i * 12) % 90}%`,
      animationDelay: `${i * 1.8}s`,
      animationDuration: `${16 + (i % 5) * 3}s`,
      fontSize: `${20 + (i % 3) * 8}px`,
      opacity: `${0.04 + (i % 4) * 0.015}`,
    }
  }))

  const bookPages = ref<BookPage[]>([])
  let allPoemsCache: PoemEntry[] = []
  let poemPoolPromise: Promise<PoemEntry[]> | null = null
  let poemQueue: PoemEntry[] = []
  let lastPoemSignature: string | null = null
  let pageRenderKey = 0

  const inkDots = ref<InkDot[]>([])

  function getPoemSignature(poem: PoemEntry) {
    return `${poem.title}__${poem.author}__${poem.poem}`
  }

  function shufflePoems(poems: PoemEntry[]) {
    const shuffled = [...poems]

    for (let i = shuffled.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
    }

    return shuffled
  }

  function dedupePoems(poems: PoemEntry[]) {
    const seen = new Set<string>()
    const uniquePoems: PoemEntry[] = []

    for (const poem of poems) {
      const signature = getPoemSignature(poem)

      if (seen.has(signature)) {
        continue
      }

      seen.add(signature)
      uniquePoems.push(poem)
    }

    return uniquePoems
  }

  function refillPoemQueue() {
    if (allPoemsCache.length === 0) {
      poemQueue = []
      return
    }

    poemQueue = shufflePoems(allPoemsCache)

    if (
      poemQueue.length > 1
      && lastPoemSignature !== null
      && getPoemSignature(poemQueue[0]) === lastPoemSignature
    ) {
      const swapIndex = poemQueue.findIndex(
        (poem, index) => index > 0 && getPoemSignature(poem) !== lastPoemSignature
      )

      if (swapIndex > 0) {
        ;[poemQueue[0], poemQueue[swapIndex]] = [poemQueue[swapIndex], poemQueue[0]]
      }
    }
  }

  async function ensurePoemPool() {
    if (allPoemsCache.length > 0) {
      return
    }

    if (poemPoolPromise === null) {
      poemPoolPromise = poemLoader.loadAllPoems().then(poems => dedupePoems(
        poems.filter(poem => poem.title?.trim() && poem.author?.trim() && poem.poem?.trim())
      ))
    }

    allPoemsCache = await poemPoolPromise
  }

  async function getNextPoem() {
    await ensurePoemPool()

    if (poemQueue.length === 0) {
      refillPoemQueue()
    }

    const nextPoem = poemQueue.shift() ?? null

    if (nextPoem) {
      lastPoemSignature = getPoemSignature(nextPoem)
    }

    return nextPoem
  }

  function createBookPage(pageId: number, poem: PoemEntry, animationDelaySeconds: number) {
    const left = 3 + Math.random() * 88
    const duration = 22 + Math.random() * 16
    const poemLines = poem.poem.split('\n')
    const lines = poemLines.length
    const maxChars = Math.max(...poemLines.map(line => line.length))
    const width = Math.max(200, maxChars * 14 + 40)
    const height = Math.max(120, lines * 24 + 80)
    const fontSize = lines > 6 ? 11 : 12

    return {
      id: pageId,
      renderKey: ++pageRenderKey,
      title: poem.title,
      author: poem.author,
      poem: poem.poem,
      style: {
        left: `${left}%`,
        width: `${width}px`,
        height: `${height}px`,
        animationDuration: `${duration}s`,
        animationDelay: `${animationDelaySeconds}s`,
        '--sway': `${20 + Math.random() * 30}px`,
        '--poem-font-size': `${fontSize}px`,
      }
    }
  }

  function onPageMouseMove(e: MouseEvent) {
    mouseX.value = e.clientX
    mouseY.value = e.clientY

    const now = Date.now()

    if (now - lastParticleTime <= 50) {
      return
    }

    lastParticleTime = now
    const id = ++particleId
    const size = 2 + Math.random() * 4
    const hue = 30 + Math.random() * 20

    mouseParticles.value.push({
      id,
      style: {
        left: `${e.clientX}px`,
        top: `${e.clientY}px`,
        width: `${size}px`,
        height: `${size}px`,
        '--hue': `${hue}`,
        animationDuration: `${0.6 + Math.random() * 0.4}s`,
      }
    })

    if (mouseParticles.value.length > 20) {
      mouseParticles.value.shift()
    }

    setTimeout(() => {
      mouseParticles.value = mouseParticles.value.filter(particle => particle.id !== id)
    }, 1000)
  }

  function onPageClick(e: MouseEvent) {
    const id = ++rippleCnt
    clickRipples.value.push({ id, style: { left: `${e.clientX}px`, top: `${e.clientY}px` } })

    const dotCount = 3 + Math.floor(Math.random() * 3)

    for (let j = 0; j < dotCount; j += 1) {
      const dotId = id * 100 + j
      const angle = Math.random() * Math.PI * 2
      const distance = 15 + Math.random() * 30
      const dx = Math.cos(angle) * distance
      const dy = Math.sin(angle) * distance
      const size = 2 + Math.random() * 4

      inkSplashDots.value.push({
        id: dotId,
        style: {
          left: `${e.clientX + dx}px`,
          top: `${e.clientY + dy}px`,
          width: `${size}px`,
          height: `${size}px`,
          animationDuration: `${0.4 + Math.random() * 0.3}s`,
        }
      })

      setTimeout(() => {
        inkSplashDots.value = inkSplashDots.value.filter(dot => dot.id !== dotId)
      }, 800)
    }

    setTimeout(() => {
      clickRipples.value = clickRipples.value.filter(ripple => ripple.id !== id)
    }, 800)
  }

  function createAmbientParticles() {
    const particles: Particle[] = []

    for (let i = 0; i < 30; i += 1) {
      const size = 1 + Math.random() * 3

      particles.push({
        id: i,
        style: {
          left: `${Math.random() * 100}%`,
          top: `${Math.random() * 100}%`,
          width: `${size}px`,
          height: `${size}px`,
          animationDuration: `${8 + Math.random() * 12}s`,
          animationDelay: `${Math.random() * 8}s`,
          opacity: `${0.1 + Math.random() * 0.2}`,
        }
      })
    }

    ambientParticles.value = particles
  }

  function triggerCelebration() {
    const particles: Particle[] = []
    const centerX = window.innerWidth / 2
    const centerY = window.innerHeight / 2

    for (let i = 0; i < 40; i += 1) {
      const angle = (Math.PI * 2 * i) / 40 + Math.random() * 0.2
      const speed = 80 + Math.random() * 200
      const dx = Math.cos(angle) * speed
      const dy = Math.sin(angle) * speed
      const size = 4 + Math.random() * 8
      const hue = Math.random() * 60 + 20

      particles.push({
        id: i,
        style: {
          left: `${centerX}px`,
          top: `${centerY}px`,
          width: `${size}px`,
          height: `${size}px`,
          '--dx': `${dx}px`,
          '--dy': `${dy}px`,
          '--hue': `${hue}`,
          animationDuration: `${0.8 + Math.random() * 0.6}s`,
        }
      })
    }

    celebrationParticles.value = particles
    setTimeout(() => { celebrationParticles.value = [] }, 1500)
  }

  async function loadBookPages() {
    try {
      await ensurePoemPool()
      const targetCount = Math.min(18, allPoemsCache.length)

      if (targetCount === 0) {
        bookPages.value = []
        return
      }

      const pages: BookPage[] = []

      for (let i = 0; i < targetCount; i += 1) {
        const nextPoem = await getNextPoem()

        if (!nextPoem) {
          break
        }

        pages.push(createBookPage(i, nextPoem, Math.random() * 24))
      }

      bookPages.value = pages
    } catch (error) {
      console.error('加载诗词失败:', error)
      bookPages.value = []
    }
  }

  async function reloadSinglePage(pageId: number) {
    const nextPoem = await getNextPoem()

    if (!nextPoem) {
      return
    }

    const pageIndex = bookPages.value.findIndex(page => page.id === pageId)

    if (pageIndex === -1) {
      return
    }

    const nextPage = createBookPage(pageId, nextPoem, 0)
    const pages = [...bookPages.value]
    pages.splice(pageIndex, 1, nextPage)
    bookPages.value = pages
  }

  function createInkDots() {
    const dots: InkDot[] = []

    for (let i = 0; i < 14; i += 1) {
      const size = 3 + Math.random() * 8
      const x = 5 + Math.random() * 90
      const y = 60 + Math.random() * 35
      const borderRadius = `${40 + Math.random() * 20}% ${50 + Math.random() * 30}% ${35 + Math.random() * 25}% ${45 + Math.random() * 30}%`

      dots.push({
        id: i,
        style: {
          width: `${size}px`,
          height: `${size}px`,
          left: `${x}%`,
          top: `${y}%`,
          borderRadius,
          animationDelay: `${Math.random() * 5}s`,
          animationDuration: `${3 + Math.random() * 4}s`,
          opacity: `${0.08 + Math.random() * 0.12}`,
        }
      })
    }

    inkDots.value = dots
  }

  function cleanup() {
    poemPoolPromise = null
  }

  return {
    mouseX,
    mouseY,
    mouseParticles,
    ambientParticles,
    celebrationParticles,
    clickRipples,
    inkSplashDots,
    floatingIcons,
    bookPages,
    inkDots,
    onPageMouseMove,
    onPageClick,
    createAmbientParticles,
    triggerCelebration,
    loadBookPages,
    createInkDots,
    cleanup,
    reloadSinglePage,
  }
}
