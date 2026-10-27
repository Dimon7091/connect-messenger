<template>
  <div class="chat-view h-[100dvh] flex flex-col overflow-hidden">
    <!-- Панель действий (выделение, ответить, удалить) -->
    <SelectionActionBar
      :selectedCount="selectedCount"
      :selectionMode="selectionMode"
      @reset="resetSelection"
      @reply="replyToSelectedMessage"
      @delete="deleteSelectedMessages"
    />

    <ChatHeader
      :displayName="displayName || ''"
      @back="goBack"
      :firstName="companionUser?.profile.firstName"
      :lastName="companionUser?.profile.lastName || ''"
      :avatarThumbUrl="companionUser?.profile.avatarThumbUrl"
      :user-id="companionUser?.id"
      :isDeleted="isCompanionDeleted"
      @click-avatar="openUserProfile"
      @menu="openMenu"
    >
      <template #status>
        <div v-if="isCompanionTyping" class="typing-status">
          печатает<span class="typing-dots"></span>
        </div>
        <span v-else>{{ statusDisplayText }}</span>
      </template>
    </ChatHeader>

    <div v-if="isLoadingHistory" class="flex justify-center py-4">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
    </div>

    <ChatMessageList
      ref="messageListRef"
      :messages="messages"
      :currentUserId="currentUserId"
      :isLoadingHistory="isLoadingHistory"
      :isLoadingMore="isLoadingMore"
      @loadMore="loadMore"
      @scroll="checkScrollPosition"
      @selectionChange="onSelectionChange"
      @deleteMessages="handleDeleteMessages"
    />

    <!-- Блок-заглушка, если ввод запрещён -->
    <div
      v-if="isInputDisabled"
      class="p-4 bg-gray-100 dark:bg-gray-900 text-center transition-all duration-200"
    >
      <div class="flex items-center justify-center gap-1 text-red-600 dark:text-red-300">
        <span v-if="!isUserInBlacklist">
          <svg
            class="w-10 h-10 flex-shrink-0"
            fill="currentColor"
            viewBox="0 0 20 20"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              fill-rule="evenodd"
              d="M8.257 3.099c.765-1.316 2.722-1.316 3.487 0l5.58 9.632c.75 1.293-.192 2.769-1.744 2.769H4.42c-1.552 0-2.494-1.476-1.744-2.769l5.58-9.632zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-6a1 1 0 00-1 1v2a1 1 0 002 0V8a1 1 0 00-1-1z"
              clip-rule="evenodd"
            />
          </svg>
        </span>
        <div class="w-full flex flex-col gap-2">
          <span class="font-medium">
            <span v-if="isCurrentUserDeleted">
              Ваш аккаунт удалён. Отправка сообщений недоступна.
            </span>
            <span v-else-if="isCompanionDeleted">
              Пользователь удалён. Отправка сообщений недоступна.
            </span>
            <span v-else-if="isUserInBlacklist"> Вы заблокировали этого пользователя. </span>
          </span>

          <button
            v-if="isUserInBlacklist && !isCurrentUserDeleted && !isCompanionDeleted"
            @click="openConfirmBlockUser"
            class="w-full py-2 bg-green-500 hover:bg-green-600 text-white rounded-full text-sm transition dark:bg-green-600"
            :disabled="unblocking"
          >
            <span v-if="unblocking">Разблокировка...</span>
            <span v-else>Разблокировать</span>
          </button>
        </div>
      </div>
    </div>

    <ChatInput
      v-else
      ref="chatInputRef"
      :disabled="sending"
      @send="sendMessage"
      @typing="onTyping"
      placeholder="Введите сообщение"
    />

    <!-- Плавающая кнопка (скролл вниз) -->
    <Teleport to="body">
      <div class="fixed bottom-0 left-0 right-0 pointer-events-none flex justify-center">
        <div class="w-full max-w-5xl mx-auto pointer-events-auto relative h-0">
          <Transition
            enter-active-class="transition duration-500 transform"
            enter-from-class="opacity-0 translate-y-10"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition duration-300 transform"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 translate-y-10"
          >
            <button
              v-if="showFloatingButton"
              @click="onFloatingButtonClick"
              class="absolute bottom-24 right-4 md:right-8 z-50 bg-white text-gray-500 rounded-full shadow-lg flex items-center justify-center w-12 h-12 transition-all dark:bg-gray-700"
            >
              <svg
                class="w-6 h-6 text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 14l-7 7m0 0l-7-7m7 7V3"
                />
              </svg>
              <span
                v-if="unreadMessagesCount > 0"
                class="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full min-w-6 h-6 flex items-center justify-center px-1.5"
              >
                {{ unreadMessagesCount > 99 ? '99+' : unreadMessagesCount }}
              </span>
            </button>
          </Transition>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      v-model="showConfirmBlockUser"
      title="Разблокировать пользователя?"
      description="Вы уверены, что хотите разблокировать этого пользователя? Он сможет снова отправлять вам сообщения."
      confirm-text="Разблокировать"
      confirm-button-class="bg-red-600 hover:bg-red-700"
      @confirm="unblockUser"
      :loading="isBlocking"
      :loading-text="'Разблокировка...'"
    />

    <UserProfileModal
      :visible="showProfileModal"
      :user-id="companionId"
      @close="showProfileModal = false"
    />

    <ChatContextMenu :chat="menuChat" :companion-id="companionId" @close="closeMenu" />
  </div>
</template>

<style scoped></style>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'
import { useUserStore } from '@/stores/userStore'
import { useWebSocketStore } from '@/stores/webSocketStore'
import { useChatMessages } from '@/composables/useChatMessages'
import SelectionActionBar from '@/components/chat/SelectionActionBar.vue'
import ChatHeader from '@/components/chat/ChatHeader.vue'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { chatService } from '@/services/http/ChatService'
import type { Chat, Message, Attachment } from '@/types/index'
import type { User } from '@messenger/shared-schema'
import { useMessageStore } from '@/stores/messageStore'
import UserProfileModal from '@/components/modals/UserProfileModal.vue'
import ChatContextMenu from '@/components/chat/ChatContextMenu.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

// ==================== Роутер и сторы ====================
const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()
const wsStore = useWebSocketStore()
const messageStore = useMessageStore()

// ==================== Базовые состояния ====================
const currentChat = ref<Chat | null>(null)
const sending = ref(false)
const selectionMode = ref(false)
const selectedCount = ref(0)
const messageListRef = ref<InstanceType<typeof ChatMessageList>>()
const chatInputRef = ref<InstanceType<typeof ChatInput>>()

// Текущий пользователь (id в виде строки)
const currentUserId = computed(() => userStore.user?.id?.toString() || '')

// ==================== companionId ====================
// Заполняется в loadChatFromUrl. Реактивно догружает companionUser через watch ниже.
const companionId = ref<string>('')

const resolveCompanionId = (): string => {
  // 1. Query
  const fromQuery = route.query.companionId
  if (typeof fromQuery === 'string' && fromQuery) return fromQuery

  const chat = currentChat.value as any
  const me = currentUserId.value
  if (!chat || !me) return ''

  // 2. Явное поле
  if (chat.companionId != null && chat.companionId !== '') {
    return String(chat.companionId)
  }

  // 3. Participants (с поддержкой разных имён полей и форматов)
  const list: any[] | undefined =
    chat.participants ?? chat.users ?? chat.members ?? chat.participantIds ?? chat.userIds

  if (Array.isArray(list)) {
    for (const p of list) {
      const pid =
        p == null ? null : typeof p === 'object' ? (p.id ?? p.userId ?? p.user?.id ?? null) : p
      if (pid == null) continue
      if (String(pid) !== String(me)) return String(pid)
    }
  }

  return ''
}

// ==================== Единый объект собеседника ====================
const companionUser = ref<User | null>(null)
const isLoadingCompanion = ref(false)

// Токен против гонок при быстром переключении чатов
let companionLoadToken = 0

const loadCompanionUser = async (id: string): Promise<void> => {
  const token = ++companionLoadToken

  if (!id) {
    companionUser.value = null
    return
  }

  // 1. Кэш
  const cached = userStore.getUserFromCache(id)
  if (cached) {
    companionUser.value = cached
    return
  }

  // 2. API
  isLoadingCompanion.value = true
  try {
    const user = await userStore.getUserById(id)
    if (token === companionLoadToken) companionUser.value = user
  } catch (e) {
    if (token === companionLoadToken) {
      console.error('[ChatView] Не удалось загрузить пользователя:', e)
      companionUser.value = null
    }
  } finally {
    if (token === companionLoadToken) isLoadingCompanion.value = false
  }
}

// Реактивная догрузка companionUser при смене companionId
watch(companionId, (id) => {
  loadCompanionUser(id)
})

// ==================== Статус, lastSeen и печатание собеседника ====================
const companionStatus = computed(() => {
  if (!companionId.value) return 'OFFLINE'
  return userStore.getUserStatus(companionId.value) || 'OFFLINE'
})

const companionLastSeen = computed(() => {
  if (!companionId.value) return null
  return userStore.getUserLastSeen(companionId.value)
})

const isCompanionTyping = computed(() => {
  if (!currentChat.value || !companionId.value) return false
  return wsStore.isUserTyping(currentChat.value.id, companionId.value)
})

const displayName = computed(() => {
  const u = companionUser.value
  if (!u) return 'Пользователь'
  const firstName = u.profile?.firstName
  const lastName = u.profile?.lastName
  if (firstName && lastName) return `${firstName} ${lastName}`
  if (firstName) return firstName
  return u.userName || 'Пользователь'
})

// ==================== Реактивное текущее время для автообновления lastSeen ====================
const currentTime = ref(Date.now())
let timeUpdateInterval: ReturnType<typeof setInterval> | null = null

const formatLastSeen = (lastSeen: string | number): string => {
  if (!lastSeen) return ''
  let timestamp: number
  if (typeof lastSeen === 'number') {
    timestamp = lastSeen * 1000
  } else {
    timestamp = Date.parse(lastSeen)
    if (isNaN(timestamp)) return ''
  }
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return ''
  const now = currentTime.value
  const diffMs = now - timestamp
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)
  if (diffMins < 1) return 'был(а) только что'
  if (diffMins < 60) return `был(а) ${diffMins} мин. назад`
  if (diffHours < 24) return `был(а) ${diffHours} ч. назад`
  if (diffDays === 1) return 'был(а) вчера'
  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
}

const formattedLastSeen = computed(() => {
  if (!companionLastSeen.value) return null
  return formatLastSeen(companionLastSeen.value)
})

const statusDisplayText = computed(() => {
  if (isCompanionTyping.value) return 'печатает'
  if (companionStatus.value === 'ONLINE') return 'онлайн'
  if (formattedLastSeen.value) return formattedLastSeen.value
  return 'не в сети'
})

// ==================== Состояния ====================
const isCurrentUserDeleted = computed(() => userStore.user?.isDeleted || false)

const isCompanionDeleted = computed(() => companionUser.value?.isDeleted || false)

const isUserInBlacklist = computed(() => {
  const blackList = userStore.user?.blackListIds || []
  return !!companionId.value && blackList.includes(companionId.value)
})

const isInputDisabled = computed(() => {
  if (isCurrentUserDeleted.value) return true
  if (isCompanionDeleted.value) return true
  if (isUserInBlacklist.value) return true
  return false
})

const unblocking = ref(false)
const showConfirmBlockUser = ref(false)
const showProfileModal = ref(false)

const openUserProfile = () => {
  if (!companionId.value) return
  showProfileModal.value = true
}

// ==================== Сообщения ====================
const chatIdGetter = () => currentChat.value?.id

const {
  messages,
  isLoadingHistory,
  isLoadingMore,
  scrollToBottom,
  loadMore,
  loadHistory,
  cleanup: cleanupObserver,
  checkScrollPosition,
  isUserAtBottom,
} = useChatMessages(
  chatIdGetter,
  computed(() => messageListRef.value?.container),
)

const unreadMessagesCount = computed(() => {
  return messages.value.filter(
    (msg) => msg.senderId !== currentUserId.value && msg.status !== 'READ',
  ).length
})

// ==================== Выделение сообщений ====================
const onSelectionChange = (selectedIds: Set<string>) => {
  selectionMode.value = selectedIds.size > 0
  selectedCount.value = selectedIds.size
}

const resetSelection = () => {
  messageListRef.value?.resetSelection()
}

const deleteSelectedMessages = () => {
  messageListRef.value?.deleteSelected()
}

const handleDeleteMessages = async (ids: string[]) => {
  if (!currentChat.value || ids.length === 0) return
  try {
    const success = await messageStore.deleteMessagesOnServer?.(currentChat.value.id, ids)
    if (success) {
      messageStore.deleteMessagesLocally(currentChat.value.id, ids)
      resetSelection()
    }
  } catch (error) {
    console.error('Ошибка удаления сообщений:', error)
  }
}

// ==================== Ответ на выбранное сообщение ====================
const replyToSelectedMessage = () => {
  if (!messageListRef.value) return
  const selectedIds = messageListRef.value.selectedIds
  if (selectedIds.size !== 1) return
  const selectedMessageId = Array.from(selectedIds)[0]
  const message = messages.value.find((m) => m.id === selectedMessageId)
  if (message) {
    messageStore.setReplyingToMessage(message)
    resetSelection()
    nextTick(() => {
      chatInputRef.value?.focus()
    })
  }
}

// ==================== Отправка сообщений ====================
const sendMessage = async (text: string, replyToId?: string, attachments?: Attachment[]) => {
  if (!currentUserId.value || !companionId.value) return
  sending.value = true
  try {
    let chat = currentChat.value
    let isFirst = false
    if (!chat) {
      isFirst = true
      const newChat = await chatStore.createOrGetChat(companionId.value)
      chatStore.addChat(newChat)
      currentChat.value = newChat
      chat = newChat
      await router.replace({ path: `/chats/${chat.id}`, query: route.query })
      wsStore.subscribeToChat(chat.id)
    }

    let replyContext = undefined
    const replyingMsg = messageStore.replyingToMessage
    if (replyToId && replyingMsg && replyingMsg.id === replyToId) {
      let senderName = 'Пользователь'
      if (replyingMsg.senderId === currentUserId.value) {
        const currentUser = userStore.user
        if (currentUser?.profile.firstName && currentUser?.profile.lastName) {
          senderName = `${currentUser.profile.firstName} ${currentUser.profile.lastName}`
        } else {
          senderName = currentUser?.profile.firstName || currentUser?.userName || 'Вы'
        }
      } else {
        const user = userStore.getUserFromCache(replyingMsg.senderId)
        if (user) {
          if (user.profile.firstName && user.profile.lastName) {
            senderName = `${user.profile.firstName} ${user.profile.lastName}`
          } else {
            senderName = user.profile.firstName || user.userName || 'Пользователь'
          }
        }
      }
      replyContext = {
        senderName,
        textSnippet:
          replyingMsg.text.length > 50 ? replyingMsg.text.substring(0, 50) + '…' : replyingMsg.text,
        originalMessageId: replyingMsg.id,
      }
    }

    const tempId = `temp_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
    const tempMessage: Message = {
      id: tempId,
      chatId: chat.id,
      senderId: currentUserId.value,
      receiverId: companionId.value,
      text: text || '',
      attachments: attachments || [],
      replyToId: replyToId || null,
      replyContext,
      timestamp: new Date().toISOString(),
      createdAt: new Date().toISOString(),
      status: 'SENT',
      deletedAt: null,
    }

    messageStore.addMessage(chat.id, tempMessage)
    wsStore.sendMessage(chat.id, companionId.value, tempMessage, isFirst, replyToId)

    if (replyToId) messageStore.clearReplyingToMessage()

    scrollToBottom()
  } catch (error) {
    console.error('Ошибка отправки:', error)
    alert('Не удалось отправить сообщение')
  } finally {
    sending.value = false
  }
}

const onTyping = (isTyping: boolean) => {
  if (currentChat.value) wsStore.sendTyping(currentChat.value.id, isTyping)
}

// ==================== Разблокировка ====================
const openConfirmBlockUser = () => {
  showConfirmBlockUser.value = true
}

const isBlocking = ref(false)

const unblockUser = async () => {
  if (!companionId.value) return
  unblocking.value = true
  isBlocking.value = true
  try {
    await userStore.removeFromBlackList(companionId.value)
    showConfirmBlockUser.value = false
  } catch (error) {
    console.error('Ошибка разблокировки:', error)
    alert('Не удалось разблокировать пользователя. Попробуйте позже.')
  } finally {
    unblocking.value = false
    isBlocking.value = false
  }
}

// ==================== Главная функция загрузки ====================
/**
 * 1. Восстанавливаем текущего пользователя (initAuth).
 * 2. Пытаемся взять companionId из query (важно для новых чатов).
 * 3. Грузим/создаём чат (по chatId или по участникам).
 * 4. Резолвим companionId окончательно (query → participants).
 * 5. Грузим companionUser.
 * 6. Подписываемся на статус собеседника.
 * 7. Навешиваем WS-обработчики и таймер lastSeen.
 */
const loadChatFromUrl = async (): Promise<boolean> => {
  // 1. Себя — до всего остального, иначе participants не разобрать.
  if (!userStore.user?.id) {
    try {
      await userStore.initAuth()
    } catch (e) {
      console.error('[ChatView] initAuth failed:', e)
    }
  }

  const chatId = route.params.chatId as string
  const isNew = !chatId || chatId === 'new'

  // 2. Для нового чата — companionId может прийти из query.
  //    Пробуем найти существующий диалог по участникам.
  if (isNew) {
    const queryCompanionId =
      typeof route.query.companionId === 'string' ? route.query.companionId : ''

    if (queryCompanionId && currentUserId.value) {
      try {
        const exist = await chatService.getChatByParticipants(currentUserId.value, queryCompanionId)
        if (exist) {
          await router.replace({ path: `/chats/${exist.id}`, query: route.query })
          currentChat.value = exist
          chatStore.addChat(exist)
          await wsStore.waitForConnection()
          wsStore.subscribeToChat(exist.id)
          await loadHistory()
        }
      } catch (e) {
        console.error('[ChatView] getChatByParticipants failed:', e)
      }
    }
  } else {
    // 3. Существующий чат — грузим из стора, иначе с бэка.
    let chat = chatStore.getChatById(chatId)
    if (!chat) {
      try {
        chat = await chatService.getDirectChat(chatId)
        if (chat) chatStore.addChat(chat)
      } catch (e) {
        console.error('[ChatView] getDirectChat failed:', e)
      }
    }
    if (chat) {
      currentChat.value = chat
      await wsStore.waitForConnection()
      wsStore.subscribeToChat(chat.id)
      await loadHistory()
    }
  }

  // 4. Резолвим companionId (query → participants)
  companionId.value = resolveCompanionId()
  if (!companionId.value) {
    console.warn(
      '[ChatView] companionId не определён — не удалось загрузить чат или отсутствует query',
    )
    return false
  }

  // 5. Грузим собеседника (кэш → API) и дожидаемся
  await loadCompanionUser(companionId.value)

  // 6. Подписка на статус собеседника
  if (wsStore.isConnected) {
    wsStore.subscribeToUserStatus(companionId.value)
  } else {
    await wsStore.waitForConnection()
    wsStore.subscribeToUserStatus(companionId.value)
  }

  // 7. WS-обработчики и таймер lastSeen
  unsubscribeWebSocket = setupWebSocketHandlers()

  if (timeUpdateInterval) clearInterval(timeUpdateInterval)
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 60000)

  return true
}

// ==================== Плавающая кнопка ====================
const isLastUnreadFullyVisible = (): boolean => {
  const container = messageListRef.value?.container
  if (!container) return false
  const unreadMessages = messages.value.filter(
    (msg) => msg.senderId !== currentUserId.value && msg.status !== 'READ',
  )
  if (unreadMessages.length === 0) return false
  const lastUnread = unreadMessages[unreadMessages.length - 1]
  const el = document.querySelector(`.chat-message[data-message-id="${lastUnread?.id}"]`)
  if (!el) return false
  const containerRect = container.getBoundingClientRect()
  const elRect = el.getBoundingClientRect()
  return elRect.top >= containerRect.top && elRect.bottom <= containerRect.bottom
}

const showFloatingButton = ref(false)

const updateFloatingButton = () => {
  if (isUserAtBottom.value) {
    showFloatingButton.value = false
    return
  }
  if (unreadMessagesCount.value > 0) {
    showFloatingButton.value = !isLastUnreadFullyVisible()
  } else {
    showFloatingButton.value = true
  }
}

let throttleTimer: ReturnType<typeof setTimeout> | null = null
const throttledUpdate = () => {
  if (throttleTimer) return
  throttleTimer = setTimeout(() => {
    updateFloatingButton()
    throttleTimer = null
  }, 100)
}

watch(
  () => messageListRef.value?.container,
  (container) => {
    if (!container) return
    container.addEventListener('scroll', throttledUpdate)
    return () => container.removeEventListener('scroll', throttledUpdate)
  },
  { immediate: true },
)

watch(
  [messages, unreadMessagesCount, () => messageListRef.value?.container],
  () => updateFloatingButton(),
  { deep: true, immediate: true },
)

watch(isUserAtBottom, () => updateFloatingButton())

// ==================== Пометить все сообщения как прочитанные ====================
const markAllUnreadAsRead = () => {
  if (!currentChat.value) return
  const chatId = currentChat.value.id

  const unreadMessages = messages.value.filter(
    (msg) => msg.senderId !== currentUserId.value && msg.status !== 'READ',
  )

  if (unreadMessages.length === 0) return

  messageStore.markAllAsRead(chatId, currentUserId.value)
  wsStore.markAllAsRead(chatId)
}

const onFloatingButtonClick = () => {
  markAllUnreadAsRead()
  scrollToBottom()
}

// Меню
const menuChat = ref<Chat | null>(null)

const openMenu = () => {
  menuChat.value = currentChat.value
}

const closeMenu = () => {
  menuChat.value = null
}

// ==================== WebSocket обработчики ====================
const setupWebSocketHandlers = () => {
  const handler = (message: Message) => {}
  wsStore.onMessageNew(handler)
  return () => wsStore.offMessageNew(handler)
}

let unsubscribeWebSocket: (() => void) | null = null

// ==================== Жизненный цикл ====================
onMounted(async () => {
  await loadChatFromUrl()
})

onUnmounted(() => {
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
    timeUpdateInterval = null
  }
  if (companionId.value) {
    wsStore.unsubscribeFromUserStatus(companionId.value)
  }
  if (unsubscribeWebSocket) unsubscribeWebSocket()
  cleanupObserver()
  if (currentChat.value) wsStore.unsubscribeFromChat(currentChat.value.id)
})

// ==================== Автоскролл при новых сообщениях ====================
watch(
  messages,
  async (newMessages, oldMessages) => {
    if (!currentChat.value || !newMessages.length) return
    const newLast = newMessages[newMessages.length - 1]
    const oldLast = oldMessages?.[oldMessages.length - 1]
    if (
      newMessages.length !== oldMessages?.length &&
      newLast &&
      (!oldLast || newLast.id !== oldLast.id) &&
      newLast.senderId === companionId.value &&
      isUserAtBottom.value
    ) {
      await nextTick()
      scrollToBottom()
    }
  },
  { deep: false },
)

// ==================== Навигация ====================
const goBack = () => router.push('/')
</script>
