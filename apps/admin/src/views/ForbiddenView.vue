<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <div class="text-center">
      <div class="text-9xl font-bold text-red-500">403</div>
      <h1 class="text-3xl font-bold text-gray-800 mt-4">Доступ запрещен</h1>
      <p class="text-gray-600 mt-2">У вас недостаточно прав для просмотра этой страницы</p>
      <button
        @click="logout"
        class="mt-6 bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
      >
        Выйти из системы
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>
