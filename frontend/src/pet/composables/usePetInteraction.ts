import { ref, computed, watch, type Ref } from 'vue'
import { usePetAnimation } from './usePetAnimation'
import type { PetAction } from './usePetState'
import {
  pickRandom, greetings, searchStart, searchEmpty, idleChat,
  feedReactions, sleepyChat, dragReactions, offlineChat,
  onlineRestored, langSwitch, checkedOutReaction,
  searchResultDynamic, historyRecall, notifDynamic,
  categoryComments, dizhiGreetings, actionChat,
} from '../data/pet-dialogues'
import { FOODS } from '../data/pet-foods'
import type { FoodItem } from '../data/pet-foods'

export function usePetInteraction(
  petState: Ref<any>,
  isZh: Ref<boolean>,
  addInteraction: (bonus: number) => void,
  play: (action: 'gaming' | 'dancing' | 'magic') => void,
  feed: (hunger: number, mood: number, affinity: number) => void,
  setAction: (action: PetAction) => void,
  setVisible: (v: boolean) => void,
) {
  const {
    currentAction, currentSprite, currentAnimClass,
    isTransitioning, transitionTo, chainTransition, forceAction
  } = usePetAnimation()

  // Sync FSM action with state
  watch(currentAction, (action) => setAction(action))

  // ============ Bubble system ============
  const bubbleText = ref('')
  const displayedBubbleText = ref('')
  const isTyping = ref(false)
  let bubbleTimer: ReturnType<typeof setTimeout> | null = null
  let typeTimer: ReturnType<typeof setInterval> | null = null
  let idleBubbleTimer: ReturnType<typeof setTimeout> | null = null

  function showBubble(dialogue: { zh: string; en: string }, durationMs?: number) {
    if (bubbleTimer) clearTimeout(bubbleTimer)
    if (typeTimer) clearInterval(typeTimer)

    const text = isZh.value ? dialogue.zh : dialogue.en
    bubbleText.value = text
    displayedBubbleText.value = ''
    isTyping.value = true

    let idx = 0
    typeTimer = setInterval(() => {
      if (idx < text.length) {
        displayedBubbleText.value += text[idx]
        idx++
      } else {
        isTyping.value = false
        if (typeTimer) clearInterval(typeTimer)
      }
    }, 40)

    const duration = durationMs || Math.max(3000, text.length * 80 + 2000)
    bubbleTimer = setTimeout(() => {
      bubbleText.value = ''
      displayedBubbleText.value = ''
    }, duration)
  }

  function showDynamicBubble(template: { zh: (d: any) => string; en: (d: any) => string }, data: any) {
    const text = isZh.value ? template.zh(data) : template.en(data)
    showBubble({ zh: text, en: text })
  }

  function dismissBubble() {
    bubbleText.value = ''
    displayedBubbleText.value = ''
    if (bubbleTimer) clearTimeout(bubbleTimer)
    if (typeTimer) clearInterval(typeTimer)
  }

  // Idle bubbles
  function startIdleBubbles() {
    const getInterval = () => {
      const freq = petState.value.bubbleFrequency
      if (freq === 'quiet') return 300000
      if (freq === 'chatty') return 30000
      return 120000
    }
    const scheduleNext = () => {
      if (idleBubbleTimer) clearTimeout(idleBubbleTimer)
      idleBubbleTimer = setTimeout(() => {
        if (petState.value.isVisible && currentAction.value === 'idle_sit' && !bubbleText.value) {
          showBubble(pickRandom(idleChat))
        }
        scheduleNext()
      }, getInterval())
    }
    if (idleBubbleTimer) clearTimeout(idleBubbleTimer)
    scheduleNext()
  }

  function stopIdleBubbles() {
    if (idleBubbleTimer) { clearTimeout(idleBubbleTimer); idleBubbleTimer = null }
  }

  // ============ Context Menu ============
  const showContextMenu = ref(false)
  const contextMenuPos = ref({ x: 0, y: 0 })
  const showFeedSub = ref(false)
  const showInteractSub = ref(false)
  const showStatus = ref(false)
  let statusTimer: ReturnType<typeof setTimeout> | null = null

  const contextMenuStyle = computed(() => ({
    left: `${contextMenuPos.value.x}px`,
    top: `${contextMenuPos.value.y}px`,
  }))

  function openContextMenu(e: MouseEvent) {
    contextMenuPos.value = { x: e.clientX, y: e.clientY }
    showContextMenu.value = true
    showFeedSub.value = false
    showInteractSub.value = false
  }

  function closeContextMenu() {
    showContextMenu.value = false
    showFeedSub.value = false
    showInteractSub.value = false
  }

  function ctxFeedToggle() { showFeedSub.value = !showFeedSub.value; showInteractSub.value = false }
  function ctxInteractToggle() { showInteractSub.value = !showInteractSub.value; showFeedSub.value = false }

  function doAction(action: 'gaming' | 'dancing' | 'magic' | 'angry') {
    closeContextMenu()
    if (action === 'angry') {
      petState.value.mood = Math.max(0, petState.value.mood - 5)
      petState.value.totalInteractions++
      petState.value.lastInteractionTime = Date.now()
    } else {
      play(action)
    }
    forceAction(action)
    const chat = actionChat[action]
    if (chat && chat.length > 0) showBubble(pickRandom(chat))
  }

  function doFeed(food: FoodItem) {
    closeContextMenu()
    feed(food.hungerRestore, food.moodRestore, food.affinityBonus)
    addInteraction(food.affinityBonus)
    chainTransition('eating', 'happy_jump')
    showBubble(pickRandom(feedReactions))
  }

  function ctxToggleSleep() {
    closeContextMenu()
    if (currentAction.value === 'sleeping') {
      transitionTo('idle_sit')
      showBubble({ zh: '嗯？怎么了？', en: 'Hmm? What is it?' })
    } else {
      showBubble(pickRandom(sleepyChat))
      chainTransition('yawn', 'sleeping')
    }
  }

  function ctxShowStatus() {
    closeContextMenu()
    if (statusTimer) clearTimeout(statusTimer)
    showStatus.value = !showStatus.value
    if (showStatus.value) {
      statusTimer = setTimeout(() => { showStatus.value = false }, 8000)
    }
  }

  function ctxHide() {
    closeContextMenu()
    setVisible(false)
  }

  function handleGlobalClick() {
    closeContextMenu()
    showStatus.value = false
    if (statusTimer) clearTimeout(statusTimer)
  }

  // ============ Event Bus handler ============
  let searchBubbleTimer: ReturnType<typeof setTimeout> | null = null

  function handlePetEvent(evt: { event: string; data?: any; ts?: number } | null | undefined) {
    if (!evt) return
    const { event, data } = evt

    switch (event) {
      case 'search:start':
        forceAction('searching')
        showBubble(pickRandom(searchStart))
        break

      case 'search:complete':
        if (data?.books?.length > 0) {
          const book = data.books[0]
          chainTransition('found_book', 'happy_jump')
          if (searchBubbleTimer) clearTimeout(searchBubbleTimer)
          searchBubbleTimer = setTimeout(() => {
            showDynamicBubble(searchResultDynamic, {
              title: book.title, location: book.location, count: data.books.length,
            })
          }, 600)
          addInteraction(2)
        } else {
          forceAction('sad')
          showBubble(pickRandom(searchEmpty))
        }
        break

      case 'filter:change':
        if (currentAction.value === 'idle_sit' || currentAction.value === 'walk_right') {
          transitionTo('waving')
          if (data?.category) {
            const cat = categoryComments[data.category]
            if (cat) showBubble(cat)
          }
        }
        break

      case 'lang:switch':
        transitionTo('waving')
        showBubble(pickRandom(langSwitch))
        break

      case 'notif:new':
        if (data?.title) {
          transitionTo('notification')
          showDynamicBubble(notifDynamic, { title: data.title })
          addInteraction(1)
        }
        break

      case 'offline:detected':
        forceAction('sad')
        showBubble(pickRandom(offlineChat))
        break

      case 'offline:restored':
        transitionTo('happy_jump')
        showBubble(pickRandom(onlineRestored))
        break
    }
  }

  // ============ Dizhi time-based behavior ============
  let dizhiTimer: ReturnType<typeof setInterval> | null = null
  let lastDizhi = ''

  function getDizhiKey(): string {
    const hour = new Date().getHours()
    if (hour >= 23 || hour < 1) return 'zi'
    if (hour < 3) return 'chou'
    if (hour < 5) return 'yin'
    if (hour < 7) return 'mao'
    if (hour < 9) return 'chen'
    if (hour < 11) return 'si'
    if (hour < 13) return 'wu'
    if (hour < 15) return 'wei'
    if (hour < 17) return 'shen'
    if (hour < 19) return 'you'
    if (hour < 21) return 'xu'
    return 'hai'
  }

  function checkDizhi() {
    const key = getDizhiKey()
    if (key !== lastDizhi) {
      lastDizhi = key
      const greeting = dizhiGreetings[key]
      if (greeting) {
        if (['zi', 'chou', 'yin'].includes(key)) {
          if (currentAction.value !== 'sleeping') chainTransition('yawn', 'sleeping')
        } else {
          showBubble(greeting)
          if (currentAction.value === 'sleeping' && !['hai', 'xu'].includes(key)) transitionTo('idle_sit')
        }
      }
    }
  }

  function startDizhiTimer() {
    lastDizhi = getDizhiKey()
    dizhiTimer = setInterval(checkDizhi, 60000)
  }

  function cleanup() {
    if (bubbleTimer) clearTimeout(bubbleTimer)
    if (typeTimer) clearInterval(typeTimer)
    if (statusTimer) clearTimeout(statusTimer)
    if (searchBubbleTimer) clearTimeout(searchBubbleTimer)
    stopIdleBubbles()
    if (dizhiTimer) clearInterval(dizhiTimer)
  }

  return {
    // Animation
    currentAction, currentSprite, currentAnimClass,
    isTransitioning, transitionTo, chainTransition, forceAction,
    // Bubble
    bubbleText, displayedBubbleText, isTyping,
    showBubble, dismissBubble, startIdleBubbles,
    // Context Menu
    showContextMenu, contextMenuStyle, showFeedSub, showInteractSub, showStatus,
    openContextMenu, closeContextMenu,
    ctxFeedToggle, ctxInteractToggle,
    doAction, doFeed, ctxToggleSleep, ctxShowStatus, ctxHide,
    handleGlobalClick,
    // Event Bus
    handlePetEvent,
    // Dizhi
    startDizhiTimer,
    // Cleanup
    cleanup,
  }
}
