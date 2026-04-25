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

if (!window.localStorage || typeof window.localStorage.getItem !== 'function') {
  const store = new Map<string, string>()
  const localStorageMock = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, String(value))
    },
    removeItem: (key: string) => {
      store.delete(key)
    },
    clear: () => {
      store.clear()
    },
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    get length() {
      return store.size
    },
  }

  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    writable: true,
    value: localStorageMock,
  })
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    writable: true,
    value: localStorageMock,
  })
}

afterEach(() => {
  if (window.localStorage && typeof window.localStorage.clear === 'function') {
    window.localStorage.clear()
  }
})
