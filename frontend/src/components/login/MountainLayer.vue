<template>
  <!-- Layer: Ink-wash Mountain Landscape (水墨山水) -->
  <div class="landscape-root">
    <!-- 残月半空 · 云掩半月 — Crescent moon veiled by drifting clouds -->
    <div class="moon-wrapper">
      <!-- Asymmetric crescent glow — shifted toward the lit side (left) -->
      <div class="crescent-glow crescent-glow-near"></div>
      <div class="crescent-glow crescent-glow-mid"></div>
      <div class="crescent-glow crescent-glow-far"></div>

      <!-- Crescent Moon (残月) — photorealistic image -->
      <img class="moon-body" :src="crescentMoonImg" alt="crescent moon" draggable="false" />

      <!-- Crescent bloom — offset to the lit side only -->
      <div class="crescent-bloom"></div>

      <!-- ===== Drifting Cloud Layers (云层) ===== -->
      <!-- Cloud layer 1: thin wisp crossing upper part of moon -->
      <div class="moon-cloud moon-cloud-1"></div>
      <!-- Cloud layer 2: thicker band crossing middle -->
      <div class="moon-cloud moon-cloud-2"></div>
      <!-- Cloud layer 3: trailing wisp below -->
      <div class="moon-cloud moon-cloud-3"></div>
      <!-- Cloud layer 4: very faint high wisp -->
      <div class="moon-cloud moon-cloud-4"></div>
      <!-- Cloud layer 5: large atmospheric haze -->
      <div class="moon-cloud moon-cloud-5"></div>

      <!-- Sparse stars dimmed by cloud cover -->
      <span class="moon-star" style="top: -20px; left: -30px; --delay: 0s; --dur: 5s;"></span>
      <span class="moon-star" style="top: -25px; right: -25px; --delay: 2s; --dur: 6s;"></span>
      <span class="moon-star" style="top: 60px; left: -20px; --delay: 3.5s; --dur: 4.5s;"></span>
      <span class="moon-star" style="top: 50px; right: -35px; --delay: 1s; --dur: 5.5s;"></span>
    </div>

    <!-- Mountain scene — overflow:hidden for mountain clipping only -->
    <div class="mountain-scene" ref="sceneRef">

    <!-- Far mountains (远山) — faint, misty, high -->
    <svg class="mountain-svg mountain-far" viewBox="0 0 1440 400" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="farGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(70,55,85,0.5)" />
          <stop offset="40%" stop-color="rgba(55,45,68,0.35)" />
          <stop offset="100%" stop-color="rgba(35,28,48,0.12)" />
        </linearGradient>
        <filter id="farBlur">
          <feGaussianBlur stdDeviation="2" />
        </filter>
      </defs>
      <path d="M0,400 L0,280 Q40,265 80,270 Q120,250 160,240 Q200,220 240,210 Q280,195 310,180 Q340,165 370,170 Q410,160 440,155 Q480,140 520,135 Q560,145 590,150 Q620,140 660,130 Q700,120 730,125 Q760,115 800,108 Q840,100 870,110 Q900,105 940,95 Q980,102 1010,110 Q1040,100 1080,115 Q1120,125 1160,120 Q1200,130 1240,140 Q1280,150 1310,155 Q1350,165 1390,175 Q1420,185 1440,190 L1440,400 Z"
        fill="url(#farGrad)" filter="url(#farBlur)" />
      <!-- Secondary far ridgeline -->
      <path d="M0,400 L0,300 Q60,285 120,290 Q180,275 240,265 Q300,250 350,245 Q400,235 450,230 Q500,220 550,225 Q600,215 650,205 Q700,210 740,215 Q780,200 820,195 Q860,190 900,198 Q940,192 980,185 Q1020,190 1060,200 Q1100,195 1140,205 Q1180,210 1220,218 Q1260,225 1300,235 Q1340,245 1380,255 Q1410,262 1440,270 L1440,400 Z"
        fill="rgba(55,45,68,0.28)" filter="url(#farBlur)" />
    </svg>

    <!-- Far-layer inter-mountain mist -->
    <div class="layer-mist layer-mist-far"></div>

    <!-- Mid mountains (中山) — moderate detail -->
    <svg class="mountain-svg mountain-mid" viewBox="0 0 1440 350" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="midGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(48,38,62,0.75)" />
          <stop offset="50%" stop-color="rgba(38,30,50,0.55)" />
          <stop offset="100%" stop-color="rgba(25,20,35,0.2)" />
        </linearGradient>
        <filter id="midBlur">
          <feGaussianBlur stdDeviation="0.8" />
        </filter>
        <!-- Ink texture overlay -->
        <filter id="inkTexture">
          <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="4" seed="2" />
          <feDisplacementMap in="SourceGraphic" scale="3" />
        </filter>
      </defs>
      <path d="M0,350 L0,220 Q30,210 60,215 Q100,195 140,185 Q180,170 220,175 Q260,155 300,145 Q340,130 380,135 Q420,120 460,110 Q500,100 535,105 Q570,95 610,88 Q650,95 680,100 Q720,90 760,82 Q800,88 840,95 Q880,85 920,78 Q960,85 1000,92 Q1040,82 1080,90 Q1120,98 1150,105 Q1190,110 1230,120 Q1270,130 1300,140 Q1340,155 1380,165 Q1420,178 1440,185 L1440,350 Z"
        fill="url(#midGrad)" filter="url(#midBlur)" />
      <!-- Ridge detail -->
      <path d="M0,350 L0,250 Q50,240 100,245 Q150,235 200,225 Q250,215 300,210 Q350,200 400,195 Q450,185 500,178 Q550,172 600,175 Q650,168 700,160 Q750,155 800,158 Q850,150 900,145 Q950,150 1000,155 Q1050,148 1100,158 Q1150,165 1200,170 Q1250,178 1300,185 Q1350,195 1400,208 Q1420,215 1440,220 L1440,350 Z"
        fill="rgba(35,28,46,0.35)" />
      <!-- Pine tree silhouettes on mid ridgeline -->
      <g class="pine-trees" opacity="0.3">
        <path d="M280,145 L276,130 L273,135 L270,120 L268,128 L265,110 L262,128 L260,120 L257,135 L254,130 L250,145" fill="rgba(30,25,40,0.6)" />
        <path d="M620,88 L617,75 L614,80 L611,65 L609,73 L607,58 L604,73 L602,65 L599,80 L596,75 L593,88" fill="rgba(30,25,40,0.5)" />
        <path d="M920,78 L917,63 L914,70 L911,55 L908,65 L906,48 L903,65 L901,55 L898,70 L895,63 L892,78" fill="rgba(30,25,40,0.55)" />
        <path d="M1100,90 L1097,78 L1095,82 L1092,70 L1090,76 L1088,62 L1085,76 L1083,70 L1080,82 L1078,78 L1075,90" fill="rgba(30,25,40,0.45)" />
        <path d="M450,110 L447,98 L445,102 L442,90 L440,96 L438,82 L435,96 L433,90 L430,102 L428,98 L425,110" fill="rgba(30,25,40,0.5)" />
      </g>
    </svg>

    <!-- Mid-layer mist band -->
    <div class="layer-mist layer-mist-mid"></div>

    <!-- Near mountains (近山) — darkest, most defined -->
    <svg class="mountain-svg mountain-near" viewBox="0 0 1440 280" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="nearGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(25,20,35,0.92)" />
          <stop offset="60%" stop-color="rgba(18,15,26,0.78)" />
          <stop offset="100%" stop-color="rgba(14,12,20,0.55)" />
        </linearGradient>
      </defs>
      <path d="M0,280 L0,180 Q40,170 80,175 Q120,160 165,150 Q200,138 240,130 Q280,120 320,125 Q360,110 400,100 Q440,90 480,95 Q520,82 560,75 Q600,80 640,85 Q680,72 720,65 Q760,70 800,75 Q840,62 880,55 Q920,60 960,68 Q1000,58 1040,52 Q1080,60 1120,68 Q1160,75 1200,82 Q1240,88 1280,95 Q1320,108 1360,120 Q1400,135 1440,148 L1440,280 Z"
        fill="url(#nearGrad)" />
      <!-- Foreground ridge overlay -->
      <path d="M0,280 L0,210 Q60,200 120,205 Q180,195 240,188 Q300,178 360,172 Q420,162 480,155 Q540,148 600,152 Q660,145 720,138 Q780,135 840,140 Q900,132 960,128 Q1020,132 1080,138 Q1140,145 1200,152 Q1260,158 1320,168 Q1360,175 1400,185 Q1420,192 1440,198 L1440,280 Z"
        fill="rgba(20,16,28,0.6)" />
      <!-- Near pine trees — larger, more defined -->
      <g class="pine-trees pine-trees-near" opacity="0.65">
        <path d="M160,150 L155,128 L150,138 L146,118 L143,130 L140,105 L137,130 L134,118 L130,138 L125,128 L120,150" fill="rgba(18,14,26,0.7)" />
        <rect x="138" y="150" width="4" height="12" fill="rgba(18,14,26,0.5)" rx="1" />
        <path d="M560,75 L555,50 L550,62 L546,40 L543,55 L540,28 L537,55 L534,40 L530,62 L525,50 L520,75" fill="rgba(18,14,26,0.65)" />
        <rect x="538" y="75" width="4" height="15" fill="rgba(18,14,26,0.45)" rx="1" />
        <path d="M1060,52 L1056,32 L1052,42 L1048,22 L1045,35 L1042,12 L1039,35 L1036,22 L1032,42 L1028,32 L1024,52" fill="rgba(18,14,26,0.7)" />
        <rect x="1040" y="52" width="4" height="14" fill="rgba(18,14,26,0.5)" rx="1" />
        <path d="M800,75 L796,58 L793,65 L790,50 L787,60 L785,40 L782,60 L780,50 L777,65 L774,58 L770,75" fill="rgba(18,14,26,0.6)" />
        <rect x="783" y="75" width="4" height="12" fill="rgba(18,14,26,0.4)" rx="1" />
      </g>
      <!-- Small boat silhouette on water edge -->
      <g class="boat-silhouette" opacity="0.35">
        <path d="M340,268 Q350,262 370,262 Q390,262 400,268" fill="none" stroke="rgba(180,160,130,0.3)" stroke-width="1.5" />
        <line x1="365" y1="262" x2="365" y2="248" stroke="rgba(180,160,130,0.25)" stroke-width="1" />
        <path d="M365,248 Q372,250 375,256" fill="none" stroke="rgba(180,160,130,0.2)" stroke-width="0.8" />
      </g>
    </svg>

    <!-- Water surface (水面) — Canvas-based realistic reflections -->
    <div class="water-surface">
      <!-- Water base gradient -->
      <div class="water-base"></div>

      <!-- SVG reflection with wave distortion -->
      <svg class="water-reflection-svg" viewBox="0 0 1440 250" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <!-- Animated wave distortion filter -->
          <filter id="waterWave" x="-5%" y="-5%" width="110%" height="110%">
            <feTurbulence type="fractalNoise" baseFrequency="0.015 0.08" numOctaves="3" seed="1" result="noise">
              <animate attributeName="baseFrequency" values="0.015 0.08;0.018 0.06;0.012 0.09;0.015 0.08" dur="12s" repeatCount="indefinite" />
            </feTurbulence>
            <feDisplacementMap in="SourceGraphic" in2="noise" scale="18" xChannelSelector="R" yChannelSelector="G" />
          </filter>
          <!-- Fade-to-bottom mask -->
          <linearGradient id="reflFade" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="white" stop-opacity="0.55" />
            <stop offset="35%" stop-color="white" stop-opacity="0.35" />
            <stop offset="70%" stop-color="white" stop-opacity="0.15" />
            <stop offset="100%" stop-color="white" stop-opacity="0" />
          </linearGradient>
          <mask id="reflMask">
            <rect width="1440" height="250" fill="url(#reflFade)" />
          </mask>
          <!-- Mountain reflection gradients -->
          <linearGradient id="nearReflGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="rgba(42,35,55,0.7)" />
            <stop offset="50%" stop-color="rgba(32,26,42,0.45)" />
            <stop offset="100%" stop-color="rgba(22,18,32,0.15)" />
          </linearGradient>
          <linearGradient id="midReflGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="rgba(55,45,68,0.45)" />
            <stop offset="60%" stop-color="rgba(40,32,52,0.25)" />
            <stop offset="100%" stop-color="rgba(28,22,38,0.08)" />
          </linearGradient>
          <linearGradient id="farReflGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="rgba(60,50,72,0.3)" />
            <stop offset="100%" stop-color="rgba(40,34,55,0.08)" />
          </linearGradient>
        </defs>
        <!-- Reflected mountain group — flipped vertically with wave distortion -->
        <g filter="url(#waterWave)" mask="url(#reflMask)">
          <!-- Far mountain reflection (faintest) -->
          <path d="M0,0 L0,35 Q40,40 80,38 Q120,44 160,48 Q200,54 240,58 Q280,64 310,70 Q340,76 370,74 Q410,78 440,80 Q480,86 520,88 Q560,84 590,82 Q620,86 660,90 Q700,95 730,93 Q760,98 800,102 Q840,105 870,100 Q900,103 940,108 Q980,104 1010,100 Q1040,105 1080,98 Q1120,92 1160,95 Q1200,90 1240,85 Q1280,80 1310,78 Q1350,72 1390,68 Q1420,62 1440,58 L1440,0 Z"
            fill="url(#farReflGrad)" opacity="0.5" />
          <!-- Mid mountain reflection -->
          <path d="M0,0 L0,52 Q30,55 60,53 Q100,60 140,65 Q180,72 220,70 Q260,78 300,82 Q340,90 380,87 Q420,95 460,100 Q500,105 535,102 Q570,108 610,112 Q650,108 680,105 Q720,112 760,118 Q800,114 840,110 Q880,118 920,122 Q960,118 1000,112 Q1040,118 1080,115 Q1120,108 1150,102 Q1190,98 1230,92 Q1270,85 1300,80 Q1340,72 1380,65 Q1420,58 1440,52 L1440,0 Z"
            fill="url(#midReflGrad)" opacity="0.6" />
          <!-- Near mountain reflection (strongest) -->
          <path d="M0,0 L0,40 Q40,42 80,40 Q120,48 165,52 Q200,58 240,62 Q280,68 320,65 Q360,72 400,78 Q440,82 480,80 Q520,88 560,92 Q600,88 640,85 Q680,92 720,96 Q760,92 800,90 Q840,96 880,100 Q920,96 960,92 Q1000,98 1040,102 Q1080,96 1120,92 Q1160,88 1200,84 Q1240,78 1280,72 Q1320,65 1360,58 Q1400,50 1440,42 L1440,0 Z"
            fill="url(#nearReflGrad)" opacity="0.7" />
          <!-- Pine tree reflections (inverted, wavering) -->
          <g opacity="0.3">
            <path d="M120,52 L125,62 L130,58 L134,68 L137,62 L140,75 L143,62 L146,68 L150,58 L155,62 L160,52" fill="rgba(25,20,35,0.5)" />
            <path d="M520,92 L525,102 L530,98 L534,108 L537,102 L540,115 L543,102 L546,108 L550,98 L555,102 L560,92" fill="rgba(25,20,35,0.45)" />
            <path d="M770,90 L774,98 L777,95 L780,105 L782,98 L785,112 L787,98 L790,105 L793,95 L796,98 L800,90" fill="rgba(25,20,35,0.4)" />
            <path d="M1024,102 L1028,112 L1032,108 L1036,118 L1039,112 L1042,125 L1045,112 L1048,118 L1052,108 L1056,112 L1060,102" fill="rgba(25,20,35,0.45)" />
          </g>
        </g>
      </svg>

      <!-- Waterline glow — luminous edge where mountains meet water -->
      <div class="waterline-glow"></div>

      <!-- Layered wave strips — horizontal sine-wave ripples -->
      <div class="wave-strips">
        <div class="wave-strip wave-strip-1"></div>
        <div class="wave-strip wave-strip-2"></div>
        <div class="wave-strip wave-strip-3"></div>
        <div class="wave-strip wave-strip-4"></div>
        <div class="wave-strip wave-strip-5"></div>
        <div class="wave-strip wave-strip-6"></div>
        <div class="wave-strip wave-strip-7"></div>
        <div class="wave-strip wave-strip-8"></div>
      </div>

      <!-- Moonlight Path (月光路) — wide tapered light corridor on water -->
      <div class="moonlight-path">
        <!-- Moon disc reflection at water surface -->
        <div class="moon-disc-reflection"></div>
        <!-- Central bright column -->
        <div class="moonpath-center"></div>
        <!-- Left glow wing -->
        <div class="moonpath-wing moonpath-wing-left"></div>
        <!-- Right glow wing -->
        <div class="moonpath-wing moonpath-wing-right"></div>
        <!-- Flickering bright spots within path -->
        <span class="moonpath-sparkle" style="top:15%; left:45%; --d:0s; --s:3px;"></span>
        <span class="moonpath-sparkle" style="top:30%; left:52%; --d:1.2s; --s:2px;"></span>
        <span class="moonpath-sparkle" style="top:45%; left:42%; --d:2.5s; --s:4px;"></span>
        <span class="moonpath-sparkle" style="top:55%; left:55%; --d:0.8s; --s:2.5px;"></span>
        <span class="moonpath-sparkle" style="top:70%; left:48%; --d:3s; --s:3.5px;"></span>
        <span class="moonpath-sparkle" style="top:25%; left:58%; --d:1.8s; --s:2px;"></span>
        <span class="moonpath-sparkle" style="top:60%; left:38%; --d:4s; --s:3px;"></span>
        <span class="moonpath-sparkle" style="top:85%; left:50%; --d:2s; --s:2.5px;"></span>
      </div>

      <!-- Sparkle highlights on water -->
      <div class="water-sparkles">
        <span class="sparkle" v-for="i in 12" :key="i"
          :style="{
            left: (5 + Math.random() * 90) + '%',
            top: (10 + Math.random() * 80) + '%',
            animationDelay: (Math.random() * 8) + 's',
            animationDuration: (3 + Math.random() * 4) + 's',
            '--size': (1 + Math.random() * 2) + 'px'
          }"></span>
      </div>

      <!-- Water depth overlay -->
      <div class="water-depth"></div>
    </div>

    <!-- Floating mist wisps (foreground atmosphere) -->
    <div class="wisp wisp-1"></div>
    <div class="wisp wisp-2"></div>
    <div class="wisp wisp-3"></div>

    <!-- Flying birds (飞鸟) silhouette -->
    <svg class="flying-birds" viewBox="0 0 200 80" xmlns="http://www.w3.org/2000/svg">
      <g class="bird-group" opacity="0.15">
        <path d="M20,40 Q28,30 36,35 Q44,30 52,40" fill="none" stroke="rgba(180,160,130,0.5)" stroke-width="1.2" stroke-linecap="round" />
        <path d="M60,35 Q66,27 72,31 Q78,27 84,35" fill="none" stroke="rgba(180,160,130,0.45)" stroke-width="1" stroke-linecap="round" />
        <path d="M45,50 Q50,43 55,46 Q60,43 65,50" fill="none" stroke="rgba(180,160,130,0.4)" stroke-width="0.9" stroke-linecap="round" />
        <path d="M90,42 Q95,36 100,39 Q105,36 110,42" fill="none" stroke="rgba(180,160,130,0.35)" stroke-width="0.8" stroke-linecap="round" />
        <path d="M75,55 Q79,50 83,52 Q87,50 91,55" fill="none" stroke="rgba(180,160,130,0.3)" stroke-width="0.7" stroke-linecap="round" />
      </g>
    </svg>
    </div><!-- /mountain-scene -->
  </div><!-- /landscape-root -->
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import crescentMoonImg from '../../assets/crescent-moon.png'

const sceneRef = ref<HTMLElement | null>(null)

// Subtle parallax on mouse move
function onMouseMove(e: MouseEvent) {
  if (!sceneRef.value) return
  const cx = (e.clientX / window.innerWidth - 0.5) * 2
  const cy = (e.clientY / window.innerHeight - 0.5) * 2
  const far = sceneRef.value.querySelector('.mountain-far') as HTMLElement
  const mid = sceneRef.value.querySelector('.mountain-mid') as HTMLElement
  const near = sceneRef.value.querySelector('.mountain-near') as HTMLElement
  const birds = sceneRef.value.querySelector('.flying-birds') as HTMLElement
  if (far) far.style.transform = `translateX(${cx * 6}px) translateY(${cy * 3}px)`
  if (mid) mid.style.transform = `translateX(${cx * 12}px) translateY(${cy * 5}px)`
  if (near) near.style.transform = `translateX(${cx * 20}px) translateY(${cy * 8}px)`
  if (birds) birds.style.transform = `translate(${cx * 15}px, ${cy * 6}px)`
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<style scoped>
/* Root wrapper — no overflow constraint */
.landscape-root {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 55vh;
  pointer-events: none;
  z-index: 2;
}

.mountain-scene {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  overflow: hidden;
}

/* ===== 残月半空 · 云掩半月 ===== */
.moon-wrapper {
  position: absolute;
  top: 0%;
  right: 18%;
  z-index: 20;
  width: 150px;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

/* Crescent Moon image — circular clip hides the square edges */
.moon-body {
  position: relative;
  width: 150px;
  height: 150px;
  z-index: 3;
  object-fit: contain;
  user-select: none;
  /* Circular clip to remove square corners — the dark interior blends with night sky */
  clip-path: circle(48% at center);
  animation: crescentGlow 8s ease-in-out infinite alternate;
  will-change: filter, transform;
  filter: drop-shadow(0 0 6px rgba(210, 205, 190, 0.3))
          drop-shadow(0 0 15px rgba(200, 195, 180, 0.12));
}

/* ===== Asymmetric Crescent Glow ===== */
/* All glow layers are offset LEFT toward the lit crescent side */
.crescent-glow {
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
}

/* Near glow — tight, bright, hugging the crescent */
.crescent-glow-near {
  width: 80px;
  height: 90px;
  top: 50%;
  left: 15%;
  transform: translate(-50%, -50%);
  z-index: 2;
  background: radial-gradient(ellipse at 60% 50%,
    rgba(210, 205, 192, 0.1) 0%,
    rgba(200, 195, 182, 0.05) 40%,
    transparent 75%);
  animation: crescentGlowPulse 7s ease-in-out infinite alternate;
}

/* Mid glow — wider, softer, still offset left */
.crescent-glow-mid {
  width: 160px;
  height: 140px;
  top: 50%;
  left: 25%;
  transform: translate(-50%, -50%);
  z-index: 1;
  background: radial-gradient(ellipse at 65% 50%,
    rgba(195, 190, 178, 0.05) 0%,
    rgba(185, 180, 168, 0.025) 35%,
    transparent 70%);
  animation: crescentGlowPulse 10s ease-in-out infinite alternate;
  animation-delay: -3s;
}

/* Far glow — very faint atmospheric wash */
.crescent-glow-far {
  width: 280px;
  height: 220px;
  top: 50%;
  left: 30%;
  transform: translate(-50%, -50%);
  z-index: 0;
  background: radial-gradient(ellipse at 60% 50%,
    rgba(185, 180, 170, 0.025) 0%,
    rgba(175, 170, 160, 0.01) 40%,
    transparent 65%);
  animation: crescentGlowPulse 14s ease-in-out infinite alternate;
  animation-delay: -6s;
}

@keyframes crescentGlowPulse {
  0% { opacity: 0.5; transform: translate(-50%, -50%) scale(0.95); }
  100% { opacity: 0.85; transform: translate(-50%, -50%) scale(1.05); }
}

/* Crescent bloom — sits on the lit side only, NOT centered on disc */
.crescent-bloom {
  position: absolute;
  top: 45%;
  left: 20%;
  width: 60px;
  height: 70px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  z-index: 2;
  background: radial-gradient(ellipse at 55% 50%,
    rgba(220, 215, 200, 0.12) 0%,
    rgba(210, 205, 190, 0.06) 35%,
    rgba(200, 195, 180, 0.02) 65%,
    transparent 100%);
  animation: crescentBloomPulse 8s ease-in-out infinite alternate;
}

@keyframes crescentBloomPulse {
  0% { opacity: 0.4; transform: translate(-50%, -50%) scale(0.92); }
  100% { opacity: 0.75; transform: translate(-50%, -50%) scale(1.08); }
}

/* ===== Drifting Cloud Wisps (云层) ===== */
.moon-cloud {
  position: absolute;
  z-index: 4;
  pointer-events: none;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0;
  animation-fill-mode: both;
}

/* Cloud 1: thin wisp crossing upper moon area */
.moon-cloud-1 {
  width: 200px;
  height: 22px;
  top: 20%;
  left: -60%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(80, 72, 65, 0.12) 15%,
    rgba(100, 90, 80, 0.22) 35%,
    rgba(110, 100, 88, 0.3) 50%,
    rgba(100, 90, 80, 0.2) 65%,
    rgba(80, 72, 65, 0.1) 85%,
    transparent 100%);
  border-radius: 40%;
  filter: blur(6px);
  animation: cloudDrift1 25s linear infinite;
}

/* Cloud 2: thicker middle band — main veil */
.moon-cloud-2 {
  width: 260px;
  height: 35px;
  top: 38%;
  left: -80%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(70, 62, 55, 0.08) 10%,
    rgba(90, 80, 70, 0.2) 25%,
    rgba(105, 95, 82, 0.35) 45%,
    rgba(110, 100, 88, 0.38) 55%,
    rgba(100, 90, 78, 0.28) 70%,
    rgba(85, 75, 65, 0.12) 88%,
    transparent 100%);
  border-radius: 35%;
  filter: blur(10px);
  animation: cloudDrift2 35s linear infinite;
  animation-delay: -8s;
}

/* Cloud 3: trailing lower wisp */
.moon-cloud-3 {
  width: 180px;
  height: 18px;
  top: 62%;
  left: -50%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(85, 76, 68, 0.1) 20%,
    rgba(100, 90, 78, 0.2) 40%,
    rgba(95, 85, 75, 0.25) 55%,
    rgba(85, 76, 68, 0.15) 75%,
    transparent 100%);
  border-radius: 45%;
  filter: blur(7px);
  animation: cloudDrift3 30s linear infinite;
  animation-delay: -15s;
}

/* Cloud 4: very faint high wisp */
.moon-cloud-4 {
  width: 150px;
  height: 14px;
  top: 8%;
  left: -45%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(90, 82, 72, 0.06) 25%,
    rgba(100, 90, 80, 0.12) 50%,
    rgba(90, 82, 72, 0.06) 75%,
    transparent 100%);
  border-radius: 50%;
  filter: blur(5px);
  animation: cloudDrift4 20s linear infinite;
  animation-delay: -5s;
}

/* Cloud 5: large atmospheric haze band */
.moon-cloud-5 {
  width: 320px;
  height: 50px;
  top: 25%;
  left: -100%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(65, 58, 52, 0.04) 10%,
    rgba(80, 72, 62, 0.1) 25%,
    rgba(90, 82, 72, 0.16) 40%,
    rgba(95, 85, 75, 0.18) 50%,
    rgba(90, 82, 72, 0.14) 60%,
    rgba(80, 72, 62, 0.08) 78%,
    transparent 100%);
  border-radius: 30%;
  filter: blur(14px);
  animation: cloudDrift5 45s linear infinite;
  animation-delay: -20s;
}

/* Cloud drift animations — each layer at different speed/direction */
@keyframes cloudDrift1 {
  0%   { left: -70%; opacity: 0; }
  8%   { opacity: 0.7; }
  50%  { opacity: 0.9; }
  92%  { opacity: 0.6; }
  100% { left: 110%; opacity: 0; }
}

@keyframes cloudDrift2 {
  0%   { left: -90%; opacity: 0; }
  5%   { opacity: 0.5; }
  30%  { opacity: 0.85; }
  70%  { opacity: 0.8; }
  95%  { opacity: 0.4; }
  100% { left: 120%; opacity: 0; }
}

@keyframes cloudDrift3 {
  0%   { left: 120%; opacity: 0; }
  8%   { opacity: 0.6; }
  50%  { opacity: 0.8; }
  92%  { opacity: 0.5; }
  100% { left: -60%; opacity: 0; }
}

@keyframes cloudDrift4 {
  0%   { left: -50%; opacity: 0; }
  10%  { opacity: 0.4; }
  50%  { opacity: 0.6; }
  90%  { opacity: 0.3; }
  100% { left: 100%; opacity: 0; }
}

@keyframes cloudDrift5 {
  0%   { left: -110%; opacity: 0; }
  5%   { opacity: 0.3; }
  25%  { opacity: 0.65; }
  75%  { opacity: 0.6; }
  95%  { opacity: 0.25; }
  100% { left: 130%; opacity: 0; }
}

/* Stars — dimmer due to cloud cover */
.moon-star {
  position: absolute;
  width: 1.5px;
  height: 1.5px;
  border-radius: 50%;
  background: rgba(220, 215, 200, 0.4);
  box-shadow: 0 0 2px rgba(210, 205, 190, 0.25);
  animation: starTwinkle var(--dur, 5s) ease-in-out infinite alternate;
  animation-delay: var(--delay, 0s);
}

@keyframes starTwinkle {
  0% { opacity: 0.08; transform: scale(0.4); }
  50% { opacity: 0.5; transform: scale(1.1); }
  100% { opacity: 0.1; transform: scale(0.5); }
}

@keyframes crescentGlow {
  0% { opacity: 0.8; filter: drop-shadow(0 0 3px rgba(210,205,190,0.2)) drop-shadow(0 0 10px rgba(200,195,180,0.06)); }
  100% { opacity: 0.95; filter: drop-shadow(0 0 5px rgba(210,205,190,0.3)) drop-shadow(0 0 14px rgba(200,195,180,0.1)); }
}

/* ===== Mountain SVGs ===== */
.mountain-svg {
  position: absolute;
  bottom: 0;
  left: -3%;
  right: -3%;
  width: 106%;
  pointer-events: none;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: transform;
}

.mountain-far {
  height: 85%;
  z-index: 1;
  opacity: 0;
  animation: mountainReveal 2s ease-out 0.2s forwards;
}

.mountain-mid {
  height: 70%;
  z-index: 2;
  opacity: 0;
  animation: mountainReveal 2s ease-out 0.5s forwards;
}

.mountain-near {
  height: 55%;
  z-index: 3;
  opacity: 0;
  animation: mountainReveal 2s ease-out 0.8s forwards;
}

@keyframes mountainReveal {
  0% {
    opacity: 0;
    transform: translateY(30px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== Pine Trees Animation ===== */
.pine-trees {
  animation: treeSway 12s ease-in-out infinite alternate;
  transform-origin: bottom center;
}
.pine-trees-near {
  animation-duration: 10s;
}
@keyframes treeSway {
  0% { transform: skewX(-0.3deg); }
  100% { transform: skewX(0.4deg); }
}

/* ===== Inter-layer Mist ===== */
.layer-mist {
  position: absolute;
  left: -10%;
  right: -10%;
  width: 120%;
  pointer-events: none;
}

.layer-mist-far {
  bottom: 52%;
  height: 100px;
  z-index: 1;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(200, 190, 170, 0.025) 20%,
    rgba(210, 200, 180, 0.04) 40%,
    rgba(200, 190, 170, 0.03) 60%,
    rgba(190, 180, 160, 0.025) 80%,
    transparent 100%);
  filter: blur(20px);
  animation: mistDriftSlow 40s linear infinite;
  opacity: 0;
  animation: mistDriftSlow 40s linear infinite, mountainReveal 3s ease-out 1s forwards;
}

.layer-mist-mid {
  bottom: 32%;
  height: 80px;
  z-index: 3;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(180, 170, 150, 0.03) 15%,
    rgba(200, 190, 170, 0.05) 35%,
    rgba(190, 180, 160, 0.04) 55%,
    rgba(180, 170, 150, 0.03) 75%,
    transparent 100%);
  filter: blur(15px);
  opacity: 0;
  animation: mistDriftSlow 35s linear infinite reverse, mountainReveal 3s ease-out 1.3s forwards;
}

@keyframes mistDriftSlow {
  0% { transform: translateX(0); }
  100% { transform: translateX(-20%); }
}

/* ===== Water Surface ===== */
.water-surface {
  position: absolute;
  bottom: 0;
  left: -3%;
  right: -3%;
  width: 106%;
  height: 26%;
  z-index: 4;
  overflow: hidden;
  opacity: 0;
  animation: mountainReveal 2.5s ease-out 1.2s forwards;
}

/* Water base — dark translucent surface */
.water-base {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg,
    rgba(15, 12, 25, 0.15) 0%,
    rgba(18, 15, 30, 0.25) 20%,
    rgba(20, 16, 32, 0.35) 50%,
    rgba(16, 13, 28, 0.45) 100%);
}

/* SVG Reflection layer */
.water-reflection-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* ===== Waterline Glow ===== */
.waterline-glow {
  position: absolute;
  top: -2px;
  left: 0;
  right: 0;
  height: 6px;
  z-index: 10;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(160, 145, 120, 0.06) 10%,
    rgba(200, 185, 155, 0.12) 25%,
    rgba(220, 200, 170, 0.18) 40%,
    rgba(240, 225, 195, 0.22) 50%,
    rgba(220, 200, 170, 0.18) 60%,
    rgba(200, 185, 155, 0.12) 75%,
    rgba(160, 145, 120, 0.06) 90%,
    transparent 100%);
  filter: blur(2px);
  animation: waterlineShimmer 8s ease-in-out infinite alternate;
}

@keyframes waterlineShimmer {
  0% { opacity: 0.5; transform: scaleX(0.98); }
  50% { opacity: 1; transform: scaleX(1.01); }
  100% { opacity: 0.6; transform: scaleX(0.99); }
}

/* ===== Wave Strips — Sine-wave horizontal ripples ===== */
.wave-strips {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.wave-strip {
  position: absolute;
  left: -5%;
  right: -5%;
  width: 110%;
  height: 2px;
  background:
    repeating-linear-gradient(90deg,
      transparent 0px,
      rgba(200, 190, 170, 0.04) 40px,
      rgba(220, 210, 190, 0.1) 80px,
      rgba(235, 225, 200, 0.15) 120px,
      rgba(220, 210, 190, 0.1) 160px,
      rgba(200, 190, 170, 0.04) 200px,
      transparent 240px);
  opacity: 0;
  animation: waveStripReveal 2s ease-out forwards;
}

/* Staggered positioning and animation */
.wave-strip-1 { top: 8%;  animation-delay: 1.5s; animation: waveStripReveal 2s ease-out 1.5s forwards, waveRipple 7s ease-in-out infinite; }
.wave-strip-2 { top: 18%; animation-delay: 1.7s; animation: waveStripReveal 2s ease-out 1.7s forwards, waveRipple 9s ease-in-out infinite; }
.wave-strip-3 { top: 30%; animation-delay: 1.9s; animation: waveStripReveal 2s ease-out 1.9s forwards, waveRipple 11s ease-in-out infinite; }
.wave-strip-4 { top: 42%; animation-delay: 2.1s; animation: waveStripReveal 2s ease-out 2.1s forwards, waveRipple 8s ease-in-out infinite; }
.wave-strip-5 { top: 55%; animation-delay: 2.3s; animation: waveStripReveal 2s ease-out 2.3s forwards, waveRipple 10s ease-in-out infinite; }
.wave-strip-6 { top: 65%; animation-delay: 2.5s; animation: waveStripReveal 2s ease-out 2.5s forwards, waveRipple 7.5s ease-in-out infinite; }
.wave-strip-7 { top: 78%; animation-delay: 2.7s; animation: waveStripReveal 2s ease-out 2.7s forwards, waveRipple 9.5s ease-in-out infinite; }
.wave-strip-8 { top: 90%; animation-delay: 2.9s; animation: waveStripReveal 2s ease-out 2.9s forwards, waveRipple 8.5s ease-in-out infinite; }

/* Lower strips get fainter */
.wave-strip-5, .wave-strip-6 { height: 1.5px; opacity: 0; }
.wave-strip-7, .wave-strip-8 { height: 1px; opacity: 0; }

@keyframes waveStripReveal {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

@keyframes waveRipple {
  0% {
    transform: translateX(0) scaleX(0.95);
    opacity: 0.3;
  }
  25% {
    transform: translateX(12px) scaleX(1.02);
    opacity: 0.7;
  }
  50% {
    transform: translateX(-8px) scaleX(0.98);
    opacity: 0.5;
  }
  75% {
    transform: translateX(6px) scaleX(1.01);
    opacity: 0.8;
  }
  100% {
    transform: translateX(0) scaleX(0.95);
    opacity: 0.3;
  }
}

/* ===== Moonlight Path (月光路) ===== */
.moonlight-path {
  position: absolute;
  top: 0;
  right: 15%;
  width: 120px;
  height: 100%;
  z-index: 5;
  pointer-events: none;
}

/* Moon disc reflection at water surface — round bright spot */
.moon-disc-reflection {
  position: absolute;
  top: -8px;
  left: 50%;
  width: 28px;
  height: 16px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse,
    rgba(255, 252, 240, 0.4) 0%,
    rgba(255, 248, 225, 0.25) 35%,
    rgba(245, 238, 210, 0.1) 65%,
    transparent 100%);
  filter: blur(2px);
  animation: discReflWave 5s ease-in-out infinite;
}

@keyframes discReflWave {
  0%, 100% {
    transform: translateX(-50%) scaleX(0.8) scaleY(0.9);
    opacity: 0.5;
  }
  30% {
    transform: translateX(-50%) scaleX(1.4) scaleY(1.1);
    opacity: 0.9;
  }
  60% {
    transform: translateX(-50%) scaleX(1.1) scaleY(0.95);
    opacity: 0.7;
  }
}

/* Central bright column — narrow bright core */
.moonpath-center {
  position: absolute;
  top: 0;
  left: 50%;
  width: 6px;
  height: 100%;
  transform: translateX(-50%);
  background: linear-gradient(180deg,
    rgba(255, 252, 240, 0.2) 0%,
    rgba(255, 248, 225, 0.15) 15%,
    rgba(245, 238, 210, 0.1) 35%,
    rgba(240, 232, 200, 0.06) 55%,
    rgba(235, 228, 196, 0.03) 75%,
    transparent 100%);
  filter: blur(3px);
  animation: centerColumnWave 6s ease-in-out infinite;
}

@keyframes centerColumnWave {
  0%, 100% {
    transform: translateX(-50%) scaleX(1);
    opacity: 0.6;
  }
  25% {
    transform: translateX(-50%) scaleX(2.5);
    opacity: 0.85;
  }
  50% {
    transform: translateX(-50%) scaleX(1.5);
    opacity: 0.5;
  }
  75% {
    transform: translateX(-50%) scaleX(3);
    opacity: 0.75;
  }
}

/* Glow wings — flanking the center, creating the V-shape */
.moonpath-wing {
  position: absolute;
  top: 5%;
  width: 50%;
  height: 95%;
}

.moonpath-wing-left {
  left: 0;
  background: linear-gradient(180deg,
    rgba(255, 248, 225, 0.08) 0%,
    rgba(245, 238, 210, 0.06) 20%,
    rgba(235, 228, 200, 0.04) 45%,
    rgba(225, 218, 190, 0.02) 70%,
    transparent 100%);
  mask-image: linear-gradient(to right, transparent 0%, rgba(255,255,255,0.4) 60%, rgba(255,255,255,0.8) 100%);
  -webkit-mask-image: linear-gradient(to right, transparent 0%, rgba(255,255,255,0.4) 60%, rgba(255,255,255,0.8) 100%);
  filter: blur(4px);
  animation: wingWaveLeft 8s ease-in-out infinite;
}

.moonpath-wing-right {
  right: 0;
  background: linear-gradient(180deg,
    rgba(255, 248, 225, 0.08) 0%,
    rgba(245, 238, 210, 0.06) 20%,
    rgba(235, 228, 200, 0.04) 45%,
    rgba(225, 218, 190, 0.02) 70%,
    transparent 100%);
  mask-image: linear-gradient(to left, transparent 0%, rgba(255,255,255,0.4) 60%, rgba(255,255,255,0.8) 100%);
  -webkit-mask-image: linear-gradient(to left, transparent 0%, rgba(255,255,255,0.4) 60%, rgba(255,255,255,0.8) 100%);
  filter: blur(4px);
  animation: wingWaveRight 8s ease-in-out infinite;
}

@keyframes wingWaveLeft {
  0%, 100% { opacity: 0.3; transform: skewX(0deg); }
  30% { opacity: 0.6; transform: skewX(-2deg); }
  70% { opacity: 0.5; transform: skewX(1deg); }
}

@keyframes wingWaveRight {
  0%, 100% { opacity: 0.3; transform: skewX(0deg); }
  30% { opacity: 0.5; transform: skewX(2deg); }
  70% { opacity: 0.6; transform: skewX(-1deg); }
}

/* Sparkle spots within the moonlight path */
.moonpath-sparkle {
  position: absolute;
  width: var(--s, 3px);
  height: var(--s, 3px);
  border-radius: 50%;
  background: rgba(255, 252, 240, 0.7);
  box-shadow: 0 0 6px rgba(255, 248, 220, 0.4);
  animation: moonpathGlint 3.5s ease-in-out infinite alternate;
  animation-delay: var(--d, 0s);
}

@keyframes moonpathGlint {
  0% { opacity: 0; transform: scale(0.3); }
  40% { opacity: 0.9; transform: scale(1.2); }
  70% { opacity: 0.4; transform: scale(0.8); }
  100% { opacity: 0; transform: scale(0.2); }
}

/* ===== Water Sparkles ===== */
.water-sparkles {
  position: absolute;
  inset: 0;
  z-index: 6;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  width: var(--size, 2px);
  height: var(--size, 2px);
  border-radius: 50%;
  background: rgba(255, 250, 230, 0.5);
  box-shadow: 0 0 4px rgba(255, 248, 220, 0.3);
  animation: sparkleGlint ease-in-out infinite alternate;
}

@keyframes sparkleGlint {
  0% {
    opacity: 0;
    transform: scale(0.5);
  }
  40% {
    opacity: 0.9;
    transform: scale(1.2);
  }
  100% {
    opacity: 0;
    transform: scale(0.3);
  }
}

/* ===== Water Depth Overlay ===== */
.water-depth {
  position: absolute;
  inset: 0;
  z-index: 7;
  pointer-events: none;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(12, 10, 20, 0.08) 30%,
    rgba(10, 8, 18, 0.18) 60%,
    rgba(8, 6, 15, 0.3) 100%);
}


/* ===== Floating Mist Wisps ===== */
.wisp {
  position: absolute;
  width: 300px;
  height: 40px;
  border-radius: 50%;
  filter: blur(25px);
  pointer-events: none;
  opacity: 0;
}

.wisp-1 {
  bottom: 35%;
  left: 10%;
  background: rgba(200, 190, 170, 0.04);
  animation: wispFloat 25s ease-in-out infinite, mountainReveal 3s ease-out 2s forwards;
}

.wisp-2 {
  bottom: 20%;
  right: 15%;
  width: 250px;
  background: rgba(190, 180, 160, 0.035);
  animation: wispFloat 30s ease-in-out infinite reverse, mountainReveal 3s ease-out 2.5s forwards;
}

.wisp-3 {
  bottom: 45%;
  left: 40%;
  width: 200px;
  height: 30px;
  background: rgba(210, 200, 180, 0.03);
  animation: wispFloat 35s ease-in-out infinite, mountainReveal 3s ease-out 3s forwards;
}

@keyframes wispFloat {
  0%, 100% {
    transform: translateX(0) translateY(0);
  }
  25% {
    transform: translateX(60px) translateY(-10px);
  }
  50% {
    transform: translateX(-30px) translateY(8px);
  }
  75% {
    transform: translateX(40px) translateY(-5px);
  }
}

/* ===== Flying Birds ===== */
.flying-birds {
  position: absolute;
  top: 8%;
  left: 25%;
  width: 120px;
  height: 50px;
  z-index: 5;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  animation: birdsFloat 20s ease-in-out infinite;
}

.bird-group {
  animation: birdFlap 3s ease-in-out infinite;
}

@keyframes birdsFloat {
  0%, 100% {
    transform: translateX(0) translateY(0);
  }
  25% {
    transform: translateX(50px) translateY(-15px);
  }
  50% {
    transform: translateX(80px) translateY(-5px);
  }
  75% {
    transform: translateX(30px) translateY(-20px);
  }
}

@keyframes birdFlap {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.85); }
}

/* ===== Boat Animation ===== */
.boat-silhouette {
  animation: boatDrift 15s ease-in-out infinite;
  transform-origin: center bottom;
}
@keyframes boatDrift {
  0%, 100% { transform: translateX(0) rotate(0deg); }
  25% { transform: translateX(5px) rotate(0.5deg); }
  50% { transform: translateX(-3px) rotate(-0.3deg); }
  75% { transform: translateX(4px) rotate(0.3deg); }
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .mountain-scene {
    height: 40vh;
  }
  .flying-birds {
    display: none;
  }
  .moon-wrapper {
    right: 10%;
    top: 5%;
    width: 36px;
    height: 36px;
  }
  .moon-body {
    width: 36px;
    height: 36px;
  }
  .moon-scatter,
  .moon-halo-3,
  .moonlight-cone {
    display: none;
  }
  .wisp {
    display: none;
  }
  .moonlight-path {
    width: 60px;
  }
}

@media (max-height: 700px) {
  .mountain-scene {
    height: 35vh;
  }
}
.moon-wrapper,
.moonlight-path {
  display: none !important;
}
</style>
