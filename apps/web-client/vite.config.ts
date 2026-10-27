import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA, type ManifestOptions } from 'vite-plugin-pwa'

const TARGET_HOST = '127.0.0.1'

interface CustomManifestOptions extends Partial<ManifestOptions> {
  gcm_sender_id?: string
}

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'firebase-messaging-sw.ts',
      registerType: 'autoUpdate',
      devOptions: {
        enabled: true,
        type: 'module',
      },
      includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'masked-icon.svg'],
      manifest: {
        name: 'Connect messenger',
        short_name: 'Connect',
        description: 'Приложение для общения',
        theme_color: '#42b883',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [
          { src: '/icon-connect-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icon-connect-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/icon-connect-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable',
          },
        ],
        gcm_sender_id: '103953800507',
      } as CustomManifestOptions,
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5174,
    host: 'localhost',
    strictPort: true, // 👈 1. Запрещаем Vite менять порт, если 5174 занят
    hmr: {
      clientPort: 5174, // 👈 2. Принудительно направляем HMR-трафик на этот же порт
    },
    allowedHosts: true,
    proxy: {
      '/api': {
        target: `http://${TARGET_HOST}:8080`,
        changeOrigin: true,
      },
      '/chat-ws': {
        target: `ws://${TARGET_HOST}:8080`,
        ws: true,
        changeOrigin: true,
        rewrite: (path) => path,
      },
    },
    watch: {
      usePolling: true,
    },
  },
  preview: {
    port: 5174,
    allowedHosts: true,
  },
  define: {
    global: 'globalThis',
    'process.env': {},
  },
  optimizeDeps: {
    include: ['sockjs-client'],
  },
})
