import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173
  },
  build: {
    // 代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          // 将 Vue 相关库打包到一起
          'vue-vendor': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
          // 将 ECharts 单独打包（体积较大）
          'echarts': ['echarts'],
          // 将 axios 单独打包
          'axios': ['axios']
        }
      }
    },
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 移除 console
        drop_debugger: true
      }
    },
    // 启用 CSS 代码分割
    cssCodeSplit: true,
    // 设置 chunk 大小警告限制
    chunkSizeWarningLimit: 1000
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'vue-i18n'],
    exclude: ['echarts'] // ECharts 较大，按需加载
  }
})
