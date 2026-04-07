import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { logger } from '../../utils/logger'

// ============ Types ============
export type PetAction =
  | 'idle_sit' | 'walk_right' | 'sleeping' | 'happy_jump'
  | 'searching' | 'reading' | 'found_book' | 'sad'
  | 'waving' | 'dragged' | 'eating' | 'pointing'
  | 'notification' | 'yawn' | 'lick_paw'
  | 'gaming' | 'dancing' | 'magic' | 'angry'

export interface PetState {
  name: string
  mood: number          // 0-100
  hunger: number        // 0-100
  affinity: number      // 0-∞
  currentAction: PetAction
  isVisible: boolean
  scale: number
  bubbleFrequency: 'quiet' | 'normal' | 'chatty'
  position: { x: number; y: number }
  lastInteractionTime: number
  totalInteractions: number
  feedCount: number
  createdAt: number
}

const STORAGE_KEY = 'jidanzai-pet-state'

const defaultState = (): PetState => ({
  name: '鸡蛋仔',
  mood: 80,
  hunger: 70,
  affinity: 0,
  currentAction: 'idle_sit',
  isVisible: true,
  scale: 1,
  bubbleFrequency: 'normal',
  position: { x: -1, y: -1 }, // -1 means default position
  lastInteractionTime: Date.now(),
  totalInteractions: 0,
  feedCount: 0,
  createdAt: Date.now(),
})

function loadState(): PetState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      // Force isVisible to true if it was accidentally set to false
      const state = { ...defaultState(), ...parsed }
      if (state.isVisible === false) {
        logger.warn('Pet was hidden, resetting to visible')
        state.isVisible = true
      }
      return state
    }
  } catch { /* ignore */ }
  return defaultState()
}

function saveState(state: PetState) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch { /* ignore */ }
}

// ============ Shared Singleton State ============
const globalState = ref<PetState>(loadState())
let activeInstances = 0
let decayInterval: ReturnType<typeof setInterval> | null = null
let saveInterval: ReturnType<typeof setInterval> | null = null
let timersStarted = false
let saveDebounceTimer: ReturnType<typeof setTimeout> | null = null

// Watch global state ONCE
watch(globalState, () => {
  if (saveDebounceTimer) clearTimeout(saveDebounceTimer)
  saveDebounceTimer = setTimeout(() => {
    saveState(globalState.value)
  }, 1000)
}, { deep: true })

function startTimers() {
  if (timersStarted) return
  stopTimers()
  timersStarted = true

  decayInterval = setInterval(() => {
    if (globalState.value.hunger > 0) {
      globalState.value.hunger = Math.max(0, globalState.value.hunger - 1)
    }
    if (globalState.value.hunger < 30 && globalState.value.mood > 0) {
      globalState.value.mood = Math.max(0, globalState.value.mood - 2)
    }
  }, 600000) // 10 min

  saveInterval = setInterval(() => {
    saveState(globalState.value)
  }, 30000)
}

function stopTimers() {
  if (decayInterval) {
    clearInterval(decayInterval)
    decayInterval = null
  }
  if (saveInterval) {
    clearInterval(saveInterval)
    saveInterval = null
  }
  timersStarted = false
}

export function usePetState() {
  const state = globalState

  // Computed helpers
  const affinityLevel = computed(() => {
    const a = state.value.affinity
    if (a >= 200) return { level: 5, title: '首席馆员', titleEn: 'Chief Librarian', icon: '📔' }
    if (a >= 100) return { level: 4, title: '资深馆员', titleEn: 'Senior Librarian', icon: '📕' }
    if (a >= 50)  return { level: 3, title: '正式馆员', titleEn: 'Librarian', icon: '📙' }
    if (a >= 20)  return { level: 2, title: '见习馆员', titleEn: 'Apprentice', icon: '📘' }
    return { level: 1, title: '实习馆员', titleEn: 'Intern', icon: '📗' }
  })

  const daysTogether = computed(() => {
    return Math.max(1, Math.floor((Date.now() - state.value.createdAt) / 86400000))
  })

  const isHungry = computed(() => state.value.hunger < 30)
  const isSad = computed(() => state.value.mood < 40)

  // Actions
  function addInteraction(affinityBonus = 1) {
    state.value.totalInteractions++
    state.value.affinity += affinityBonus
    state.value.lastInteractionTime = Date.now()
    if (state.value.mood < 100) {
      state.value.mood = Math.min(100, state.value.mood + 3)
    }
  }

  function play(actionType: 'gaming' | 'dancing' | 'magic') {
    state.value.totalInteractions++
    state.value.affinity += 2
    
    if (actionType === 'gaming' || actionType === 'dancing') {
      state.value.mood = Math.min(100, state.value.mood + 10)
      state.value.hunger = Math.max(0, state.value.hunger - 5)
    } else if (actionType === 'magic') {
      state.value.mood = Math.min(100, state.value.mood + 5)
      state.value.hunger = Math.max(0, state.value.hunger - 2)
    }
    state.value.lastInteractionTime = Date.now()
  }

  function feed(hungerRestore: number, moodRestore: number, affinityBonus: number) {
    state.value.hunger = Math.min(100, state.value.hunger + hungerRestore)
    state.value.mood = Math.min(100, state.value.mood + moodRestore)
    state.value.affinity += affinityBonus
    state.value.feedCount++
    state.value.lastInteractionTime = Date.now()
  }

  function setAction(action: PetAction) {
    state.value.currentAction = action
  }

  function setVisible(visible: boolean) {
    state.value.isVisible = visible
  }

  function setScale(scale: number) {
    state.value.scale = Math.max(0.5, Math.min(2, scale))
  }

  function setPosition(x: number, y: number) {
    state.value.position = { x, y }
  }

  function setBubbleFrequency(freq: 'quiet' | 'normal' | 'chatty') {
    state.value.bubbleFrequency = freq
  }

  function resetState() {
    state.value = defaultState()
    saveState(state.value)
  }

  onMounted(() => {
    activeInstances++
    if (activeInstances === 1) {
      startTimers()
    }
  })

  onUnmounted(() => {
    activeInstances = Math.max(0, activeInstances - 1)
    if (activeInstances === 0) {
      stopTimers()
      saveState(state.value)
    }
  })

  return {
    state,
    affinityLevel,
    daysTogether,
    isHungry,
    isSad,
    addInteraction,
    play,
    feed,
    setAction,
    setVisible,
    setScale,
    setPosition,
    setBubbleFrequency,
    resetState,
  }
}
