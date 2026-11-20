<template>
  <section class="flex flex-col shadow-lg p-3 rounded-xl border border-[#8295ff]/35">
    <div class="flex items-center gap-2">
      <!-- Иконка корзины -->
      <svg
        class="w-6 h-6 text-red-500 shrink-0"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="3 6 5 6 21 6" />
        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        <line x1="10" y1="11" x2="10" y2="17" />
        <line x1="14" y1="11" x2="14" y2="17" />
      </svg>
      <h3 class="text-lg font-semibold text-gray-800">Удалённых пользователей</h3>
    </div>

    <div class="flex justify-center mt-2">
      <!-- Скелетон при загрузке -->
      <div
        v-if="statisticsStore.isLoading"
        class="h-8 w-16 bg-gray-200 rounded-md animate-pulse"
      />
      <span v-else-if="statisticsStore.error" class="text-sm text-red-500 text-center pt-2">
        {{ statisticsStore.error }}
      </span>
      <span v-else class="text-2xl font-bold text-red-500 tabular-nums">
        {{ statisticsStore.deletedUsers }}
      </span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useStatisticsStore } from '@/stores/usersStatisticStore'

const statisticsStore = useStatisticsStore()

// Загружаем статистику при загрузке приложения
onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    statisticsStore.init()
  }
})
</script>