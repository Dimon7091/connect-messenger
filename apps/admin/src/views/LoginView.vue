<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100">
    <!-- Окно входа -->
    <div class="bg-white rounded-lg shadow-xl w-full max-w-md">
      <header
        class="w-full h-14 rounded-t-lg flex justify-center items-center bg-linear-to-r from-indigo-600 to-cyan-600"
      >
        <h1 class="text-3xl text-white font-bold">Connect</h1>
      </header>

      <!-- Контейнер формы -->
      <div class="p-8">
        <h1 class="text-2xl font-bold text-center text-gray-800 mb-6">Вход в систему</h1>
        <!-- Форма логина -->
        <form @submit.prevent="handleSubmit" class="space-y-4" novalidate>
          <!-- Поле username -->
          <div class="space-y-1">
            <label for="username" class="block text-sm font-medium text-gray-700">
              Имя пользователя
            </label>
            <input
              id="username"
              v-model="form.username"
              type="text"
              placeholder="ivan123"
              :class="[
                'w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors',
                errors.username
                  ? 'border-red-500 focus:ring-red-500'
                  : 'border-gray-300 focus:border-blue-500',
              ]"
            />
            <span v-if="errors.username" class="text-sm text-red-500 block">
              {{ errors.username }}
            </span>
          </div>

          <!-- Поле пароль -->
          <div class="space-y-1">
            <label for="password" class="block text-sm font-medium text-gray-700"> Пароль </label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="******"
              :class="[
                'w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors',
                errors.password
                  ? 'border-red-500 focus:ring-red-500'
                  : 'border-gray-300 focus:border-blue-500',
              ]"
            />
            <span v-if="errors.password" class="text-sm text-red-500 block">
              {{ errors.password }}
            </span>
          </div>

          <!-- Кнопка отправки -->
          <button
            type="submit"
            :disabled="isLoading"
            class="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed font-medium"
          >
            {{ isLoading ? 'Вход...' : 'Войти' }}
          </button>

          <!-- Сообщение об ошибке -->
          <div
            v-if="submitError"
            class="bg-red-50 text-red-600 p-3 rounded-md text-sm text-center border border-red-200"
          >
            {{ submitError }}
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.store'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()
const isLoading = ref(false)
const submitError = ref('')
const form = ref({ username: '', password: '' })

// Валидация
const errors = ref({
  username: '',
  password: '',
})

const validateForm = () => {
  let isValid = true
  errors.value = { username: '', password: '' }

  if (!form.value.username) {
    errors.value.username = 'Имя пользователя обязательно'
    isValid = false
  } else if (form.value.username.length < 3) {
    errors.value.username = 'Имя пользователя должно быть не менее 3 символов'
    isValid = false
  }

  if (!form.value.password) {
    errors.value.password = 'Пароль обязателен'
    isValid = false
  } else if (form.value.password.length < 4) {
    errors.value.password = 'Пароль должен быть не менее 4 символов'
    isValid = false
  }

  return isValid
}

const handleSubmit = async () => {
  submitError.value = ''

  if (!validateForm()) {
    return
  }

  isLoading.value = true

  try {
    await authStore.login({
      userName: form.value.username,
      password: form.value.password,
    })

    await router.push('/')
  } catch (error) {
    if (error instanceof Error) {
      submitError.value = error.message
    } else {
      submitError.value = 'Ошибка подключения к серверу'
    }
    console.error('Ошибка авторизации:', error)
  } finally {
    isLoading.value = false
  }
}
</script>
