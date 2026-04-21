import type { DirectiveBinding, ObjectDirective } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

export type RevealPreset = 'hero' | 'section' | 'card' | 'sidebar' | 'footer'

export interface RevealDirectiveValue {
  preset?: RevealPreset
  delay?: number
  once?: boolean
}

interface RevealPresetConfig {
  duration: number
  ease: string
  start: string
  x: number
  y: number
  scale: number
  blur: number
}

interface ManagedRevealElement extends HTMLElement {
  __revealCleanup__?: () => void
  __revealSignature__?: string
}

let motionInitialized = false

export function initMotion() {
  if (motionInitialized || typeof window === 'undefined') {
    return
  }

  gsap.registerPlugin(ScrollTrigger)
  motionInitialized = true
}

export function prefersReducedMotion() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false
  }

  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function readMotionNumber(tokenName: string, fallback: number) {
  if (typeof window === 'undefined') {
    return fallback
  }

  const rawValue = window.getComputedStyle(document.documentElement).getPropertyValue(tokenName).trim()
  const parsed = Number.parseFloat(rawValue)
  return Number.isFinite(parsed) ? parsed : fallback
}

function normalizeRevealOptions(value?: RevealPreset | RevealDirectiveValue): Required<RevealDirectiveValue> {
  if (!value) {
    return { preset: 'section', delay: 0, once: true }
  }

  if (typeof value === 'string') {
    return { preset: value, delay: 0, once: true }
  }

  return {
    preset: value.preset ?? 'section',
    delay: value.delay ?? 0,
    once: value.once ?? true,
  }
}

function resolvePresetConfig(preset: RevealPreset): RevealPresetConfig {
  const fast = readMotionNumber('--motion-duration-fast', 0.2)
  const medium = readMotionNumber('--motion-duration-medium', 0.42)
  const slow = readMotionNumber('--motion-duration-slow', 0.68)
  const distanceSm = readMotionNumber('--motion-distance-sm', 14)
  const distanceMd = readMotionNumber('--motion-distance-md', 24)
  const distanceLg = readMotionNumber('--motion-distance-lg', 40)
  const blurEnter = readMotionNumber('--motion-blur-enter', 12)
  const easeEnter = readMotionString('--motion-ease-enter', 'power2.out')
  const easeEmphasis = readMotionString('--motion-ease-emphasis', 'power3.out')

  const presets: Record<RevealPreset, RevealPresetConfig> = {
    hero: {
      duration: slow,
      ease: easeEmphasis,
      start: 'top 92%',
      x: 0,
      y: distanceLg,
      scale: 0.985,
      blur: blurEnter,
    },
    section: {
      duration: medium,
      ease: easeEnter,
      start: 'top 88%',
      x: 0,
      y: distanceMd,
      scale: 0.992,
      blur: blurEnter * 0.85,
    },
    card: {
      duration: fast,
      ease: easeEnter,
      start: 'top 92%',
      x: 0,
      y: distanceSm,
      scale: 0.985,
      blur: blurEnter * 0.7,
    },
    sidebar: {
      duration: medium,
      ease: easeEmphasis,
      start: 'top 90%',
      x: -distanceMd,
      y: distanceSm,
      scale: 0.995,
      blur: blurEnter * 0.8,
    },
    footer: {
      duration: slow,
      ease: easeEnter,
      start: 'top 94%',
      x: 0,
      y: distanceLg,
      scale: 0.995,
      blur: blurEnter,
    },
  }

  return presets[preset]
}

function readMotionString(tokenName: string, fallback: string) {
  if (typeof window === 'undefined') {
    return fallback
  }

  const rawValue = window.getComputedStyle(document.documentElement).getPropertyValue(tokenName).trim()
  return rawValue || fallback
}

function resetRevealElement(el: ManagedRevealElement) {
  el.__revealCleanup__?.()
  delete el.__revealCleanup__
  delete el.__revealSignature__
}

function applyReducedMotionState(el: ManagedRevealElement) {
  gsap.killTweensOf(el)
  gsap.set(el, {
    autoAlpha: 1,
    clearProps: 'opacity,visibility,transform,filter,willChange',
  })
  el.classList.add('motion-reveal--ready')
}

function setupReveal(el: ManagedRevealElement, optionsValue?: RevealPreset | RevealDirectiveValue) {
  initMotion()

  const options = normalizeRevealOptions(optionsValue)
  const signature = `${options.preset}:${options.delay}:${options.once}`

  if (el.__revealSignature__ === signature) {
    return
  }

  if (!(el instanceof HTMLElement) || !el.isConnected) {
    return
  }

  resetRevealElement(el)
  el.__revealSignature__ = signature
  el.classList.add('motion-reveal', `motion-reveal--${options.preset}`)

  if (prefersReducedMotion()) {
    applyReducedMotionState(el)
    return
  }

  if (el.offsetParent === null) {
    requestAnimationFrame(() => {
      if (el.offsetParent === null) {
        applyReducedMotionState(el)
        return
      }
      setupRevealWithScroll(el, options, signature)
    })
    return
  }

  setupRevealWithScroll(el, options, signature)
}

function setupRevealWithScroll(el: ManagedRevealElement, options: Required<RevealDirectiveValue>, signature: string) {
  if (el.__revealSignature__ === signature) {
    return
  }

  const preset = resolvePresetConfig(options.preset)
  const fromVars: gsap.TweenVars = {
    autoAlpha: 0,
    x: preset.x,
    y: preset.y,
    scale: preset.scale,
    filter: `blur(${preset.blur}px)`,
    transformOrigin: '50% 50%',
    willChange: 'transform, opacity, filter',
    force3D: true,
  }

  gsap.set(el, fromVars)

  const playReveal = () => {
    if (el.classList.contains('motion-reveal--ready')) {
      return
    }

    gsap.to(el, {
      autoAlpha: 1,
      x: 0,
      y: 0,
      scale: 1,
      filter: 'blur(0px)',
      duration: preset.duration,
      delay: options.delay,
      ease: preset.ease,
      overwrite: 'auto',
      onStart: () => {
        el.classList.add('motion-reveal--animating')
      },
      onComplete: () => {
        el.classList.remove('motion-reveal--animating')
        el.classList.add('motion-reveal--ready')
        gsap.set(el, { clearProps: 'willChange' })
      },
    })
  }

  const resetState = () => {
    el.classList.remove('motion-reveal--ready', 'motion-reveal--animating')
    gsap.set(el, fromVars)
  }

  const trigger = ScrollTrigger.create({
    trigger: el,
    start: preset.start,
    once: options.once,
    onEnter: playReveal,
    onEnterBack: options.once ? undefined : playReveal,
    onLeaveBack: options.once ? undefined : resetState,
  })

  el.__revealCleanup__ = () => {
    trigger.kill()
    gsap.killTweensOf(el)
    gsap.set(el, {
      clearProps: 'opacity,visibility,transform,filter,willChange',
    })
    el.classList.remove('motion-reveal--animating', 'motion-reveal--ready', `motion-reveal--${options.preset}`)
  }

  requestAnimationFrame(() => ScrollTrigger.refresh())
}

export function createRevealDirective(): ObjectDirective<HTMLElement, RevealPreset | RevealDirectiveValue> {
  return {
    mounted(el, binding) {
      setupReveal(el as ManagedRevealElement, binding.value)
    },
    updated(el, binding: DirectiveBinding<RevealPreset | RevealDirectiveValue>) {
      setupReveal(el as ManagedRevealElement, binding.value)
    },
    unmounted(el) {
      resetRevealElement(el as ManagedRevealElement)
    },
  }
}

export function animateEntrance(targets: gsap.TweenTarget, vars: gsap.TweenVars) {
  initMotion()
  if (prefersReducedMotion()) {
    gsap.set(targets, { clearProps: 'opacity,visibility,transform,filter,willChange' })
    return null
  }

  return gsap.fromTo(targets, vars.from ?? {}, vars.to ?? {})
}

export { gsap, ScrollTrigger }
