// stores/messageStore.ts
import { defineStore } from 'pinia'
import { ref, type Ref } from 'vue'
import type { Message, Attachment } from '@/types/index'
import { messageService } from '@/services/http/MessageService'
import { triggerRef } from 'vue'
import { chatService } from '@/services/http/ChatService'

// ==================== Вспомогательная функция для приведения к миллисекундам ====================
const toMs = (value: string | number): number => {
  if (typeof value === 'number') {
    return value < 1e12 ? value * 1000 : value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    if (!isNaN(parsed)) {
      return parsed < 1e12 ? parsed * 1000 : parsed
    }
    return new Date(value).getTime()
  }
  return 0
}

// Функция сравнения сообщений по createdAt (вторичный ключ – id для стабильности)
const compareMessages = (a: Message, b: Message): number => {
  const timeA = toMs(a.createdAt)
  const timeB = toMs(b.createdAt)
  if (timeA !== timeB) return timeA - timeB
  return a.id.localeCompare(b.id)
}

export const useMessageStore = defineStore('message', () => {
  // ==================== Внутреннее состояние (через Record) ====================
  const messagesMap = ref<Record<string, Message[]>>({})
  const pendingMessagesMap = ref<Record<string, { tempId: string; text: string; chatId: string }>>(
    {},
  )
  const paginationMap = ref<
    Record<string, { hasMore: boolean; page: number; total: number; lastTimestamp?: string }>
  >({})
  const loadingMessagesMap = ref<Record<string, boolean>>({})
  const replyingToMessage = ref<Message | null>(null) // Сообщение, на которое отвечаем

  // ==================== Управление режимом ответа ====================
  const setReplyingToMessage = (message: Message | null) => {
    replyingToMessage.value = message
  }
  const clearReplyingToMessage = () => {
    replyingToMessage.value = null
  }

  // ==================== Геттеры ====================
  const getMessages = (chatId: string): Message[] => messagesMap.value[chatId] || []

  const getLastMessage = (chatId: string): Message | null => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages || chatMessages.length === 0) return null
    return chatMessages[chatMessages.length - 1] ?? null
  }
  const getUnreadMessages = (chatId: string, currentUserId: string): Message[] => {
    const chatMessages = messagesMap.value[chatId] || []
    return chatMessages.filter(
      (m) =>
        m.senderId !== currentUserId &&
        m.status !== 'READ' &&
        m.status !== 'DELIVERED' &&
        !m.deletedAt,
    )
  }
  const getUnreadCount = (chatId: string, currentUserId: string): number =>
    getUnreadMessages(chatId, currentUserId).length
  const isLoading = (chatId: string): boolean => loadingMessagesMap.value[chatId] || false
  const hasMoreMessages = (chatId: string): boolean => paginationMap.value[chatId]?.hasMore ?? true
  const getMessageById = (chatId: string, messageId: string): Message | null => {
    const chatMessages = messagesMap.value[chatId] || []
    return chatMessages.find((m) => m.id === messageId) || null
  }
  const getRepliedMessage = (chatId: string, replyToId: string): Message | null => {
    const chatMessages = messagesMap.value[chatId] || []
    return chatMessages.find((m) => m.id === replyToId) || null
  }

  // ==================== Методы для загрузки сообщений ====================
  const loadMessages = async (
    chatId: string,
    limit: number = 50,
    force: boolean = false,
  ): Promise<void> => {
    if (chatId === 'new') return
    if (isLoading(chatId)) return
    // Если сообщения уже есть и не требуется принудительная загрузка – пропускаем
    if (!force && getMessages(chatId).length > 0) {
      console.log(`📦 Messages for chat ${chatId} already loaded, skipping fetch`)
      return
    }
    setLoading(chatId, true)
    try {
      const chatMessages = await messageService.getChatMessages(chatId, limit)
      if (chatMessages && chatMessages.length > 0) {
        setMessages(chatId, chatMessages)
        const hasMore = chatMessages.length === limit
        const lastTimestamp =
          chatMessages.length > 0
            ? (chatMessages[chatMessages.length - 1]?.timestamp as string)
            : undefined
        setPagination(chatId, hasMore, 0, chatMessages.length, lastTimestamp)
        console.log(`✅ Loaded ${chatMessages.length} messages for chat ${chatId}`)
      } else {
        setPagination(chatId, false, 0, 0, undefined)
      }
    } catch (error) {
      console.error(`❌ Error loading messages for chat ${chatId}:`, error)
    } finally {
      setLoading(chatId, false)
    }
  }

  const loadMoreMessages = async (chatId: string, limit: number = 30): Promise<void> => {
    const paginationInfo = paginationMap.value[chatId]
    if (!paginationInfo?.hasMore) return
    if (isLoading(chatId)) return
    setLoading(chatId, true)
    try {
      const beforeTimestamp = paginationInfo.lastTimestamp
      const newMessages = await messageService.getChatMessages(chatId, limit, beforeTimestamp)
      if (newMessages && newMessages.length > 0) {
        addMessages(chatId, newMessages, true)
        const hasMore = newMessages.length === limit
        const lastTimestamp =
          newMessages.length > 0
            ? (newMessages[newMessages.length - 1]?.timestamp as string)
            : paginationInfo.lastTimestamp
        setPagination(
          chatId,
          hasMore,
          paginationInfo.page + 1,
          paginationInfo.total + newMessages.length,
          lastTimestamp,
        )
        console.log(`✅ Loaded ${newMessages.length} more messages for chat ${chatId}`)
      } else {
        setPagination(
          chatId,
          false,
          paginationInfo.page,
          paginationInfo.total,
          paginationInfo.lastTimestamp,
        )
      }
    } catch (error) {
      console.error(`❌ Error loading more messages for chat ${chatId}:`, error)
    } finally {
      setLoading(chatId, false)
    }
  }

  const loadMessagesAround = async (chatId: string, messageId: string): Promise<void> => {
    setLoading(chatId, true)
    try {
      await loadMessages(chatId, 100)
    } catch (error) {
      console.error(`❌ Error loading messages around ${messageId}:`, error)
    } finally {
      setLoading(chatId, false)
    }
  }

  const setPagination = (
    chatId: string,
    hasMore: boolean,
    page: number,
    total: number,
    lastTimestamp?: string,
  ): void => {
    paginationMap.value = {
      ...paginationMap.value,
      [chatId]: { hasMore, page, total, lastTimestamp },
    }
  }
  const resetPagination = (chatId: string): void => {
    const { [chatId]: _, ...rest } = paginationMap.value
    paginationMap.value = rest
  }
  const refreshMessages = async (chatId: string, limit: number = 50): Promise<void> => {
    clearMessages(chatId)
    resetPagination(chatId)
    await loadMessages(chatId, limit, true)
  }

  // ==================== Основные методы ====================
  const generateTempId = (): string =>
    `temp_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`

  const addOptimisticMessage = (
    chatId: string,
    text: string,
    senderId: string,
    receiverId: string,
    replyToId?: string | null,
    attachments?: Attachment[],
    replyContext?: Message['replyContext'],
  ): Message => {
    const tempId = generateTempId()
    const tempMessage: Message = {
      id: tempId,
      chatId,
      senderId,
      receiverId,
      text,
      timestamp: new Date().toISOString(),
      createdAt: new Date().toISOString(),
      status: 'SENT',
      attachments,
      replyToId: replyToId || null,
      replyContext: replyContext || undefined,
    }
    const currentMessages = messagesMap.value[chatId] || []
    messagesMap.value = { ...messagesMap.value, [chatId]: [...currentMessages, tempMessage] }
    pendingMessagesMap.value = { ...pendingMessagesMap.value, [tempId]: { tempId, text, chatId } }
    console.log(`📝 Optimistic message added: ${tempId}`)
    return tempMessage
  }

  const confirmMessage = (tempId: string, realMessage: Message): void => {
    for (const [chatId, chatMessages] of Object.entries(messagesMap.value)) {
      const index = chatMessages.findIndex((m) => m.id === tempId)
      if (index !== -1) {
        const oldMessage = chatMessages[index]
        const mergedMessage: Message = {
          ...realMessage,
          status: 'SENT', // TypeScript выводит литеральный тип 'SENT'
          replyContext: realMessage.replyContext ?? oldMessage?.replyContext,
        }
        const updatedMessages = [...chatMessages]
        updatedMessages[index] = mergedMessage
        messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
        triggerRef(messagesMap)
        const { [tempId]: _, ...rest } = pendingMessagesMap.value
        pendingMessagesMap.value = rest
        console.log(`✅ Message confirmed: ${tempId} -> ${realMessage.id}`)
        break
      }
    }
  }

  const markMessageAsError = (tempId: string): void => {
    for (const [chatId, chatMessages] of Object.entries(messagesMap.value)) {
      const index = chatMessages.findIndex((m) => m.id === tempId)
      if (index !== -1) {
        const updatedMessages = chatMessages.map((msg, i) =>
          i === index ? { ...msg, status: 'FAILED' as const } : msg,
        )
        messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
        console.log(`❌ Message marked as error: ${tempId}`)
        break
      }
    }
  }

  const markAsFailed = (chatId: string, messageId: string): void => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    const updatedMessages = chatMessages.map((msg) =>
      msg.id === messageId ? { ...msg, status: 'FAILED' as const } : msg,
    )
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    console.log(`❌ Message marked as failed: ${messageId}`)
  }

  const updateMessageId = (chatId: string, tempId: string, serverId: string): void => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    const updatedMessages = chatMessages.map((msg) =>
      msg.id === tempId ? { ...msg, id: serverId } : msg,
    )
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    console.log(`🔄 Message ID updated: ${tempId} -> ${serverId}`)
  }

  const addMessage = (chatId: string, message: Message): void => {
    const currentMessages = messagesMap.value[chatId] || []
    const exists = currentMessages.some((m) => m.id === message.id)
    if (exists) return
    messagesMap.value = { ...messagesMap.value, [chatId]: [...currentMessages, message] }
    console.log(`📨 Message added to chat ${chatId}: ${message.id}`)
  }

  const addMessages = (chatId: string, newMessages: Message[], prepend = false): void => {
    const currentMessages = messagesMap.value[chatId] || []
    const uniqueNewMessages = newMessages.filter(
      (m) => !currentMessages.some((existing) => existing.id === m.id),
    )
    if (uniqueNewMessages.length === 0) return
    const sortedMessages = [...uniqueNewMessages].sort(compareMessages)
    const updatedMessages = prepend
      ? [...sortedMessages, ...currentMessages]
      : [...currentMessages, ...sortedMessages]
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    console.log(
      `📚 Added ${uniqueNewMessages.length} messages to chat ${chatId} (prepend: ${prepend})`,
    )
  }

  const setMessages = (chatId: string, historyMessages: Message[]): void => {
    if (!chatId || chatId === 'new') return
    const sortedMessages = [...historyMessages].sort(compareMessages)
    messagesMap.value = { ...messagesMap.value, [chatId]: sortedMessages }
    console.log(`📚 History loaded for chat ${chatId}: ${sortedMessages.length} messages`)
  }

  const updateMessageStatus = (
    chatId: string,
    messageId: string,
    status: Message['status'],
  ): void => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    const updatedMessages = chatMessages.map((msg) =>
      msg.id === messageId ? { ...msg, status } : msg,
    )
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    triggerRef(messagesMap)
    console.log(`📝 Message ${messageId} status updated to ${status}`)
  }

  const markAsDelivered = (chatId: string, messageId: string): void =>
    updateMessageStatus(chatId, messageId, 'DELIVERED')

  const markAsRead = (chatId: string, messageId: string): void =>
    updateMessageStatus(chatId, messageId, 'READ')

  const markAllAsRead = (chatId: string, currentUserId: string): void => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    let hasChanges = false
    const updatedMessages = chatMessages.map((m) => {
      if (m.senderId !== currentUserId && m.status !== 'READ' && !m.deletedAt) {
        hasChanges = true
        return { ...m, status: 'READ' as const }
      }
      return m
    })
    if (hasChanges) {
      messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
      console.log(`👁️ All messages in chat ${chatId} marked as read`)
    }
  }

  const deleteMessagesLocally = (chatId: string, messageIds: string | string[]): void => {
    const ids = Array.isArray(messageIds) ? messageIds : [messageIds]
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    const updatedMessages = chatMessages.filter((msg) => !ids.includes(msg.id))
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    triggerRef(messagesMap)
    console.log(`🗑️ Locally deleted ${ids.length} message(s) from chat ${chatId}`)
  }

  const deleteMessagesOnServer = async (
    chatId: string,
    messagesIds: string[],
  ): Promise<boolean> => {
    try {
      await messageService.deleteMessages({ messagesIds, chatId })
      return true
    } catch (error) {
      console.error(`❌ Failed to delete messages on server:`, error)
      return false
    }
  }

  const softDeleteMessage = (chatId: string, messageId: string): void => {
    const chatMessages = messagesMap.value[chatId]
    if (!chatMessages) return
    const updatedMessages = chatMessages.map((msg) =>
      msg.id === messageId ? { ...msg, deletedAt: new Date() } : msg,
    )
    messagesMap.value = { ...messagesMap.value, [chatId]: updatedMessages }
    console.log(`🗑️ Message ${messageId} soft deleted`)
  }

  const clearMessages = (chatId: string): void => {
    const { [chatId]: _, ...rest } = messagesMap.value
    messagesMap.value = rest
    const { [chatId]: __, ...restPagination } = paginationMap.value
    paginationMap.value = restPagination
    const { [chatId]: ___, ...restLoading } = loadingMessagesMap.value
    loadingMessagesMap.value = restLoading
    console.log(`🧹 Messages cleared for chat ${chatId}`)
  }

  const clearChatHistory = async (chatId: string): Promise<boolean> => {
    try {
      await chatService.clearChatHistory(chatId) // специальный метод бекенда
      clearMessages(chatId) // локальная очистка
      console.log(`🧹 История чата ${chatId} полностью очищена`)
      return true
    } catch (error) {
      console.error(`❌ Ошибка при очистке истории чата ${chatId}:`, error)
      return false
    }
  }

  const setLoading = (chatId: string, isLoading: boolean): void => {
    loadingMessagesMap.value = { ...loadingMessagesMap.value, [chatId]: isLoading }
  }

  const rollbackAllPending = (): void => {
    Object.keys(pendingMessagesMap.value).forEach((tempId) => markMessageAsError(tempId))
    pendingMessagesMap.value = {}
  }

  const getChatStats = (chatId: string, currentUserId: string) => {
    const chatMessages = messagesMap.value[chatId] || []
    const total = chatMessages.length
    const unread = chatMessages.filter(
      (m) => m.senderId !== currentUserId && m.status !== 'READ' && !m.deletedAt,
    ).length
    const sent = chatMessages.filter((m) => m.senderId === currentUserId && !m.deletedAt).length
    const received = total - sent
    return { total, unread, sent, received }
  }

  const replyToMessage = (
    chatId: string,
    replyToId: string,
    text: string,
    senderId: string,
  ): Message => addOptimisticMessage(chatId, text, senderId, replyToId)

  return {
    messages: messagesMap,
    pendingMessages: pendingMessagesMap,
    pagination: paginationMap,
    loadingMessages: loadingMessagesMap,

    getMessages,
    getLastMessage,
    getUnreadMessages,
    getUnreadCount,
    isLoading,
    hasMoreMessages,
    getMessageById,
    getRepliedMessage,
    getChatStats,
    replyingToMessage,
    setReplyingToMessage,
    clearReplyingToMessage,

    clearChatHistory,
    deleteMessagesLocally,
    deleteMessagesOnServer,
    generateTempId,
    addOptimisticMessage,
    confirmMessage,
    markMessageAsError,
    markAsFailed,
    updateMessageId,
    addMessage,
    addMessages,
    setMessages,
    updateMessageStatus,
    markAsDelivered,
    markAsRead,
    markAllAsRead,
    softDeleteMessage,
    clearMessages,
    setLoading,
    setPagination,
    resetPagination,
    rollbackAllPending,
    replyToMessage,

    loadMessages,
    loadMoreMessages,
    loadMessagesAround,
    refreshMessages,
  }
})
