import { ref, onMounted, onUnmounted } from 'vue'

export function usePetDrag(
  petEl: ReturnType<typeof ref<HTMLElement | null>>,
  onDragStart: () => void,
  onDragEnd: (x: number, y: number) => void,
) {
  const isDragging = ref(false)
  const dragOffset = ref({ x: 0, y: 0 })

  let startPos = { x: 0, y: 0 }

  function getEventPos(e: MouseEvent | TouchEvent) {
    if ('touches' in e) {
      return { x: e.touches[0].clientX, y: e.touches[0].clientY }
    }
    return { x: e.clientX, y: e.clientY }
  }

  function handleStart(e: MouseEvent | TouchEvent) {
    if (!petEl.value) return
    // Prevent right-click drag
    if ('button' in e && e.button !== 0) return

    if (e.type === 'mousedown') e.preventDefault()
    isDragging.value = true
    onDragStart()

    const pos = getEventPos(e)
    const rect = petEl.value.getBoundingClientRect()
    dragOffset.value = {
      x: pos.x - rect.left,
      y: pos.y - rect.top,
    }
    startPos = pos
  }

  function handleMove(e: MouseEvent | TouchEvent) {
    if (!isDragging.value || !petEl.value) return
    if (e.cancelable) e.preventDefault()

    const pos = getEventPos(e)
    const el = petEl.value
    const parentW = window.innerWidth
    const parentH = window.innerHeight

    let newX = pos.x - dragOffset.value.x
    let newY = pos.y - dragOffset.value.y

    // Boundary clamping (keep fully visible during drag)
    const elW = el.offsetWidth
    const elH = el.offsetHeight
    newX = Math.max(0, Math.min(parentW - elW, newX))
    newY = Math.max(0, Math.min(parentH - elH, newY))

    el.style.left = `${newX}px`
    el.style.top = `${newY}px`
    el.style.right = 'auto'
    el.style.bottom = 'auto'
  }

  function handleEnd(e: MouseEvent | TouchEvent) {
    if (!isDragging.value || !petEl.value) return
    isDragging.value = false

    const el = petEl.value
    const rect = el.getBoundingClientRect()

    // Clamp into viewport with animation
    let finalX = rect.left
    let finalY = rect.top
    const elW = el.offsetWidth
    const elH = el.offsetHeight
    const vw = window.innerWidth
    const vh = window.innerHeight

    finalX = Math.max(0, Math.min(vw - elW, finalX))
    finalY = Math.max(0, Math.min(vh - elH, finalY))

    el.style.transition = 'left 0.3s ease-out, top 0.3s ease-out'
    el.style.left = `${finalX}px`
    el.style.top = `${finalY}px`

    setTimeout(() => {
      if (el) el.style.transition = ''
    }, 300)

    onDragEnd(finalX, finalY)
  }

  onMounted(() => {
    document.addEventListener('mousemove', handleMove, { passive: false })
    document.addEventListener('mouseup', handleEnd)
    document.addEventListener('touchmove', handleMove, { passive: false })
    document.addEventListener('touchend', handleEnd)
  })

  onUnmounted(() => {
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleEnd)
    document.removeEventListener('touchmove', handleMove)
    document.removeEventListener('touchend', handleEnd)
  })

  return {
    isDragging,
    handleStart,
  }
}
