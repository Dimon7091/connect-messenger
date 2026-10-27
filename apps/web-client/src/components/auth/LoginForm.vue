<template>
  <form @submit.prevent="handleLogin" class="space-y-5" novalidate>
    <!-- Поле "Имя пользователя" -->
    <div class="space-y-1">
      <label for="username" class="block text-ls font-medium text-gray-700 dark:text-gray-300">
        Имя пользователя
      </label>
      <div class="relative">
        <input
          id="username"
          v-model="form.username"
          type="text"
          placeholder="ivan123"
          class="peer w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
          :class="[
            errors.username
              ? 'border-red-500 focus:ring-red-500'
              : 'border-gray-300 dark:border-gray-600 focus:border-blue-500',
          ]"
        />
      </div>
      <span v-if="errors.username" class="text-sm text-red-500 block">{{ errors.username }}</span>
    </div>

    <!-- Поле "Пароль" -->
    <div class="space-y-1">
      <label for="password" class="block text-ls font-medium text-gray-700 dark:text-gray-300">
        Пароль
      </label>
      <div class="relative">
        <input
          id="password"
          v-model="form.password"
          type="password"
          placeholder="******"
          class="peer w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
          :class="[
            errors.password
              ? 'border-red-500 focus:ring-red-500'
              : 'border-gray-300 dark:border-gray-600 focus:border-blue-500',
          ]"
        />
      </div>
      <span v-if="errors.password" class="text-sm text-red-500 block">{{ errors.password }}</span>
    </div>

    <button
      type="submit"
      :disabled="isLoading"
      class="w-full bg-blue-600 text-lg text-white py-2 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed font-medium"
    >
      {{ isLoading ? 'Вход...' : 'Войти' }}
    </button>

    <!-- Плавное появление ошибки (только Tailwind) -->
    <Transition
      enter-active-class="transition-all duration-400 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-300 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
      mode="out-in"
    >
      <div
        v-if="submitError"
        class="bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 p-3 rounded-md text-sm text-center border border-red-200 dark:border-red-800"
      >
        {{ submitError }}
      </div>
    </Transition>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const isLoading = ref(false)
const submitError = ref('')

onMounted(() => {
  const errorParam = route.query.error
  if (errorParam && typeof errorParam === 'string') {
    submitError.value = decodeURIComponent(errorParam)
    router.replace({ query: {} })
  }
})

const validate = () => {
  let isValid = true
  errors.username = ''
  errors.password = ''

  if (!form.username) {
    errors.username = 'Имя пользователя обязательно'
    isValid = false
  } else if (form.username.length < 3) {
    errors.username = 'Имя пользователя должно быть не менее 3 символов'
    isValid = false
  }

  if (!form.password) {
    errors.password = 'Пароль обязателен'
    isValid = false
  } else if (form.password.length < 4) {
    errors.password = 'Пароль должен быть не менее 4 символов'
    isValid = false
  }

  return isValid
}

const handleLogin = async () => {
  submitError.value = ''
  if (!validate()) return

  isLoading.value = true
  try {
    await userStore.login(form.username, form.password)
    router.push('/')
  } catch (error: any) {
    submitError.value = error.message || 'Ошибка входа'
  } finally {
    isLoading.value = false
  }
}
</script>
