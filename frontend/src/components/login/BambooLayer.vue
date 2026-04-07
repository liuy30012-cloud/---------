<template>
  <!-- Layer: Bamboo -->
  <div class="bamboo-layer">
    <svg class="bamboo bamboo-left" viewBox="0 0 60 400" xmlns="http://www.w3.org/2000/svg">
      <line x1="20" y1="0" x2="20" y2="400" stroke="rgba(180,160,120,0.06)" stroke-width="3"/>
      <line x1="20" y1="60" x2="20" y2="62" stroke="rgba(180,160,120,0.1)" stroke-width="5"/>
      <line x1="20" y1="140" x2="20" y2="142" stroke="rgba(180,160,120,0.1)" stroke-width="5"/>
      <line x1="20" y1="220" x2="20" y2="222" stroke="rgba(180,160,120,0.1)" stroke-width="5"/>
      <line x1="20" y1="300" x2="20" y2="302" stroke="rgba(180,160,120,0.1)" stroke-width="5"/>
      <path d="M20,55 Q40,35 50,15" fill="none" stroke="rgba(180,160,120,0.05)" stroke-width="1.5"/>
      <path d="M20,55 Q35,45 48,30" fill="none" stroke="rgba(180,160,120,0.04)" stroke-width="1"/>
      <path d="M20,135 Q40,115 55,100" fill="none" stroke="rgba(180,160,120,0.05)" stroke-width="1.5"/>
      <path d="M20,215 Q0,195 -10,175" fill="none" stroke="rgba(180,160,120,0.05)" stroke-width="1.5"/>
      <path d="M20,215 Q5,205 -5,190" fill="none" stroke="rgba(180,160,120,0.04)" stroke-width="1"/>
      <path d="M20,295 Q40,275 52,260" fill="none" stroke="rgba(180,160,120,0.05)" stroke-width="1.5"/>
      <ellipse cx="50" cy="12" rx="12" ry="5" fill="rgba(180,160,120,0.03)" transform="rotate(-30,50,12)"/>
      <ellipse cx="55" cy="97" rx="14" ry="5" fill="rgba(180,160,120,0.025)" transform="rotate(-25,55,97)"/>
      <ellipse cx="52" cy="257" rx="13" ry="5" fill="rgba(180,160,120,0.03)" transform="rotate(-28,52,257)"/>
      <ellipse cx="-12" cy="172" rx="12" ry="4" fill="rgba(180,160,120,0.025)" transform="rotate(25,-12,172)"/>
    </svg>
    <svg class="bamboo bamboo-right" viewBox="0 0 60 400" xmlns="http://www.w3.org/2000/svg">
      <line x1="40" y1="0" x2="40" y2="400" stroke="rgba(180,160,120,0.05)" stroke-width="2.5"/>
      <line x1="40" y1="80" x2="40" y2="82" stroke="rgba(180,160,120,0.08)" stroke-width="4"/>
      <line x1="40" y1="180" x2="40" y2="182" stroke="rgba(180,160,120,0.08)" stroke-width="4"/>
      <line x1="40" y1="280" x2="40" y2="282" stroke="rgba(180,160,120,0.08)" stroke-width="4"/>
      <path d="M40,75 Q20,55 10,35" fill="none" stroke="rgba(180,160,120,0.04)" stroke-width="1.5"/>
      <path d="M40,175 Q60,155 70,140" fill="none" stroke="rgba(180,160,120,0.04)" stroke-width="1.5"/>
      <path d="M40,275 Q20,255 8,240" fill="none" stroke="rgba(180,160,120,0.04)" stroke-width="1.5"/>
      <ellipse cx="8" cy="32" rx="11" ry="4" fill="rgba(180,160,120,0.025)" transform="rotate(30,8,32)"/>
      <ellipse cx="72" cy="137" rx="12" ry="4" fill="rgba(180,160,120,0.02)" transform="rotate(-30,72,137)"/>
      <ellipse cx="6" cy="237" rx="11" ry="4" fill="rgba(180,160,120,0.025)" transform="rotate(28,6,237)"/>
    </svg>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.bamboo-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 3;
}

.bamboo {
  position: absolute;
  height: 400px;
  opacity: 0.6;
}

.bamboo-left {
  left: 5%;
  top: 20%;
}

.bamboo-right {
  right: 5%;
  top: 30%;
}

@media (max-width: 768px) {
  .bamboo {
    display: none;
  }
}
</style>
