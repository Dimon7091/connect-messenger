<template>
  <div
    class="bg-linear-to-r from-indigo-600 to-cyan-600 dark:from-gray-800 dark:to-gray-900 dark:bg-linear-to-r py-2.5 px-3"
  >
    <div class="flex items-center justify-between gap-">
      <!-- Чаты с бейджем -->
      <button
        @click="goToChats"
        class="flex-1 flex flex-col items-center gap-1.5 text-white font-semibold rounded-xl py-2 px-2 transition-all duration-200 relative group cursor-pointer"
        :class="[
          isActive('/chats') ? 'bg-white/25 backdrop-blur-sm shadow-lg' : 'hover:bg-white/10',
        ]"
      >
        <div class="relative">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
            />
          </svg>
          <span
            v-if="chatStore.unreadChatsCount > 0"
            class="absolute -top-2 -right-2.5 bg-red-500 text-white text-sm font-bold rounded-full min-w-5 h-5 flex items-center justify-center"
          >
            {{ chatStore.unreadChatsCount }}
          </span>
        </div>
        <span class="text-sm">Чаты</span>
      </button>

      <button
        @click="goToSettings"
        class="flex-1 flex flex-col items-center gap-1.5 text-white font-semibold rounded-xl py-2 px-2 transition-all duration-200 group cursor-pointer"
        :class="[
          isActive('/settings') ? 'bg-white/20 backdrop-blur-sm shadow-lg' : 'hover:bg-white/10',
        ]"
      >
        <svg
          class="w-6 h-6 transition-transform duration-200 group-hover:rotate-90"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
          />
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
        <span class="text-sm">Настройки</span>
      </button>

      <button
        @click="goToProfile"
        class="flex-1 flex flex-col items-center gap-1.5 text-white font-semibold rounded-xl py-2 px-2 transition-all duration-200 group cursor-pointer"
        :class="[
          isActive('/profile') ? 'bg-white/20 backdrop-blur-sm shadow-lg' : 'hover:bg-white/10',
        ]"
      >
        <svg
          class="w-6 h-6 transition-transform duration-200 group-hover:scale-110"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
          />
        </svg>
        <span class="text-sm">Профиль</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()

const goToChats = () => router.push('/chats')
const goToSettings = () => router.push('/settings')
const goToProfile = () => router.push('/profile')

const isActive = (path: string) => route.path === path
</script>
