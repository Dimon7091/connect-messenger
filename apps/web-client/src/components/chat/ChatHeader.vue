<template>
  <div
    class="chat-header flex justify-between p-2 bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700"
  >
    <div class="flex items-center gap-2.5">
      <button
        @click="$emit('back')"
        class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition border border-gray-300 dark:border-gray-500"
      >
        <svg
          class="w-8 h-8 text-gray-700 dark:text-gray-200"
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
      <div class="flex items-center gap-3">
        <!-- Аватар -->
        <div
          class="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold shadow-sm"
          :class="avatarGradientClass"
          @click="$emit('click-avatar')"
        >
          <img
            v-if="avatarThumbUrl"
            :src="avatarThumbUrl"
            class="w-full h-full rounded-full object-cover"
            alt="avatar"
          />
          <span v-else-if="props.isDeleted">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
              <!-- Контур человека белого цвета -->
              <g
                stroke="#FFFFFF"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                opacity="0.6"
              >
                <circle cx="10" cy="7" r="4" />
                <path d="M3 19V16.5C3 15.12 4.12 14 5.5 14H13.5" />
              </g>
              <!-- Белый крестик действия -->
              <g stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round">
                <line x1="16" y1="14" x2="22" y2="20" />
                <line x1="22" y1="14" x2="16" y2="20" />
              </g>
            </svg>
          </span>
          <span v-else>{{ avatarInitials }}</span>
        </div>
        <div>
          <h2 class="font-semibold text-gray-900 dark:text-white">{{ displayName }}</h2>
          <div class="text-sm text-gray-500">
            <slot name="status">
              {{ statusText }}
            </slot>
          </div>
        </div>
      </div>
    </div>

    <!-- Кнопка меню -->
    <button
      @click="$emit('menu')"
      class="rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition"
      title="Настройки чата"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke-width="1.5"
        stroke="currentColor"
        class="w-8 h-8 text-gray-400 dark:text-gray-400"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M12 6.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5ZM12 12.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5ZM12 18.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5Z"
        />
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'

const props = defineProps<{
  displayName: string
  firstName?: string
  lastName?: string
  avatarThumbUrl?: string | null
  userId?: string | number
  statusText?: string
  isDeleted: boolean
}>()

defineEmits<{
  (e: 'back'): void
  (e: 'click-avatar'): void
  (e: 'menu'): void
}>()

// Градиент для аватара (если нет avatarUrl)
const avatarGradientClass = computed(() => {
  if (props.avatarThumbUrl) return ''
  if (props.userId) return getAvatarGradient(props.userId)
  // fallback, если userId не передан (например, для групповых чатов)
  return 'bg-gradient-to-br from-orange-500 to-orange-700'
})

// Инициалы: используем firstName/lastName, иначе из displayName через утилиту
const avatarInitials = computed(() => {
  if (props.firstName && props.lastName) {
    return (props.firstName.charAt(0) + props.lastName.charAt(0)).toUpperCase()
  }
  if (props.firstName) {
    return props.firstName.charAt(0).toUpperCase()
  }
  if (props.lastName) {
    return props.lastName.charAt(0).toUpperCase()
  }
  // Если нет firstName/lastName, получаем инициалы из displayName через утилиту
  return getInitialsFromName(props.displayName)
})
</script>
