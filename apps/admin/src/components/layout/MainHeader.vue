<template>
  <header
    class="bg-linear-to-r from-indigo-600 to-cyan-600 px-6 py-2 flex justify-between items-center text-white shadow-lg"
  >
    <div class="flex-1">
      <h1 class="text-xl font-semibold tracking-wide">Админ панель</h1>
    </div>

    <div class="flex items-center gap-3">
      <!-- Аватар -->
      <div class="font-bold">Администатор:</div>

      <!-- Данные администратора -->
      <div class="text-sm font-medium p-1">
        <div v-if="authStore.isLoading" class="flex items-center gap-2">
          <span
            class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
          ></span>
          <span>Загрузка...</span>
        </div>
        <span v-else-if="authStore.error" class="text-amber-200"> ⚠️ Не удалось загрузить </span>
        <span v-else class="text-white">
          <div>{{ authStore.userName }}</div>
        </span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.store'
import { onMounted } from 'vue'

const authStore = useAuthStore()

onMounted(() => {
  authStore.init()
})
</script>
