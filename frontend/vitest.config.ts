import { defineConfig, mergeConfig } from 'vite'
import baseConfig from './vite.config'

export default mergeConfig(baseConfig, defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
}))
