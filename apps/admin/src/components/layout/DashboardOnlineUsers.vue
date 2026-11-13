<template>
  <section class="flex-row items-center shadow-lg p-3 rounded-xl border border-[#8295ff]/35">
    <div class="flex justify-center">
      <h3 class="text-xl">Пользователей онлайн</h3>
    </div>
    <div class="flex justify-center">
      <span v-if="statisticsStore.isLoading" class="pt-2 text-xl font-bold text-green-500">
        Загрузка...
      </span>
      <span v-else-if="statisticsStore.error" class="pt-2 text-xl font-bold text-red-500">
        Ошибка:{{ statisticsStore.error }}
      </span>
      <span v-else class="pt-2 text-xl font-bold text-green-500">
        {{ statisticsStore.onlineUsers }}
      </span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useStatisticsStore } from '@/stores/usersStat'

const statisticsStore = useStatisticsStore()

// Загружаем статистику при загрузке приложения
onMounted(() => {
  // Проверяем, есть ли токен (пользователь авторизован)
  const token = localStorage.getItem('token')
  if (token) {
    statisticsStore.init()
  }
})
</script>
