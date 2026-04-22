<template>
  <!-- Layer: Flowing mist -->
  <div class="mist-layer">
    <div class="mist mist-1"></div>
    <div class="mist mist-2"></div>
    <div class="mist mist-3"></div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.mist-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 2;
  overflow: hidden;
}

.mist {
  position: absolute;
  width: 200%;
  height: 100px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(240, 240, 235, 0.3) 25%,
    rgba(240, 240, 235, 0.5) 50%,
    rgba(240, 240, 235, 0.3) 75%,
    transparent 100%);
  filter: blur(40px);
  opacity: 0.4;
}

.mist-1 {
  top: 20%;
  animation: mistFloat1 40s ease-in-out infinite;
}

.mist-2 {
  top: 50%;
  animation: mistFloat2 50s ease-in-out infinite;
  animation-delay: -10s;
}

.mist-3 {
  top: 70%;
  animation: mistFloat3 45s ease-in-out infinite;
  animation-delay: -20s;
}

@keyframes mistFloat1 {
  0%, 100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-30%) translateY(-20px);
  }
}

@keyframes mistFloat2 {
  0%, 100% {
    transform: translateX(-40%) translateY(0);
  }
  50% {
    transform: translateX(-60%) translateY(15px);
  }
}

@keyframes mistFloat3 {
  0%, 100% {
    transform: translateX(-45%) translateY(0);
  }
  50% {
    transform: translateX(-35%) translateY(-10px);
  }
}

@media (max-width: 768px) {
  .mist {
    opacity: 0.2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mist {
    animation: none !important;
  }
}
</style>
