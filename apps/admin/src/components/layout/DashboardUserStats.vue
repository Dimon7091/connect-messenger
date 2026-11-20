<template>
  <div class="bg-white rounded-xl shadow-lg border border-[#8295ff]/35 overflow-hidden">
    <div class="bg-blue-500 py-2 px-6">
      <h3 class="text-lg font-bold text-white flex items-center gap-2">
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        Статистика пользователей
      </h3>
    </div>

    <div class="p-5">
      <!-- Скелетон при загрузке -->
      <div v-if="statisticsStore.isLoading" class="grid grid-cols-4 gap-4">
        <div v-for="i in 4" :key="i" class="h-24 bg-gray-100 rounded-lg animate-pulse" />
      </div>

      <div v-else-if="statisticsStore.error" class="text-sm text-red-500 text-center py-4">
        {{ statisticsStore.error }}
      </div>

      <div v-else class="grid grid-cols-4 gap-4">

        <!-- 1. Всего пользователей -->
        <div class="flex flex-col items-center gap-2 p-4 rounded-xl bg-blue-50/50 border border-blue-100">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <span class="text-sm font-semibold text-gray-700">Всего пользователей</span>
          </div>
          <span class="text-3xl font-bold text-blue-600 tabular-nums">{{ statisticsStore.totalUsers }}</span>
          <div class="w-full mt-1">
            <div class="w-full h-2 bg-gray-100 rounded-full overflow-hidden flex">
              <div v-if="activePercent > 0" class="h-full bg-green-500 rounded-l-full transition-all duration-500" :style="{ width: activePercent + '%' }" />
              <div v-if="deletedPercent > 0" class="h-full bg-red-400 rounded-r-full transition-all duration-500" :style="{ width: deletedPercent + '%' }" />
            </div>
          </div>
        </div>

        <!-- 2. Активных -->
        <div class="flex flex-col items-center gap-2 p-4 rounded-xl bg-green-50/50 border border-green-100">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-green-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <polyline points="16 3 19 6 23 2" />
            </svg>
            <span class="text-sm font-semibold text-gray-700">Активных</span>
          </div>
          <span class="text-3xl font-bold text-green-600 tabular-nums">{{ statisticsStore.activeUsers }}</span>
          <span class="text-xs text-gray-500 font-medium">{{ activePercent }}% от всех</span>
        </div>

        <!-- 3. Удаленных -->
        <div class="flex flex-col items-center gap-2 p-4 rounded-xl bg-red-50/50 border border-red-100">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-red-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              <line x1="10" y1="11" x2="10" y2="17" />
              <line x1="14" y1="11" x2="14" y2="17" />
            </svg>
            <span class="text-sm font-semibold text-gray-700">Удаленных</span>
          </div>
          <span class="text-3xl font-bold text-red-600 tabular-nums">{{ statisticsStore.deletedUsers }}</span>
          <span class="text-xs text-gray-500 font-medium">{{ deletedPercent }}% от всех</span>
        </div>

        <!-- 4. Онлайн -->
        <div class="flex flex-col items-center gap-2 p-4 rounded-xl bg-amber-50/50 border border-amber-100">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-amber-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <circle cx="9" cy="7" r="2" fill="currentColor" />
            </svg>
            <span class="text-sm font-semibold text-gray-700">Онлайн</span>
          </div>
          <span class="text-3xl font-bold text-amber-600 tabular-nums">{{ statisticsStore.onlineUsers }}</span>
          <span class="text-xs text-gray-500 font-medium">сейчас</span>
        </div>

      </div>
    </div>
  </div>
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

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    statisticsStore.init()
  }
})
</script>
