<template>
  <form @submit.prevent="handleRegister" class="space-y-4" novalidate>
    <!-- Имя пользователя -->
    <div class="space-y-1">
      <label for="reg-username" class="block text-lg font-medium text-gray-700 dark:text-gray-300">
        Имя пользователя *
      </label>
      <div class="relative">
        <input
          id="reg-username"
          v-model="form.userName"
          type="text"
          placeholder="ivan123"
          class="peer w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
          :class="[
            errors.username
              ? 'border-red-500 focus:ring-red-500'
              : 'border-gray-300 dark:border-gray-600 focus:border-blue-500',
          ]"
        />
        <div
          class="absolute left-0 top-full mt-1 px-2 py-1 text-xs text-white bg-gray-800 dark:bg-gray-900 rounded shadow-lg opacity-0 peer-focus:opacity-100 transition-opacity pointer-events-none z-10"
        >
          Только латинские буквы (a-z), цифры (0-9), точки, дефисы, подчёркивания
        </div>
      </div>
      <span v-if="errors.username" class="text-sm text-red-500 block">{{ errors.username }}</span>
    </div>

    <!-- Пароль -->
    <div class="space-y-1">
      <label for="reg-password" class="block text-lg font-medium text-gray-700 dark:text-gray-300">
        Пароль *
      </label>
      <div class="relative">
        <input
          id="reg-password"
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
        <div
          class="absolute left-0 top-full mt-1 px-2 py-1 text-xs text-white bg-gray-800 dark:bg-gray-900 rounded shadow-lg opacity-0 peer-focus:opacity-100 transition-opacity pointer-events-none z-10"
        >
          Минимум 8 символов
        </div>
      </div>
      <span v-if="errors.password" class="text-sm text-red-500 block">{{ errors.password }}</span>
    </div>

    <!-- Подтверждение пароля -->
    <div class="space-y-1">
      <label
        for="confirm-password"
        class="block text-lg font-medium text-gray-700 dark:text-gray-300"
      >
        Подтверждение пароля *
      </label>
      <div class="relative">
        <input
          id="confirm-password"
          v-model="form.confirmPassword"
          type="password"
          placeholder="******"
          class="peer w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
          :class="[
            errors.confirmPassword
              ? 'border-red-500 focus:ring-red-500'
              : 'border-gray-300 dark:border-gray-600 focus:border-blue-500',
          ]"
        />
        <div
          class="absolute left-0 top-full mt-1 px-2 py-1 text-xs text-white bg-gray-800 dark:bg-gray-900 rounded shadow-lg opacity-0 peer-focus:opacity-100 transition-opacity pointer-events-none z-10"
        >
          Должен совпадать с паролем
        </div>
      </div>
      <span v-if="errors.confirmPassword" class="text-sm text-red-500 block">{{
        errors.confirmPassword
      }}</span>
    </div>

    <!-- Имя (обязательное) -->
    <div class="space-y-1">
      <label for="firstName" class="block text-lg font-medium text-gray-700 dark:text-gray-300">
        Имя *
      </label>
      <input
        id="firstName"
        v-model="form.firstName"
        type="text"
        placeholder="Иван"
        class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
      />
    </div>

    <!-- Фамилия (опционально) -->
    <div class="space-y-1">
      <label for="lastName" class="block text-lg font-medium text-gray-700 dark:text-gray-300">
        Фамилия (опционально)
      </label>
      <input
        id="lastName"
        v-model="form.lastName"
        type="text"
        placeholder="Иванов"
        class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors dark:bg-gray-700 dark:text-white"
      />
    </div>

    <!-- Поле токена полностью скрыто. Никаких визуальных элементов, связанных с токеном, нет. -->

    <button
      type="submit"
      :disabled="isLoading"
      class="w-full bg-blue-600 text-lg text-white py-2 rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed font-medium"
    >
      {{ isLoading ? 'Регистрация...' : 'Зарегистрироваться' }}
    </button>

    <div
      v-if="submitErrors.length > 0"
      class="flex bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 p-3 rounded-md text-sm border border-red-200 dark:border-red-800"
    >
      <ul class="list-disc list-outside pl-5 text-left w-full space-y-1">
        <li v-for="(error, index) in submitErrors" :key="index" class="error-item">
          {{ error }}
        </li>
      </ul>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/userStore'
import type { RegisterRequest } from '@/types/index'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = reactive<RegisterRequest & { confirmPassword: string }>({
  userName: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  invitationToken: '',
})

// Получаем токен из URL (query-параметр ?invitationToken=...)
const invitationTokenFromUrl = computed(() => {
  const token = route.query.invitationToken
  return typeof token === 'string' ? token : null
})

// При монтировании заполняем поле токена, если он есть в URL
onMounted(() => {
  if (invitationTokenFromUrl.value) {
    form.invitationToken = invitationTokenFromUrl.value
  }
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})
const isLoading = ref(false)
const submitErrors = ref<string[]>([])

const validate = () => {
  let isValid = true
  errors.username = ''
  errors.password = ''
  errors.confirmPassword = ''

  if (!form.userName) {
    errors.username = 'Имя пользователя обязательно'
    isValid = false
  } else if (form.userName.length < 3) {
    errors.username = 'Имя пользователя должно быть не менее 3 символов'
    isValid = false
  }

  if (!form.password) {
    errors.password = 'Пароль обязателен'
    isValid = false
  } else if (form.password.length < 8) {
    errors.password = 'Пароль должен быть не менее 8 символов'
    isValid = false
  }

  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Пароли не совпадают'
    isValid = false
  }

  return isValid
}

const handleRegister = async () => {
  if (!validate()) return

  isLoading.value = true
  submitErrors.value = []
  try {
    await userStore.register({
      userName: form.userName,
      password: form.password,
      firstName: form.firstName,
      lastName: form.lastName,
      invitationToken: form.invitationToken,
    })

    router.push({ name: 'home' })
  } catch (error: any) {
    const serverErrors = error?.data?.errors

    if (serverErrors && typeof serverErrors === 'object') {
      const errorMessages: string[] = []
      if (serverErrors.userName) errorMessages.push(serverErrors.userName)
      if (serverErrors.password) errorMessages.push(serverErrors.password)
      if (serverErrors.firstName) errorMessages.push(serverErrors.firstName)
      if (serverErrors.lastName) errorMessages.push(serverErrors.lastName)
      if (serverErrors.invitationToken) errorMessages.push(serverErrors.invitationToken)
      if (errorMessages.length > 0) {
        submitErrors.value = errorMessages
      } else {
        submitErrors.value = ['Ошибка регистрации']
      }
    } else {
      submitErrors.value = [error?.data?.error || error?.data?.message || 'Ошибка регистрации']
    }
  } finally {
    isLoading.value = false
  }
}
</script>
