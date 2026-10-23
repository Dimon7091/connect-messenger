// stores/websocketStore.ts
import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import { wsClient } from '@/services/websocket/WebSocketClient'
import { useMessageStore } from '@/stores/messageStore'
import { useUserStore } from '@/stores/userStore'
import type { Message, Chat } from '@/types/index'
import { useChatStore } from '@/stores/chatStore'
import type {
  WSEventType,
  SentMessagePayloand,
  MessageDeliveredPayload,
  MessageNewPayload,
  MessageReadPayload,
  TypingPayload,
  ErrorPayload,
  ConnectionPayload,
  ChatHistoryClearedResponse,
  UserStatusPayload,
  UserStatusRequest,
  AllMessagesReadPayload,
} from '@/types/websocket.types'

export const useWebSocketStore = defineStore('websocket', () => {
  // === State ===
  const isConnected = ref(false)
  const activeChatSubscriptions = ref<Map<string, boolean>>(new Map())

  // === ИСПРАВЛЕНО: хранилище для typing индикаторов (chatId -> string[]) ===
  // Используем reactive вместо ref<Map>, чтобы Vue отслеживал изменения внутри
  const typingUsers = reactive<Record<string, string[]>>({})

  // Хранилище для неподтвержденных сообщений (для retry логики)
  const pendingMessages = ref<
    Map<
      string,
      {
        message: Message
        retryCount: number
        timestamp: number
        receiverId: string
      }
    >
  >(new Map())

  // === Callback системы для компонентов ===
  const messageNewCallbacks = ref<Array<(message: Message) => void>>([])
  const messageSentCallbacks = ref<Array<(data: SentMessagePayloand) => void>>([])
  const connectionCallbacks = ref<Array<(status: boolean) => void>>([])
  const errorCallbacks = ref<Array<(error: ErrorPayload) => void>>([])

  let isInitialized = false

  // === Stores ===
  const messageStore = useMessageStore()
  const userStore = useUserStore()
  const chatStore = useChatStore()

  // === Computed ===
  const getTypingUsers = computed(() => (chatId: string): string[] => {
    return typingUsers[chatId] || []
  })

  const isUserTyping = computed(() => (chatId: string, userId: string): boolean => {
    return typingUsers[chatId]?.includes(userId) || false
  })

  const isReady = computed(() => isConnected.value)

  // === Private Methods ===

  // Хранилище таймаутов для печатающих пользователей (ключ "chatId:userId")
  const typingTimeouts = new Map<string, ReturnType<typeof setTimeout>>()

  // метод обновления статуса печатания
  const updateTypingStatus = (chatId: string, userId: string, isTyping: boolean): void => {
    const key = `${chatId}:${userId}`

    // Отменяем предыдущий таймаут, если он есть
    if (typingTimeouts.has(key)) {
      clearTimeout(typingTimeouts.get(key))
      typingTimeouts.delete(key)
    }

    const currentUsers = typingUsers[chatId] || []

    if (isTyping) {
      // Добавляем пользователя, если его ещё нет
      if (!currentUsers.includes(userId)) {
        typingUsers[chatId] = [...currentUsers, userId]
      }
      // Устанавливаем новый таймаут на удаление через 3 секунды
      const timeout = setTimeout(() => {
        const stillTyping = typingUsers[chatId] || []
        if (stillTyping.includes(userId)) {
          const filtered = stillTyping.filter((id) => id !== userId)
          if (filtered.length === 0) {
            delete typingUsers[chatId]
          } else {
            typingUsers[chatId] = filtered
          }
        }
        typingTimeouts.delete(key)
      }, 3000)
      typingTimeouts.set(key, timeout)
    } else {
      // Удаляем пользователя сразу (таймаут уже отменён)
      const filtered = currentUsers.filter((id) => id !== userId)
      if (filtered.length === 0) {
        delete typingUsers[chatId]
      } else {
        typingUsers[chatId] = filtered
      }
    }
  }

  const restoreChatSubscriptions = (): void => {
    for (const [chatKey] of activeChatSubscriptions.value.entries()) {
      wsClient.subscribeToChat(chatKey)
      console.log(`Restored subscription to chat: ${chatKey}`)
    }
  }

  const retryPendingMessages = (): void => {
    const now = Date.now()
    const timeout = 30000

    for (const [tempId, data] of pendingMessages.value.entries()) {
      if (now - data.timestamp > timeout) {
        pendingMessages.value.delete(tempId)
        messageStore.markAsFailed(data.message.chatId, tempId)
      } else if (data.retryCount < 3) {
        data.retryCount++

        const payload: SentMessagePayloand = {
          chatId: data.message.chatId,
          messageId: data.message.id,
          senderId: userStore.user?.id ?? '',
          receiverId: data.receiverId,
          serverMessageId: '',
          text: data.message.text,
          status: 'SENT',
          timestamp:
            typeof data.message.timestamp === 'string'
              ? data.message.timestamp
              : new Date().toISOString(),
        }

        wsClient.send('message_sent', payload)
        console.log(`Retrying message ${tempId}, attempt ${data.retryCount}`)
      } else {
        pendingMessages.value.delete(tempId)
        messageStore.markAsFailed(data.message.chatId, tempId)
      }
    }
  }

  // === Public Callback Methods ===

  const onMessageNew = (callback: (message: Message) => void): (() => void) => {
    messageNewCallbacks.value.push(callback)
    console.log(
      `✅ Callback registered for message_new, total: ${messageNewCallbacks.value.length}`,
    )
    return () => {
      const index = messageNewCallbacks.value.indexOf(callback)
      if (index !== -1) messageNewCallbacks.value.splice(index, 1)
      console.log(`❌ Callback unregistered, remaining: ${messageNewCallbacks.value.length}`)
    }
  }

  const offMessageNew = (callback: (message: Message) => void): void => {
    const index = messageNewCallbacks.value.indexOf(callback)
    if (index !== -1) messageNewCallbacks.value.splice(index, 1)
  }

  const onMessageSent = (callback: (data: SentMessagePayloand) => void): (() => void) => {
    messageSentCallbacks.value.push(callback)
    return () => {
      const index = messageSentCallbacks.value.indexOf(callback)
      if (index !== -1) messageSentCallbacks.value.splice(index, 1)
    }
  }

  const offMessageSent = (callback: (data: SentMessagePayloand) => void): void => {
    const index = messageSentCallbacks.value.indexOf(callback)
    if (index !== -1) messageSentCallbacks.value.splice(index, 1)
  }

  const onConnectionChange = (callback: (status: boolean) => void): (() => void) => {
    connectionCallbacks.value.push(callback)
    return () => {
      const index = connectionCallbacks.value.indexOf(callback)
      if (index !== -1) connectionCallbacks.value.splice(index, 1)
    }
  }

  const onError = (callback: (error: ErrorPayload) => void): (() => void) => {
    errorCallbacks.value.push(callback)
    return () => {
      const index = errorCallbacks.value.indexOf(callback)
      if (index !== -1) errorCallbacks.value.splice(index, 1)
    }
  }

  // === ИНИЦИАЛИЗАЦИЯ ===
  const init = (): void => {
    if (isInitialized) {
      console.log('⚠️ WebSocketStore already initialized, skipping')
      return
    }

    const token = userStore.token
    if (!token) {
      console.error('No token for WebSocket connection')
      return
    }

    console.log('🔧 Initializing WebSocketStore...')
    isInitialized = true
    wsClient.connect(token)

    registerWebSocketHandlers()
  }

  const registerWebSocketHandlers = (): void => {
    console.log('📡 Registering WebSocket handlers...')

    // Подписка на статус соединения
    wsClient.on('connection', (payload: unknown) => {
      const data = payload as ConnectionPayload
      isConnected.value = data.status === 'connected'
      console.log(`WebSocket connection status: ${data.status}`)

      connectionCallbacks.value.forEach((cb) => cb(isConnected.value))

      if (isConnected.value) {
        wsClient.subscribeToUserQueue()
        restoreChatSubscriptions()
        retryPendingMessages()
      }
    })

    // Подписка на новые сообщения
    wsClient.on('message_new', (payload: unknown) => {
      const data = payload as MessageNewPayload
      console.log('📨 New message received:', data)

      if (data.chat) {
        const newChat: Chat = {
          id: data.chat.id,
          type: data.chat.type,
          participants: data.chat.participants,
          unreadCount: data.chat.unreadCount ?? '1',
          lastMessage: data.chat.lastMessage,
          createdAt: data.chat.createdAt,
          updatedAt: data.chat.updatedAt,
        }
        if (chatStore.hasChat(data.chat.id)) {
          console.log('✅ Чат существует, обновляем существующий:', data.chat)
          chatStore.updateChatLocally(data.chat.id, {
            unreadCount: data.chat.unreadCount,
            lastMessage: data.chat.lastMessage || '',
            updatedAt: data.chat.updatedAt,
          })
        } else {
          console.log('✅ Чат не существует, добавляем чат:', data.chat)
          chatStore.addChat(newChat)
        }
      }

      const processedMessage: Message = {
        id: data.id,
        chatId: data.chatId,
        senderId: data.senderId,
        receiverId: data.receiverId,
        text: data.text,
        timestamp: data.timestamp,
        createdAt: data.createdAt,
        status: data.status.toLowerCase() as 'SENT' | 'DELIVERED' | 'READ' | 'FAILED',
        deletedAt: data.deletedAt ? new Date(data.deletedAt) : undefined,
        attachments: data.attachments,
        replyToId: data.replyToId,
        replyContext: data.replyContext,
        readBy: data.readBy,
      }

      messageStore.addMessage(processedMessage.chatId, processedMessage)
      console.log('✅ Отправляем подтверждение доставки сообщения:', data.chat)
      markAsDelivered(processedMessage.chatId, processedMessage.id)

      messageNewCallbacks.value.forEach((cb) => {
        try {
          cb(processedMessage)
        } catch (error) {
          console.error('Error in message_new callback:', error)
        }
      })
    })

    wsClient.on('message_delivered', (payload: unknown) => {
      const data = payload as MessageDeliveredPayload
      console.log('✅ Message delivered:', data)
      messageStore.markAsDelivered(data.chatId, data.messageId)
      if (pendingMessages.value.has(data.messageId)) {
        pendingMessages.value.delete(data.messageId)
      }
    })

    wsClient.on('message_read', (payload: unknown) => {
      const data = payload as MessageReadPayload
      console.log('👁️ Message read:', data)
      messageStore.markAsRead(data.chatId, data.messageId)
      chatStore.decrementChatUnreadCountLocally(data.chatId)
    })

    wsClient.on('all_messages_read', (payload: unknown) => {
      const data = payload as AllMessagesReadPayload
      console.log('👁️ AllMessages read:', data)
      messageStore.markAllAsRead(data.chatId, userStore.user?.id!)
      chatStore.clearChatUnreadCountLocally(data.chatId)
    })

    wsClient.on('message_deleted', (payload: unknown) => {
      const data = payload as {
        chatId: string
        deletedMessagesIds: string[] | number[]
        chatUpdatedAt: string
        unreadCount: number
        lastMessage: string | null
      }
      const deletedIds = data.deletedMessagesIds.map((id) => String(id))
      messageStore.deleteMessagesLocally(data.chatId, deletedIds)
      chatStore.updateChatLocally(data.chatId, {
        unreadCount: data.unreadCount,
        lastMessage: data.lastMessage || '',
        updatedAt: data.chatUpdatedAt,
      })
      chatStore.$state = { ...chatStore.$state }
      console.log(
        `🗑️ Deleted ${deletedIds.length} messages in chat ${data.chatId}, lastMessage: "${data.lastMessage}"`,
      )
    })

    wsClient.on('chat_history_cleared', (payload: unknown) => {
      const data = payload as ChatHistoryClearedResponse
      console.log('Chat cleared:', data)
      messageStore.clearMessages(data.chatId)
      if (!chatStore.hasChat(data.chatId)) {
        chatStore.updateChatLocally(data.chatId, {
          lastMessage: 'Нет сообщений',
          unreadCount: 0,
          updatedAt: new Date().toISOString(),
        })
      }
      chatStore.updateChatLocally(data.chatId, {
        lastMessage: 'Нет сообщений',
        unreadCount: 0,
        updatedAt: new Date().toISOString(),
      })
    })

    wsClient.on('user_status', (payload: unknown) => {
      const data = payload as UserStatusPayload
      console.log('🟢 User status changed:', data)
      userStore.updateUserStatus(data.userId as string, data.status, data.lastSeen)
    })

    wsClient.on('typing_start', (payload: unknown) => {
      const data = payload as TypingPayload
      console.log('✏️ User typing:', data)
      updateTypingStatus(data.chatId, data.userId, true)
    })

    wsClient.on('typing_stop', (payload: unknown) => {
      const data = payload as TypingPayload
      console.log('✏️ User stopped typing:', data)
      updateTypingStatus(data.chatId, data.userId, false)
    })

    wsClient.on('message_sent', (payload: unknown) => {
      const data = payload as SentMessagePayloand
      console.log('📤 Message sent confirmation:', data)
      if (pendingMessages.value.has(data.messageId)) {
        const pending = pendingMessages.value.get(data.messageId)!
        pendingMessages.value.delete(data.messageId)
        messageStore.updateMessageId(pending.message.chatId, data.messageId, data.serverMessageId)
      }
      messageSentCallbacks.value.forEach((cb) => {
        try {
          cb(data)
        } catch (error) {
          console.error('Error in message_sent callback:', error)
        }
      })
    })

    wsClient.on('error', (payload: unknown) => {
      const error = payload as ErrorPayload
      console.error('WebSocket error:', error.message)
      errorCallbacks.value.forEach((cb) => cb(error))
    })
  }

  // === Public Methods ===

  const subscribeToChat = (chatKey: string): void => {
    if (!activeChatSubscriptions.value.has(chatKey)) {
      wsClient.subscribeToChat(chatKey)
      activeChatSubscriptions.value.set(chatKey, true)
      console.log(`✅ Subscribed to chat: ${chatKey}`)
    } else {
      console.log(`Already subscribed to chat: ${chatKey}`)
    }
  }

  const unsubscribeFromChat = (chatKey: string): void => {
    if (activeChatSubscriptions.value.has(chatKey)) {
      wsClient.unsubscribeFromChat(chatKey)
      activeChatSubscriptions.value.delete(chatKey)
      console.log(`Unsubscribed from chat: ${chatKey}`)
    }
  }

  const isSubscribedToChat = (chatKey: string): boolean => {
    return activeChatSubscriptions.value.get(chatKey) || false
  }

  const subscribeToUserStatus = (userId: string): void => {
    if (!isConnected.value) {
      console.warn('WebSocket not connected, cannot subscribe to user status')
      return
    }
    wsClient.subscribeToUserStatus(userId)
    console.log(`✅ Subscribed to status of user ${userId}`)
  }

  const unsubscribeFromUserStatus = (userId: string): void => {
    if (!isConnected.value) {
      console.warn('WebSocket not connected, cannot unsubscribe from user status')
      return
    }
    wsClient.unsubscribeFromUserStatus(userId)
    console.log(`✅ Unsubscribed from status of user ${userId}`)
  }

  const requestUserStatus = (userId: string): void => {
    subscribeToUserStatus(userId)
  }

  const sendMessage = (
    chatId: string,
    receiverId: string,
    message: Message,
    isFistMessage: boolean,
    replyToId?: string,
  ): void => {
    if (!isConnected.value) {
      console.warn('WebSocket not connected, message will be queued')
    }
    console.log('✅ Сообщение:', chatId)

    pendingMessages.value.set(message.id, {
      message,
      retryCount: 0,
      timestamp: Date.now(),
      receiverId,
    })

    const payload: SentMessagePayloand = {
      chatId: chatId,
      messageId: message.id,
      senderId: userStore.user?.id ?? '',
      receiverId: receiverId,
      serverMessageId: '',
      text: message.text,
      status: 'SENT',
      timestamp:
        typeof message.timestamp === 'string' ? message.timestamp : new Date().toISOString(),
      attachments: message.attachments,
      replyToId: replyToId || null,
    }

    wsClient.send('message_sent', payload)
    console.log(`📨 Message sent: ${message.id} to chat ${chatId}`)
  }

  const markAsRead = (chatId: string, messageId: string): void => {
    if (!isConnected.value) return
    const payload: MessageReadPayload = {
      chatId,
      messageId,
      userId: userStore.user?.id || '',
    }
    wsClient.send('message_read', payload)
  }

  const markAllAsRead = (chatId: string): void => {
    if (!isConnected.value) return
    const payload: AllMessagesReadPayload = {
      chatId,
    }
    wsClient.send('all_messages_read', payload)
  }

  const markAsDelivered = (chatId: string, messageId: string): void => {
    if (!isConnected.value) return
    const payload: MessageDeliveredPayload = {
      chatId,
      messageId,
      userId: userStore.user?.id || '',
    }
    wsClient.send('message_delivered', payload)
  }

  const sendTyping = (chatId: string, isTyping: boolean): void => {
    if (!isConnected.value) return

    if (isTyping) {
      const lastTyping = (window as any).__lastTypingTime?.[chatId] || 0
      if (Date.now() - lastTyping < 2000) {
        return
      }
      if (!(window as any).__lastTypingTime) {
        ;(window as any).__lastTypingTime = {}
      }
      ;(window as any).__lastTypingTime[chatId] = Date.now()
    }

    const payload: TypingPayload = {
      chatId: chatId.toString(),
      userId: userStore.user?.id || '',
    }
    wsClient.send(isTyping ? 'typing_start' : 'typing_stop', payload)
  }

  const disconnect = (): void => {
    wsClient.disconnect()
    isConnected.value = false
    activeChatSubscriptions.value.clear()
    // Очистка реактивного объекта typingUsers
    Object.keys(typingUsers).forEach((key) => delete typingUsers[key])
    pendingMessages.value.clear()
    isInitialized = false
    connectionCallbacks.value.forEach((cb) => cb(false))
    // Очищаем все таймауты
    for (const timeout of typingTimeouts.values()) {
      clearTimeout(timeout)
    }
    typingTimeouts.clear()
    // очистка typingUsers
    Object.keys(typingUsers).forEach((key) => delete typingUsers[key])
    console.log('WebSocket disconnected and cleaned up')
  }

  const reconnect = (): void => {
    const token = userStore.token
    if (token) {
      disconnect()
      init()
    }
  }

  const reset = (): void => {
    isInitialized = false
    messageNewCallbacks.value = []
    messageSentCallbacks.value = []
    connectionCallbacks.value = []
    errorCallbacks.value = []
    pendingMessages.value.clear()
    activeChatSubscriptions.value.clear()
    Object.keys(typingUsers).forEach((key) => delete typingUsers[key])
    console.log('WebSocketStore reset')
  }

  const waitForConnection = (timeout: number = 5000): Promise<boolean> => {
    return new Promise((resolve) => {
      if (isConnected.value) {
        resolve(true)
        return
      }
      const startTime = Date.now()
      const checkInterval = setInterval(() => {
        if (isConnected.value) {
          clearInterval(checkInterval)
          resolve(true)
        } else if (Date.now() - startTime > timeout) {
          clearInterval(checkInterval)
          resolve(false)
        }
      }, 100)
    })
  }

  const getActiveSubscriptions = (): string[] => {
    return Array.from(activeChatSubscriptions.value.keys())
  }

  return {
    // State (только то, что нужно снаружи)
    isConnected,
    activeChatSubscriptions,
    pendingMessages,
    // typingUsers не экспортируем (вместо него используем геттеры)

    // Computed
    getTypingUsers,
    isUserTyping,
    isReady,

    // Callback registration
    onMessageNew,
    offMessageNew,
    onMessageSent,
    offMessageSent,
    onConnectionChange,
    onError,

    // Methods
    init,
    subscribeToChat,
    unsubscribeFromChat,
    isSubscribedToChat,
    subscribeToUserStatus,
    unsubscribeFromUserStatus,
    requestUserStatus,
    sendMessage,
    markAsRead,
    markAllAsRead,
    markAsDelivered,
    sendTyping,
    disconnect,
    reconnect,
    waitForConnection,
    getActiveSubscriptions,
    reset,
  }
})
