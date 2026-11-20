<template>
  <section class="flex flex-col shadow-lg p-3 rounded-xl border border-[#8295ff]/35">
    <div class="flex items-center gap-2">
      <!-- Иконка пользователей -->
      <svg
        class="w-6 h-6 text-blue-500 shrink-0"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
      </svg>
      <h3 class="text-lg font-semibold text-gray-800">Всего</h3>
    </div>

    <div class="flex justify-center mt-2">
      <!-- Скелетон при загрузке -->
      <div
        v-if="statisticsStore.isLoading"
        class="flex flex-col items-center gap-2 w-full"
      >
        <div class="h-8 w-20 bg-gray-200 rounded-md animate-pulse" />
        <div class="h-2 w-full bg-gray-100 rounded-full animate-pulse" />
      </div>

      <div v-else-if="statisticsStore.error" class="text-sm text-red-500 text-center pt-2">
        {{ statisticsStore.error }}
      </div>

      <div v-else class="flex flex-col items-center w-full">
        <span class="text-2xl font-bold text-blue-500 tabular-nums">
          {{ statisticsStore.totalUsers }}
        </span>

        <!-- Прогресс-бар: активные vs удалённые -->
        <div
          v-if="statisticsStore.totalUsers > 0"
          class="mt-2 w-full h-2 bg-gray-100 rounded-full overflow-hidden flex"
        >
          <div
            v-if="activePercent > 0"
            class="h-full transition-all duration-500"
            :style="{ width: activePercent + '%' }"
            :class="activePercent > 0 ? 'bg-green-500' : ''"
            :title="`Активных: ${statisticsStore.activeUsers}`"
          />
          <div
            v-if="deletedPercent > 0"
            class="h-full transition-all duration-500"
            :style="{ width: deletedPercent + '%' }"
            :class="deletedPercent > 0 ? 'bg-red-400' : ''"
            :title="`Удалённых: ${statisticsStore.deletedUsers}`"
          />
        </div>

        <!-- Подписи под прогресс-баром -->
        <div
          v-if="statisticsStore.totalUsers > 0"
          class="flex justify-between w-full mt-1 text-xs text-gray-400"
        >
          <span>▬ активные {{ statisticsStore.activeUsers }}</span>
          <span>▬ удалённые {{ statisticsStore.deletedUsers }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useStatisticsStore } from '@/stores/usersStatisticStore'

const statisticsStore = useStatisticsStore()

const activePercent = computed(() => {
  if (!statisticsStore.totalUsers) return 0
  return Math.round((statisticsStore.activeUsers / statisticsStore.totalUsers) * 100)
})

const deletedPercent = computed(() => {
  if (!statisticsStore.totalUsers) return 0
  return Math.round((statisticsStore.deletedUsers / statisticsStore.totalUsers) * 100)
})

// Загружаем статистику при загрузке приложения
onMounted(() => {
  // Проверяем, есть ли токен (пользователь авторизован)
  const token = localStorage.getItem('token')
  if (token) {
    statisticsStore.init()
  }
})
</script>
