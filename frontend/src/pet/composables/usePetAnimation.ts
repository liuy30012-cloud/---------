import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { PetAction } from './usePetState'

// Sprite file mapping
export const SPRITE_MAP: Record<PetAction, string> = {
  idle_sit: './pet/idle_sit.png',
  walk_right: './pet/walk_right.png',
  sleeping: './pet/sleep.png',
  happy_jump: './pet/happy_jump.png',
  searching: './pet/searching.png',
  reading: './pet/reading.png',
  found_book: './pet/found_book.png',
  sad: './pet/sad.png',
  waving: './pet/waving.png',
  dragged: './pet/dragged.png',
  eating: './pet/eating.png',
  pointing: './pet/pointing.png',
  notification: './pet/notification.png',
  yawn: './pet/yawn.png',
  lick_paw: './pet/lick_paw.png',
  gaming: './pet/gaming.png',
  dancing: './pet/dancing.png',
  magic: './pet/magic.png',
  angry: './pet/angry.png',
}

// CSS animation class for each state
export const ANIMATION_CLASS: Record<PetAction, string> = {
  idle_sit: 'pet-anim-breathe',
  walk_right: 'pet-anim-walk',
  sleeping: 'pet-anim-sleep',
  happy_jump: 'pet-anim-bounce',
  searching: 'pet-anim-search',
  reading: 'pet-anim-read',
  found_book: 'pet-anim-run-in',
  sad: 'pet-anim-tremble',
  waving: 'pet-anim-wave',
  dragged: 'pet-anim-dragged',
  eating: 'pet-anim-chew',
  pointing: 'pet-anim-point',
  notification: 'pet-anim-run-in',
  yawn: 'pet-anim-yawn',
  lick_paw: 'pet-anim-lick',
  gaming: 'pet-anim-gaming',
  dancing: 'pet-anim-dance',
  magic: 'pet-anim-magic',
  angry: 'pet-anim-angry',
}

// Duration for timed states (ms), null = indefinite
export const STATE_DURATION: Partial<Record<PetAction, number>> = {
  walk_right: 8000,
  happy_jump: 2500,
  found_book: 2000,
  sad: 3000,
  waving: 3000,
  eating: 3000,
  pointing: 5000,
  notification: 4000,
  yawn: 2000,
  lick_paw: 8000,
  reading: 20000,
  gaming: 5000,
  dancing: 5000,
  magic: 4000,
  angry: 4000,
}

// Idle random transitions with weights
interface IdleTransition {
  action: PetAction
  weight: number
  minInterval: number // ms since last action change
}

const IDLE_TRANSITIONS: IdleTransition[] = [
  { action: 'walk_right', weight: 30, minInterval: 30000 },
  { action: 'reading', weight: 20, minInterval: 45000 },
  { action: 'lick_paw', weight: 15, minInterval: 60000 },
  { action: 'yawn', weight: 10, minInterval: 120000 },
  { action: 'dancing', weight: 5, minInterval: 180000 },
  { action: 'magic', weight: 5, minInterval: 180000 },
]

export function usePetAnimation() {
  const currentAction = ref<PetAction>('idle_sit')
  const previousAction = ref<PetAction>('idle_sit')
  const isTransitioning = ref(false)
  const lastActionChangeTime = ref(Date.now())

  let stateTimer: ReturnType<typeof setTimeout> | null = null
  let idleTimer: ReturnType<typeof setInterval> | null = null
  let chainTimer: ReturnType<typeof setTimeout> | null = null
  let transitionTimer: ReturnType<typeof setTimeout> | null = null

  const currentSprite = computed(() => SPRITE_MAP[currentAction.value])
  const currentAnimClass = computed(() => ANIMATION_CLASS[currentAction.value])

  function transitionTo(action: PetAction, afterAction?: PetAction) {
    if (currentAction.value === action) return

    // Clear existing timer
    if (stateTimer) {
      clearTimeout(stateTimer)
      stateTimer = null
    }
    if (chainTimer) {
      clearTimeout(chainTimer)
      chainTimer = null
    }
    if (transitionTimer) {
      clearTimeout(transitionTimer)
      transitionTimer = null
    }

    previousAction.value = currentAction.value
    isTransitioning.value = true

    // Small delay for CSS transition
    transitionTimer = setTimeout(() => {
      currentAction.value = action
      lastActionChangeTime.value = Date.now()
      isTransitioning.value = false
      transitionTimer = null

      // If this state has a duration, auto-return
      const duration = STATE_DURATION[action]
      if (duration) {
        stateTimer = setTimeout(() => {
          const returnTo = afterAction || 'idle_sit'
          transitionTo(returnTo)
        }, duration)
      }
    }, 150) // 150ms for fade-out
  }

  // Chain: action1 → action2 → idle
  function chainTransition(action1: PetAction, action2: PetAction) {
    transitionTo(action1)
    // Small delay to ensure action1 registers fully before capturing chainTimer if needed,
    // actually transitionTo handles its own timeout.
    const dur1 = STATE_DURATION[action1] || 2000
    chainTimer = setTimeout(() => {
      transitionTo(action2)
    }, dur1)
  }

  // Random idle behavior
  function tryRandomAction() {
    if (currentAction.value !== 'idle_sit') return

    const elapsed = Date.now() - lastActionChangeTime.value
    const eligible = IDLE_TRANSITIONS.filter(t => elapsed >= t.minInterval)
    if (eligible.length === 0) return

    const totalWeight = eligible.reduce((sum, t) => sum + t.weight, 0)
    if (totalWeight === 0) return // Prevent division by zero

    let roll = Math.random() * totalWeight * 3 // make it less frequent
    if (roll > totalWeight) return // 2/3 chance of doing nothing

    for (const t of eligible) {
      roll -= t.weight
      if (roll <= 0) {
        transitionTo(t.action)
        return
      }
    }
  }

  function startIdleLoop() {
    idleTimer = setInterval(tryRandomAction, 5000) // check every 5s
  }

  function stopIdleLoop() {
    if (idleTimer) clearInterval(idleTimer)
  }

  function forceAction(action: PetAction) {
    transitionTo(action)
  }

  onMounted(() => startIdleLoop())
  onUnmounted(() => {
    stopIdleLoop()
    if (stateTimer) clearTimeout(stateTimer)
    if (chainTimer) clearTimeout(chainTimer)
    if (transitionTimer) clearTimeout(transitionTimer)
  })

  return {
    currentAction,
    previousAction,
    currentSprite,
    currentAnimClass,
    isTransitioning,
    transitionTo,
    chainTransition,
    forceAction,
  }
}
