import { afterEach } from 'vitest'
import { config } from '@vue/test-utils'

config.global.stubs = {
  transition: false,
  Transition: false,
}

class MockMessageChannel {
  port1 = {
    onmessage: null as ((event: MessageEvent) => void) | null,
    postMessage: () => {},
    start: () => {},
    close: () => {},
  }

  port2 = {
    postMessage: () => {},
    start: () => {},
    close: () => {},
  }
}

if (!('MessageChannel' in globalThis)) {
  Object.defineProperty(globalThis, 'MessageChannel', {
    configurable: true,
    writable: true,
    value: MockMessageChannel,
  })
}

if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  })
}

afterEach(() => {
  if (window.localStorage && typeof window.localStorage.clear === 'function') {
    window.localStorage.clear()
  }
})
