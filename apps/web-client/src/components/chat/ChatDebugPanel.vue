<template>
  <div class="debug-panel bg-gray-800 text-white p-2 text-xs flex gap-4">
    <div>🔌 WebSocket: {{ wsStore.isConnected ? '✅' : '❌' }}</div>
    <div>📡 Подписка: {{ isSubscribed ? '✅' : '❌' }}</div>
    <div>💬 Сообщений: {{ messagesCount }}</div>
  </div>
</template>

<script setup lang="ts">
import { useWebSocketStore } from '@/stores/webSocketStore'

defineProps<{
  isSubscribed: boolean
  messagesCount: number
}>()

const wsStore = useWebSocketStore()
</script>
