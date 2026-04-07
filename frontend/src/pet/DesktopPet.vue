<template>
  <div
    v-if="petState.isVisible"
    class="pet-container pet-visible"
    ref="containerRef"
    :style="containerStyle"
  >
    <div
      class="pet-interactive"
      :class="{ 'is-dragging': isDragging }"
      @mousedown="handleStart"
      @touchstart="handleStart"
      @contextmenu.prevent="openContextMenu"
      @dblclick="goHome"
    >
      <!-- Status Panel (above pet) -->
      <Transition name="pet-bubble">
        <div v-if="showStatus" class="pet-status-panel" @click.stop>
          <div class="pet-status-header">
            <span>🐱</span>
            <span class="pet-status-name">{{ petState.name }}</span>
            <span class="pet-status-level">{{ affinityLevel.icon }} {{ isZh ? affinityLevel.title : affinityLevel.titleEn }}</span>
          </div>
          <div class="pet-stat-row">
            <span class="pet-stat-icon">❤️</span>
            <div class="pet-stat-bar-bg">
              <div class="pet-stat-bar-fill mood" :style="{ width: petState.mood + '%' }"></div>
            </div>
            <span class="pet-stat-value">{{ petState.mood }}</span>
          </div>
          <div class="pet-stat-row">
            <span class="pet-stat-icon">🍖</span>
            <div class="pet-stat-bar-bg">
              <div class="pet-stat-bar-fill hunger" :style="{ width: petState.hunger + '%' }"></div>
            </div>
            <span class="pet-stat-value">{{ petState.hunger }}</span>
          </div>
          <div class="pet-stat-info">
            <span>💛 {{ isZh ? '好感度' : 'Affinity' }}: {{ petState.affinity }}</span>
            <span>📅 {{ isZh ? '相伴' : 'Together' }} {{ daysTogether }} {{ isZh ? '天' : 'days' }}</span>
            <span>🏆 {{ isZh ? '互动' : 'Interactions' }}: {{ petState.totalInteractions }}</span>
          </div>
        </div>
      </Transition>

      <!-- Dialogue Bubble -->
      <Transition name="pet-bubble">
        <div v-if="bubbleText" class="pet-bubble" @click="dismissBubble">
          <span>{{ displayedBubbleText }}</span>
          <span v-if="isTyping" class="cursor-blink"></span>
        </div>
      </Transition>

      <!-- Sprite -->
      <div class="pet-sprite-wrapper" :style="{ transform: `scale(${petState.scale})` }">
        <!-- Zzz effect -->
        <div v-if="currentAction === 'sleeping'" class="pet-zzz">
          <span>Z</span><span>z</span><span>Z</span>
        </div>
        <!-- Sparkle effect -->
        <div v-if="['happy_jump', 'found_book', 'magic', 'dancing'].includes(currentAction)" class="pet-sparkles">
          <span class="pet-sparkle">⭐</span>
          <span class="pet-sparkle">✨</span>
          <span class="pet-sparkle">⭐</span>
          <span class="pet-sparkle">✨</span>
          <span class="pet-sparkle">⭐</span>
        </div>
        <img
          :src="currentSprite"
          :class="[currentAnimClass, { transitioning: isTransitioning }]"
          class="pet-sprite-img"
          alt="鸡蛋仔"
          draggable="false"
        />
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div v-if="showContextMenu" class="pet-context-menu" :style="contextMenuStyle" @click.stop>
        <button class="pet-ctx-item" @click="ctxFeedToggle">
          <span class="ctx-icon">🍎</span>
          <span>{{ isZh ? '喂食' : 'Feed' }}</span>
        </button>
        <div v-if="showFeedSub" class="pet-ctx-submenu">
          <button v-for="food in FOODS" :key="food.id" class="pet-ctx-item" @click="doFeed(food)">
            <span class="ctx-icon">{{ food.icon }}</span>
            <span>{{ isZh ? food.nameZh : food.nameEn }}</span>
          </button>
        </div>
        <button class="pet-ctx-item" @click="ctxInteractToggle">
          <span class="ctx-icon">✨</span>
          <span>{{ isZh ? '互动' : 'Interact' }}</span>
        </button>
        <div v-if="showInteractSub" class="pet-ctx-submenu">
          <button class="pet-ctx-item" @click="doAction('gaming')">
            <span class="ctx-icon">🎮</span>
            <span>{{ isZh ? '玩游戏' : 'Gaming' }}</span>
          </button>
          <button class="pet-ctx-item" @click="doAction('dancing')">
            <span class="ctx-icon">🎵</span>
            <span>{{ isZh ? '跳舞' : 'Dance' }}</span>
          </button>
          <button class="pet-ctx-item" @click="doAction('magic')">
            <span class="ctx-icon">🔮</span>
            <span>{{ isZh ? '魔法' : 'Magic' }}</span>
          </button>
          <button class="pet-ctx-item" @click="doAction('angry')">
            <span class="ctx-icon">💢</span>
            <span>{{ isZh ? '惹生气' : 'Tease' }}</span>
          </button>
        </div>
        <div class="pet-ctx-divider"></div>
        <button class="pet-ctx-item" @click="ctxToggleSleep">
          <span class="ctx-icon">{{ currentAction === 'sleeping' ? '☀️' : '💤' }}</span>
          <span>{{ currentAction === 'sleeping' ? (isZh ? '唤醒' : 'Wake') : (isZh ? '催眠' : 'Sleep') }}</span>
        </button>
        <button class="pet-ctx-item" @click="ctxShowStatus">
          <span class="ctx-icon">📊</span>
          <span>{{ isZh ? '查看状态' : 'Status' }}</span>
        </button>
        <button class="pet-ctx-item" @click="goHome">
          <span class="ctx-icon">📍</span>
          <span>{{ isZh ? '回到默认位置' : 'Go Home' }}</span>
        </button>
        <div class="pet-ctx-divider"></div>
        <button class="pet-ctx-item" @click="ctxHide">
          <span class="ctx-icon">❌</span>
          <span>{{ isZh ? '隐藏鸡蛋仔' : 'Hide Jidanzai' }}</span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, inject, nextTick } from 'vue'
import { usePetState } from './composables/usePetState'
import { usePetDrag } from './composables/usePetDrag'
import { usePetInteraction } from './composables/usePetInteraction'
import { FOODS } from './data/pet-foods'
import { pickRandom, greetings, dragReactions } from './data/pet-dialogues'
import { logger } from '../utils/logger'
import './pet.css'

// ============ Injected data from App.vue ============
const petEventBus = inject<ReturnType<typeof ref<any[]>>>('petEventBus', ref([]))
const currentLocale = inject<ReturnType<typeof ref<string>>>('currentLocale', ref('zh'))
const isZh = computed(() => currentLocale.value === 'zh')

// ============ Core state ============
const {
  state: petState, affinityLevel, daysTogether,
  addInteraction, play, feed, setAction, setVisible, setPosition
} = usePetState()

logger.debug('🐱 DesktopPet mounted, initial state:', {
  isVisible: petState.value.isVisible,
  position: petState.value.position,
  currentAction: petState.value.currentAction
})

// ============ Interaction (bubble, context menu, events, dizhi) ============
const interaction = usePetInteraction(
  petState, isZh, addInteraction, play, feed, setAction, setVisible
)

const {
  currentAction, currentSprite, currentAnimClass,
  isTransitioning, transitionTo, forceAction,
  bubbleText, displayedBubbleText, isTyping,
  showBubble, dismissBubble, startIdleBubbles,
  showContextMenu, contextMenuStyle, showFeedSub, showInteractSub, showStatus,
  openContextMenu, ctxFeedToggle, ctxInteractToggle,
  doAction, doFeed, ctxToggleSleep, ctxShowStatus, ctxHide,
  handleGlobalClick, handlePetEvent, startDizhiTimer,
} = interaction

// ============ Drag ============
const containerRef = ref<HTMLElement | null>(null)
const { isDragging, handleStart } = usePetDrag(
  containerRef,
  () => { forceAction('dragged'); showBubble(pickRandom(dragReactions)) },
  (x, y) => { setPosition(x, y); transitionTo('idle_sit') },
)

// ============ Positioning ============
const containerStyle = computed(() => {
  const pos = petState.value.position
  if (pos.x === -1 && pos.y === -1) return { left: 'auto', top: 'auto', right: '24px', bottom: '24px' }
  return { left: `${pos.x}px`, top: `${pos.y}px`, right: 'auto', bottom: 'auto' }
})

function goHome() {
  interaction.closeContextMenu()
  petState.value.position = { x: -1, y: -1 }
  transitionTo('happy_jump')
  showBubble({ zh: '鸡蛋仔回来了~', en: 'Jidanzai is back~' })
}

// ============ React to App events ============
watch(petEventBus, (queue) => {
  if (!queue || !Array.isArray(queue) || queue.length === 0) return
  const events = [...queue]
  queue.splice(0, events.length)
  events.forEach(evt => {
    if (evt) handlePetEvent(evt)
  })
}, { deep: true })

// ============ Lifecycle ============
onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
  startIdleBubbles()
  nextTick(() => {
    setTimeout(() => {
      transitionTo('waving')
      showBubble(pickRandom(greetings))
    }, 1000)
  })
  startDizhiTimer()
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
  interaction.cleanup()
})
</script>