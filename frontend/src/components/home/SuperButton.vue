<template>
  <div
    class="action-center btn-magnetic-area"
    @mousemove="onMagneticMovePatched"
    @mouseleave="onMagneticLeave"
  >
    <i v-for="p in ambientParticles" :key="p.id" class="btn-ambient-particle" :style="p.style"></i>
    <div v-for="g in btnGhosts" :key="g.id" class="btn-ghost" :style="g.style"></div>
    <button
      class="view-all-btn"
      @mousemove="onBtnMoveFinal"
      @mouseleave="onBtnLeaveFinal"
      @mousedown="onBtnClickFinal"
      @mouseup="onBtnMouseUp"
      @mouseenter="onBtnEnterFinal(); triggerTextGlitch($t('related.viewAll'))"
      @dblclick="onBtnDblClick"
      @contextmenu="onBtnContextMenu"
      @focus="onBtnFocus"
      @blur="onBtnBlur"
      :style="btnFullStyle"
      :class="{
        'btn-hovered': btnIsHovered,
        'btn-pressing': btnPressing,
        'btn-focused': btnFocused,
        'btn-heartbeat': btnHeartbeat,
        'btn-bouncing': btnBouncing
      }"
    >
      <i class="btn-wing wing-left"></i>
      <i class="btn-wing wing-right"></i>
      <i class="btn-pulse-ring"></i>
      <i class="btn-edge-glow" :style="btnEdgeGlowStyle"></i>
      <i class="splash-drop drop-1"></i>
      <i class="splash-drop drop-2"></i>
      <i class="splash-drop drop-3"></i>
      <i class="splash-drop drop-4"></i>
      <i class="splash-drop drop-5"></i>
      <i class="splash-drop drop-6"></i>
      <span class="btn-spotlight" :style="btnSpotlightStyle"></span>
      <span class="btn-ripple" v-for="r in btnRipples" :key="r.id" :style="r.style"></span>
      <i v-for="b in btnBursts" :key="b.id" class="btn-burst-particle" :style="b.style"></i>
      <i v-for="t in btnTrails" :key="t.id" class="btn-trail-dot" :style="t.style"></i>
      <i v-for="s in btnShockwaves" :key="s.id" class="btn-shockwave" :style="s.style"></i>
      <i v-for="v in btnVortexParticles" :key="v.id" class="btn-vortex-dot" :style="v.style"></i>
      <i v-for="c in btnConfetti" :key="c.id" class="btn-confetti" :style="c.style"></i>
      <span class="btn-charge-bar" :style="{ width: btnChargeProgress + '%' }"></span>
      <span class="btn-glitch-text" v-if="btnShowGlitch">{{ btnGlitchText }}</span>
      <span class="btn-text-wrap" :class="{ 'btn-text-hidden': btnShowGlitch }">{{ $t('related.viewAll') }}</span>
      <span class="material-symbols-outlined btn-arrow">arrow_forward</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { useSuperButton } from '../../composables/useSuperButton'

const {
  btnIsHovered, btnPressing, btnFocused, btnHeartbeat, btnBouncing,
  btnChargeProgress, btnShowGlitch, btnGlitchText,
  ambientParticles, btnGhosts, btnRipples, btnBursts, btnTrails,
  btnShockwaves, btnVortexParticles, btnConfetti,
  btnFullStyle, btnSpotlightStyle, btnEdgeGlowStyle,
  onBtnEnterFinal, onBtnLeaveFinal, onBtnMoveFinal, onBtnClickFinal,
  onBtnMouseUp, onBtnDblClick, onBtnContextMenu, onBtnFocus, onBtnBlur,
  onMagneticMovePatched, onMagneticLeave,
  triggerTextGlitch,
} = useSuperButton()
</script>
