<template>
  <section class="flex flex-col shadow-lg p-3 rounded-xl border border-[#8295ff]/35">
    <div class="flex items-center gap-2">
      <!-- Иконка активного пользователя -->
      <svg
        class="w-6 h-6 text-indigo-500 shrink-0"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <polyline points="16 3 19 6 23 2" />
      </svg>
      <h3 class="text-lg font-semibold text-gray-800">Активных пользователей</h3>
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
      <span v-else class="text-2xl font-bold text-indigo-500 tabular-nums">
        {{ statisticsStore.activeUsers }}
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