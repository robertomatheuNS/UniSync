import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],

  base: '/',

  server: {
    host: 'localhost',
    port: 5173,
    origin: 'http://localhost:5173',
    cors: true,
    watch: {
      usePolling: process.env.VITE_USE_POLLING === 'true',
    },
  },

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
