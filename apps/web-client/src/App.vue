<template>
  <div
    id="app"
    class="relative h-[100dvh] max-w-5xl mx-auto shadow-[15px_0_15px_-5px_rgba(0,0,0,0.15),-15px_0_15px_-5px_rgba(0,0,0,0.15)]"
  >
    <div id="modal-root"></div>
    <router-view />
    <GalleryModal />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useWebSocketStore } from '@/stores/webSocketStore'
import { useUserStore } from '@/stores/userStore'
import { useSettingsStore } from '@/stores/settings'
import GalleryModal from '@/components/GalleryModal.vue'

const wsStore = useWebSocketStore()
const userStore = useUserStore()

onMounted(() => {
  // Инициализируем WebSocket после авторизации
  if (userStore.isAuthenticated && userStore.token) {
    wsStore.init()
    console.log(`WebSocket connection status`)
  }
  // Инициализируем стор настроек
  useSettingsStore()
})

onUnmounted(() => {
  wsStore.disconnect()
})
</script>
