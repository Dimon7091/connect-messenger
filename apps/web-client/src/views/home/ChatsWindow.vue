<template>
  <div class="relative">
    <!-- Скелетон загрузки -->
    <div v-if="chatStore.loading" class="divide-y divide-gray-100 dark:divide-gray-800">
      <div v-for="i in 5" :key="i" class="flex items-center gap-3 px-4 py-3 animate-pulse">
        <div class="w-12 h-12 bg-gray-200 dark:bg-gray-700 rounded-full"></div>
        <div class="flex-1">
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-32 mb-2"></div>
          <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-48"></div>
        </div>
      </div>
    </div>

    <!-- Пустое состояние -->
    <div
      v-else-if="chats.length === 0"
      class="flex flex-col items-center justify-center h-64 text-center px-4"
    >
      <div
        class="w-20 h-20 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center mb-4"
      >
        <svg
          class="w-10 h-10 text-gray-400 dark:text-gray-500"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"
          />
        </svg>
      </div>
      <p class="text-gray-500 dark:text-gray-400 font-medium text-xl">Нет чатов</p>
      <p class="text-gray-400 dark:text-gray-500 mt-1">Начните новый чат</p>
    </div>

    <!-- Список чатов -->
    <template v-else>
      <ChatPreview
        v-for="chat in chats"
        :key="chat.id"
        :chat="chat"
        :companion="getChatCompanion(chat)"
        :last-message="chat.lastMessage"
        :last-message-time="chat.updatedAt"
        :unread-count="Number(chat.unreadCount)"
        @click="openChat"
        @menu="openMenu"
      />
    </template>
    <AddChatBottom />
    <ChatContextMenu :chat="menuChat" :companion-id="companionIdForMenu" @close="closeMenu" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'
import { useUserStore } from '@/stores/userStore'
import { useWebSocketStore } from '@/stores/webSocketStore'
import type { Chat } from '@/types/index'
import type { User } from '@messenger/shared-schema'
import ChatPreview from '@/components/chat/ChatPreview.vue'
import ChatContextMenu from '@/components/chat/ChatContextMenu.vue'
import AddChatBottom from '@/components/AddChatButton.vue'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const wsStore = useWebSocketStore()

// Множество подписанных пользователей (чтобы не подписываться повторно)
const subscribedUsers = ref<Set<string>>(new Set())
// Множество подписанных чатов (для получения событий печатания)
const subscribedChats = ref<Set<string>>(new Set())

// Меню
const menuChat = ref<Chat | null>(null)

// Получаем список чатов
const chats = computed(() => chatStore.sortedChats)

// Получить компаньона из кэша userStore
const getChatCompanion = (chat: Chat): User | null => {
  if (chat.type === 'GROUP') return null
  const currentUserId = userStore.user?.id
  if (!currentUserId) return null
  const companionId = chat.participants.find((id) => id.toString() !== currentUserId.toString())
  if (!companionId) return null
  return userStore.getUserFromCache(companionId.toString())
}

// ID компаньона для выбранного чата (для передачи в меню)
const companionIdForMenu = computed(() => {
  if (!menuChat.value) return ''
  const companion = getChatCompanion(menuChat.value)
  return companion?.id || ''
})

// Подписка на статусы пользователей
const subscribeToUserStatuses = (userIds: string[]) => {
  for (const userId of userIds) {
    if (!subscribedUsers.value.has(userId)) {
      wsStore.subscribeToUserStatus(userId)
      subscribedUsers.value.add(userId)
      console.log(`Подписались на статус пользователя ${userId}`)
    }
  }
}

// Отписка от статусов
const unsubscribeFromUserStatuses = (userIds: string[]) => {
  for (const userId of userIds) {
    if (subscribedUsers.value.has(userId)) {
      wsStore.unsubscribeFromUserStatus(userId)
      subscribedUsers.value.delete(userId)
      console.log(`Отписались от статуса пользователя ${userId}`)
    }
  }
}

// Подписка на чаты (для получения событий печатания)
const subscribeToChats = () => {
  for (const chat of chats.value) {
    if (!subscribedChats.value.has(chat.id)) {
      wsStore.subscribeToChat(chat.id)
      subscribedChats.value.add(chat.id)
      console.log(`Подписались на чат ${chat.id}`)
    }
  }
}

// Отписка от всех чатов
const unsubscribeFromChats = () => {
  for (const chatId of subscribedChats.value) {
    wsStore.unsubscribeFromChat(chatId)
  }
  subscribedChats.value.clear()
  console.log('Отписались от всех чатов')
}

// Загрузить компаньонов и подписаться на статусы / чаты
const loadCompanions = async () => {
  const currentUserId = userStore.user?.id
  if (!currentUserId) return

  const companionIds = new Set<string>()
  for (const chat of chats.value) {
    if (chat.type === 'DIRECT') {
      const companionId = chat.participants.find((id) => id.toString() !== currentUserId.toString())
      if (companionId) {
        companionIds.add(companionId.toString())
      }
    }
  }

  if (companionIds.size === 0) return

  // Загружаем данные компаньонов в кэш (если их там ещё нет)
  await Promise.all(Array.from(companionIds).map((id) => userStore.getUserById(id)))

  if (wsStore.isConnected) {
    subscribeToUserStatuses(Array.from(companionIds))
    subscribeToChats()
  } else {
    console.log('WebSocket не подключён, подписки будут установлены позже')
  }
}

// Обновить подписки при изменении списка чатов
const updateSubscriptions = () => {
  const currentUserId = userStore.user?.id
  if (!currentUserId) return

  const currentCompanionIds = new Set<string>()
  for (const chat of chats.value) {
    if (chat.type === 'DIRECT') {
      const companionId = chat.participants.find((id) => id.toString() !== currentUserId.toString())
      if (companionId) {
        currentCompanionIds.add(companionId.toString())
      }
    }
  }

  // Управление подписками на статусы
  const toUnsubscribe = Array.from(subscribedUsers.value).filter(
    (userId) => !currentCompanionIds.has(userId),
  )
  if (toUnsubscribe.length) {
    unsubscribeFromUserStatuses(toUnsubscribe)
  }

  const toSubscribe = Array.from(currentCompanionIds).filter(
    (userId) => !subscribedUsers.value.has(userId),
  )
  if (toSubscribe.length) {
    Promise.all(toSubscribe.map((id) => userStore.getUserById(id))).then(() => {
      if (wsStore.isConnected) {
        subscribeToUserStatuses(toSubscribe)
      }
    })
  }

  // Управление подписками на чаты (для печатания)
  const currentChatIds = new Set(chats.value.map((chat) => chat.id))
  const toUnsubChats = Array.from(subscribedChats.value).filter((id) => !currentChatIds.has(id))
  if (toUnsubChats.length) {
    for (const chatId of toUnsubChats) {
      wsStore.unsubscribeFromChat(chatId)
      subscribedChats.value.delete(chatId)
    }
  }
  const toSubChats = Array.from(currentChatIds).filter((id) => !subscribedChats.value.has(id))
  if (toSubChats.length && wsStore.isConnected) {
    for (const chatId of toSubChats) {
      wsStore.subscribeToChat(chatId)
      subscribedChats.value.add(chatId)
    }
  }
}

// Открыть чат
const openChat = (chat: Chat) => {
  if (chat.type === 'GROUP') {
    router.push({
      path: `/chats/${chat.id}`,
    })
    return
  }

  const currentUserId = userStore.user?.id
  if (!currentUserId) return

  const companionId = chat.participants.find((id) => id.toString() !== currentUserId.toString())
  if (!companionId) return

  const companion = userStore.getUserFromCache(companionId.toString())
  if (!companion) {
    console.error('Компаньон не найден для чата:', chat.id)
    return
  }

  router.push({
    path: `/chats/${chat.id}`,
  })
}

const openMenu = (chat: Chat) => {
  menuChat.value = chat
}
const closeMenu = () => {
  menuChat.value = null
}

// Следим за изменением чатов
watch(
  chats,
  () => {
    if (!chatStore.loading) {
      updateSubscriptions()
    }
  },
  { deep: true },
)

// Обработчик подключения WebSocket (для восстановления подписок)
let unsubscribeConnection: (() => void) | null = null

onMounted(async () => {
  console.log('ChatList mounted')
  const currentUserId = userStore.user?.id
  if (!currentUserId) {
    console.warn('Пользователь не авторизован')
    return
  }

  await chatStore.fetchChats()
  console.log('Чаты загружены, количество:', chats.value.length)

  if (!wsStore.isConnected) {
    console.log('Ожидание WebSocket соединения...')
    await wsStore.waitForConnection(5000)
  }

  if (wsStore.isConnected) {
    await loadCompanions()
  } else {
    console.warn('WebSocket не подключился, статусы не будут отображаться')
  }

  unsubscribeConnection = wsStore.onConnectionChange(async (connected) => {
    if (connected && subscribedUsers.value.size === 0 && chats.value.length) {
      console.log('WebSocket переподключён, подписываемся на статусы и чаты')
      await loadCompanions()
    } else if (connected && subscribedUsers.value.size > 0) {
      // Восстанавливаем подписки на чаты при переподключении, если они были
      for (const chatId of subscribedChats.value) {
        wsStore.subscribeToChat(chatId)
      }
    }
  })
})

onUnmounted(() => {
  if (unsubscribeConnection) unsubscribeConnection()
  unsubscribeFromUserStatuses(Array.from(subscribedUsers.value))
  unsubscribeFromChats()
})
</script>
