<template>
  <!-- Контейнер чата -->
  <div
    @click="handleClick"
    class="flex items-center gap-3 p-3 cursor-pointer shadow-xs hover:bg-gray-50 dark:hover:bg-gray-800 transition"
  >
    <!-- Аватар с индикатором статуса -->
    <div class="shrink-0 relative">
      <!-- Сам аватар -->
      <div
        :class="[
          'w-14 h-14 rounded-full backdrop-blur-sm flex items-center justify-center shadow-xl text-white font-bold text-xl uppercase',
          avatarGradientClass,
        ]"
      >
        <img
          v-if="avatarThumbUrl"
          :src="avatarThumbUrl"
          class="w-full h-full rounded-full object-cover"
          alt="avatar"
        />
        <span v-else-if="isDeleted">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none">
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

      <!-- Индикатор статуса (только для DIRECT чатов и когда онлайн) -->
      <div
        v-if="chat.type === 'DIRECT' && companion && isOnline"
        class="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full border-2 border-white dark:border-gray-900 bg-green-500"
      ></div>
    </div>

    <!-- Блок сообщения / статуса печатания -->
    <div class="flex-1 min-w-0 border-b-gray-200 dark:border-b-gray-700 pb-3">
      <div>
        <h3 class="font-medium text-black dark:text-white text-lg truncate">
          {{ displayName }}
        </h3>
      </div>
      <div>
        <div v-if="!isCompanionInBlacklist">
          <div v-if="isCompanionTyping" class="typing-status">
            печатает<span class="typing-dots"></span>
          </div>
          <p v-else class="text-md text-gray-600 dark:text-gray-400 truncate">
            {{ lastMessageText || 'Нет сообщений' }}
          </p>
        </div>
        <div v-else class="flex flex-row">
          <p class="dark:text-gray-400 pr-2 text-red-600">Заблокирован</p>
          <svg viewBox="0 0 24 24" width="22" height="22" fill="#ff5959">
            <path
              d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"
            />
          </svg>
        </div>
      </div>
    </div>

    <!-- Блок с временем, непрочитанными и тремя точками -->
    <div class="flex items-center gap-2 shrink-0">
      <div class="flex flex-col items-end gap-1 min-h-14">
        <span class="text-sm font-medium text-gray-500 dark:text-gray-400 whitespace-nowrap">
          {{ formattedTime }}
        </span>
        <div
          v-if="unreadCount > 0"
          class="bg-blue-500 text-white text-sm font-bold rounded-full min-w-6 h-6 flex items-center justify-center"
        >
          <span class="leading-none">{{ unreadCount }}</span>
        </div>
      </div>

      <!-- Кнопка меню -->
      <button
        @click.stop="handleMenuClick"
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
  </div>
</template>

<script setup lang="ts">
import { watch, computed } from 'vue'
import type { Chat } from '@/types/index'
import type { User } from '@messenger/shared'
import { useUserStore } from '@/stores/userStore'
import { useWebSocketStore } from '@/stores/webSocketStore'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'

const userStore = useUserStore()
const wsStore = useWebSocketStore()

const props = defineProps<{
  chat: Chat
  companion?: User | null
  lastMessage?: string | null
  unreadCount?: number
  lastMessageTime?: Date | string
}>()

// --- Аватар и градиент ---
const avatarThumbUrl = computed(() => props.companion?.profile?.avatarThumbUrl || null)
const userId = computed(() => props.companion?.id)

const avatarGradientClass = computed(() => {
  if (avatarThumbUrl.value) return ''
  if (props.chat.type === 'GROUP') {
    // Для групповых чатов используем простой градиент на основе id чата
    const gradients = [
      'bg-gradient-to-br from-red-500 to-red-700',
      'bg-gradient-to-br from-blue-500 to-blue-700',
      'bg-gradient-to-br from-green-500 to-green-700',
      'bg-gradient-to-br from-yellow-500 to-yellow-700',
      'bg-gradient-to-br from-purple-500 to-purple-700',
      'bg-gradient-to-br from-pink-500 to-pink-700',
      'bg-gradient-to-br from-indigo-500 to-indigo-700',
      'bg-gradient-to-br from-teal-500 to-teal-700',
      'bg-gradient-to-br from-orange-500 to-orange-700',
      'bg-gradient-to-br from-cyan-500 to-cyan-700',
      'bg-gradient-to-br from-rose-500 to-rose-700',
      'bg-gradient-to-br from-violet-500 to-violet-700',
      'bg-gradient-to-br from-fuchsia-500 to-fuchsia-700',
      'bg-gradient-to-br from-emerald-500 to-emerald-700',
      'bg-gradient-to-br from-sky-500 to-sky-700',
    ]
    const id = typeof props.chat.id === 'string' ? parseInt(props.chat.id) || 0 : props.chat.id
    return gradients[id % gradients.length]
  }
  // Для личных чатов – стабильный градиент на основе userId
  if (userId.value) {
    return getAvatarGradient(userId.value)
  }
  return 'bg-gradient-to-br from-gray-500 to-gray-700'
})

// --- Отображаемое имя ---
const displayName = computed(() => {
  if (props.chat.type === 'GROUP') {
    return props.chat.name || 'Групповой чат'
  }
  if (props.companion) {
    const firstName = props.companion.profile?.firstName
    const lastName = props.companion.profile?.lastName
    if (firstName && lastName) return `${firstName} ${lastName}`
    if (firstName) return firstName
    return props.companion.userName
  }
  return 'Пользователь'
})

// --- Инициалы для аватара ---
const avatarInitials = computed(() => {
  if (props.chat.type === 'GROUP') {
    const name = props.chat.name || 'Групповой чат'
    return getInitialsFromName(name) // можно оставить только первую букву для группы, но утилита сама решит
  }
  if (props.companion) {
    const fullName = displayName.value // displayName уже содержит имя и фамилию или userName
    return getInitialsFromName(fullName)
  }
  return '?'
})

// --- Проверка на удаленного пользователя ---
const isDeleted = computed(() => props.companion?.isDeleted ?? false)

// --- Последнее сообщение, счётчик и время ---
const lastMessageText = computed(() => {
  if (props.lastMessage) return props.lastMessage
  if (props.chat.lastMessage) return props.chat.lastMessage
  return null
})

const isCompanionInBlacklist = computed(() => {
  const blackList = userStore.user?.blackListIds || []
  return blackList.includes(props.companion?.id as string)
})

const unreadCount = computed(() => {
  if (props.unreadCount !== undefined) return props.unreadCount
  return props.chat.unreadCount || 0
})

const formattedTime = computed(() => {
  const time = props.lastMessageTime
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((today.getTime() - messageDate.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) {
    return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  } else if (diffDays === 1) {
    return 'Вчера'
  } else {
    if (date.getFullYear() === now.getFullYear()) {
      return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
    } else {
      return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' })
    }
  }
})

// --- Статусы ---
const isOnline = computed(() => {
  if (props.chat.type !== 'DIRECT' || !props.companion) return false
  const status = userStore.getUserStatus(props.companion.id.toString())
  return status === 'ONLINE'
})

const isCompanionTyping = computed(() => {
  if (props.chat.type !== 'DIRECT' || !props.companion) return false
  return wsStore.isUserTyping(props.chat.id, props.companion.id.toString())
})

// --- Эмиты ---
const emit = defineEmits<{
  click: [chat: Chat]
  menu: [chat: Chat]
}>()

const handleMenuClick = () => emit('menu', props.chat)
const handleClick = () => emit('click', props.chat)

// --- Отладка ---
watch(
  () => props.companion,
  (newVal) => {
    console.log(
      'ChatPreview companion:',
      newVal?.profile?.firstName || newVal?.userName,
      'chat:',
      props.chat.id,
    )
  },
  { immediate: true },
)

watch(
  () => props.chat,
  (newVal) => {
    console.log('ChatPreview chat:', newVal.id, newVal.type, newVal.participants)
  },
  { immediate: true },
)
</script>

<style scoped>
.truncate {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.typing-dots::after {
  content: '';
  animation: typing 1.5s infinite;
  display: inline-block;
  width: 1.5em;
  text-align: left;
}
@keyframes typing {
  0% {
    content: '';
  }
  25% {
    content: '.';
  }
  50% {
    content: '..';
  }
  75% {
    content: '...';
  }
  100% {
    content: '';
  }
}
</style>
