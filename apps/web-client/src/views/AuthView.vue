<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-2xl w-100 mx-4">
      <header
        class="w-full h-14 rounded-t-lg flex justify-center items-center bg-linear-to-r from-indigo-600 to-cyan-600"
      >
        <h1 class="text-2xl text-white font-bold">Connect</h1>
      </header>

      <div class="flex border-b border-gray-200 dark:border-gray-700">
        <button
          @click="setActiveTab('login')"
          :class="[
            'flex-1 py-3 text-center font-medium transition-colors duration-200',
            activeTab === 'login'
              ? 'text-blue-600 border-b-2 border-blue-600 dark:text-blue-400'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          Вход
        </button>
        <button
          @click="setActiveTab('register')"
          :class="[
            'flex-1 py-3 text-center font-medium transition-colors duration-200',
            activeTab === 'register'
              ? 'text-blue-600 border-b-2 border-blue-600 dark:text-blue-400'
              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
          ]"
        >
          Регистрация
        </button>
      </div>

      <div class="p-6">
        <Transition name="fade-slide" mode="out-in">
          <LoginForm v-if="activeTab === 'login'" key="login" />
          <RegisterForm v-else key="register" />
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref<'login' | 'register'>('login')

// Синхронизация вкладки с query-параметром ?open=register
const syncTabFromQuery = () => {
  const openParam = route.query.open
  if (openParam === 'register') {
    activeTab.value = 'register'
  } else {
    activeTab.value = 'login'
  }
}

// При монтировании
onMounted(syncTabFromQuery)

// При изменении query-параметра
watch(
  () => route.query.open,
  () => {
    syncTabFromQuery()
  },
)

// Установка вкладки с обновлением URL
const setActiveTab = (tab: 'login' | 'register') => {
  activeTab.value = tab
  // Если вкладка "register" – добавляем параметр, иначе убираем
  const newQuery = { ...route.query }
  if (tab === 'register') {
    newQuery.open = 'register'
  } else {
    delete newQuery.open
  }
  router.push({ query: newQuery })
}
</script>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
