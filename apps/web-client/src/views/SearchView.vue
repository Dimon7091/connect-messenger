<template>
  <div class="bg-white dark:bg-gray-900 h-[100dvh]">
    <header
      class="flex items-center py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 pl-4"
    >
      <button
        @click="goBack"
        class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition border border-gray-300 dark:border-gray-500"
      >
        <svg
          class="w-8 h-8 text-gray-700 dark:text-gray-300"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M10 19l-7-7m0 0l7-7m-7 7h18"
          />
        </svg>
      </button>

      <h1 class="ml-3 text-lg font-semibold text-gray-900 dark:text-white">Новое сообщение</h1>
    </header>

    <div class="dark:bg-gray-700 h-screen pt-3">
      <div class="px-3">
        <input
          v-model="searchQuery"
          @input="handleSearch"
          placeholder="Поиск или новый чат"
          class="w-full mb-3 pl-6 pr-10 py-2.5 bg-gray-100 dark:bg-gray-800 shadow-sm rounded-3xl focus:outline-none focus:ring-2 focus:ring-[#50a2e9] dark:text-white placeholder-gray-500 transition border border-blue-300 dark:border-gray-600"
        />
      </div>

      <div v-if="loading" class="text-center py-8">
        <div
          class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-[#50a2e9]"
        ></div>
        <p class="text-gray-500 dark:text-gray-400 mt-2">Поиск...</p>
      </div>

      <div v-else-if="searchQuery">
        <div v-if="searchResults.length > 0">
          <UserPreview
            v-for="user in searchResults"
            :key="user.id"
            :user="user"
            @click="openChat"
          />
        </div>
        <div v-else class="text-center py-8 text-gray-500 dark:text-gray-400">
          Пользователь "{{ searchQuery }}" не найден
        </div>
      </div>

      <div v-else class="text-center py-8">
        <svg
          class="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-3"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <p class="text-gray-500 dark:text-gray-400">Начните вводить имя пользователя</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import UserPreview from '@/components/UserPreview.vue'
import { userService } from '@/services/http/UserService'
import type { User } from '@messenger/shared-schema'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const searchQuery = ref('')
const searchResults = ref<User[]>([])
const loading = ref(false)
const userStore = useUserStore()

let debounceTimer: ReturnType<typeof setTimeout>

const openChat = (user: User) => {
  userStore.updateUserInCache(user.id, user)
  router.push({
    path: `/chats/new`,
    query: {
      companionId: user.id,
    },
  })
}

watch(searchQuery, (newValue) => {
  if (!newValue.trim()) {
    searchResults.value = []
    loading.value = false
    clearTimeout(debounceTimer)
  }
})

const handleSearch = () => {
  clearTimeout(debounceTimer)

  if (!searchQuery.value.trim()) {
    searchResults.value = []
    loading.value = false
    return
  }

  loading.value = true

  debounceTimer = setTimeout(async () => {
    try {
      const results = await userService.searchUsers(searchQuery.value.trim())
      searchResults.value = results
    } catch (error) {
      console.error('Ошибка поиска:', error)
      searchResults.value = []
    } finally {
      loading.value = false
    }
  }, 500)
}

const goBack = () => {
  router.back()
}
</script>
