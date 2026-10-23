// stores/chatStore.ts - оптимизированная версия
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatService } from '@/services/http/ChatService'
import { useMessageStore } from './messageStore'
import type { Chat, User } from '@/types/index'

export const useChatStore = defineStore('chat', () => {
  // ==================== Состояние ====================
  const chats = ref<Chat[]>([])
  const currentChat = ref<Chat | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pendingChats = ref<Map<string, { chat: Chat; userId: string }>>(new Map())

  // ==================== Геттеры ====================
  const allChats = computed(() => chats.value)

  const sortedChats = computed(() => {
    // Оптимизация: сортировка без лишних преобразований
    return [...chats.value].sort(
      (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
    )
  })

  const directChats = computed(() => chats.value.filter((chat) => chat.type === 'DIRECT'))

  const getChatById = (chatId: string): Chat | undefined =>
    chats.value.find((chat) => chat.id === chatId)

  const getDirectChatWithUser = (userId: string): Chat | undefined => {
    const realChat = chats.value.find(
      (chat) => chat.type === 'DIRECT' && chat.participants.includes(userId),
    )
    if (realChat) return realChat
    const pending = pendingChats.value.get(userId)
    return pending?.chat
  }

  const getUnreadCountByChatId = (chatId: string): number => {
    const chat = getChatById(chatId)
    return chat?.unreadCount ?? 0
  }

  const getLastMessageByChatId = (chatId: string): string => {
    const chat = getChatById(chatId)
    return chat?.lastMessage ?? ''
  }

  const unreadChatsCount = computed(
    () => chats.value.filter((chat) => Number(chat.unreadCount || 0) > 0).length,
  )

  // ==================== API методы ====================
  const fetchChats = async (): Promise<void> => {
    loading.value = true
    error.value = null
    try {
      chats.value = await chatService.getChats()
    } catch (err) {
      error.value = 'Ошибка загрузки чатов'
      console.error('Ошибка загрузки чатов:', err)
    } finally {
      loading.value = false
    }
  }

  const createLocalChat = (userId: string, currentUserId: string): Chat => {
    const tempId = `temp_${Date.now()}_${userId}`
    const tempChat: Chat = {
      id: tempId,
      type: 'DIRECT',
      participants: [userId, currentUserId],
      unreadCount: 0,
      lastMessage: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    pendingChats.value.set(userId, { chat: tempChat, userId })
    chats.value.unshift(tempChat)
    console.log('📝 Оптимистическое создание чата:', tempChat)
    return tempChat
  }

  const createOrGetChat = async (companionId: string): Promise<Chat> => {
    try {
      return await chatService.createOrGetDirectChat(companionId)
    } catch (err) {
      console.error('Ошибка создания/получения чата:', err)
      throw err
    }
  }

  const getChatByParticipants = async (
    participant1: string,
    participant2: string,
  ): Promise<Chat | undefined> => {
    try {
      const chat = await chatService.getChatByParticipants(participant1, participant2)
      if (!chat) console.warn('Чат не найден для участников:', participant1, participant2)
      return chat
    } catch (err) {
      console.error('Ошибка получения чата:', err)
      throw err
    }
  }

  const addChat = (chat: Chat): void => {
    if (!chats.value.some((c) => c.id === chat.id)) {
      chats.value.unshift(chat)
      console.log('➕ Чат добавлен в store:', chat.id)
    }
  }

  const confirmChat = (tempChatId: string, realChat: Chat): void => {
    const index = chats.value.findIndex((c) => c.id === tempChatId)
    if (index !== -1) chats.value[index] = realChat
    for (const [userId, data] of pendingChats.value.entries()) {
      if (data.chat.id === tempChatId) {
        pendingChats.value.delete(userId)
        break
      }
    }
  }

  const rollbackChat = (tempChatId: string): void => {
    const index = chats.value.findIndex((c) => c.id === tempChatId)
    if (index !== -1) chats.value.splice(index, 1)
    for (const [userId, data] of pendingChats.value.entries()) {
      if (data.chat.id === tempChatId) {
        pendingChats.value.delete(userId)
        break
      }
    }
  }

  const updateChat = async (chatId: string, data: Partial<Chat>): Promise<Chat | null> => {
    try {
      const updatedChat = await chatService.updateChat(chatId, data)
      const index = chats.value.findIndex((c) => c.id === chatId)
      if (index !== -1) chats.value[index] = updatedChat
      if (currentChat.value?.id === chatId) currentChat.value = updatedChat
      return updatedChat
    } catch (err) {
      console.error('Ошибка обновления чата:', err)
      return null
    }
  }

  const deleteChat = async (chatId: string, userId: string): Promise<boolean> => {
    try {
      await chatService.deleteChat(chatId)
      chats.value = chats.value.filter((c) => c.id !== chatId)
      if (currentChat.value?.id === chatId) currentChat.value = null
      return true
    } catch (err) {
      console.error('Ошибка удаления чата:', err)
      return false
    }
  }

  const clearChatHistory = async (chatId: string): Promise<boolean> => {
    const messageStore = useMessageStore()
    const success = await messageStore.clearChatHistory(chatId)
    if (success) {
      updateChatLocally(chatId, {
        lastMessage: 'Нет сообщений',
        unreadCount: 0,
        updatedAt: new Date().toISOString(),
      })
    }
    return success
  }

  // ==================== Локальные обновления (реактивные) ====================
  // Единый метод локального обновления — создаёт новый массив, триггеря реактивность
  const updateChatLocally = (chatId: string, updates: Partial<Chat>): void => {
    // Нормализация updatedAt: число -> ISO строка
    const normalizedUpdates = { ...updates }
    if (normalizedUpdates.updatedAt !== undefined) {
      const ts = normalizedUpdates.updatedAt
      if (typeof ts === 'number') {
        const ms = ts < 1e12 ? ts * 1000 : ts
        normalizedUpdates.updatedAt = new Date(ms).toISOString()
      }
    }

    const index = chats.value.findIndex((c) => String(c.id) === String(chatId))
    if (index === -1) return

    const updatedChat = { ...chats.value[index], ...normalizedUpdates } as Chat
    const shouldMoveToTop =
      normalizedUpdates.lastMessage !== undefined || normalizedUpdates.updatedAt !== undefined

    if (shouldMoveToTop) {
      chats.value = [updatedChat, ...chats.value.filter((c) => String(c.id) !== String(chatId))]
    } else {
      const newChats = [...chats.value]
      newChats[index] = updatedChat
      chats.value = newChats
    }

    if (String(currentChat.value?.id) === String(chatId)) {
      currentChat.value = updatedChat
    }
    console.log('Chat updated locally, id:', chatId)
  }

  // Обёртки для удобства
  const updateChatLastMessageLocally = (chatId: string, lastMessage: string): void => {
    updateChatLocally(chatId, { lastMessage })
  }

  const updateChatUnreadCountLocally = (chatId: string, unreadCount: number): void => {
    updateChatLocally(chatId, { unreadCount })
  }

  const updateChatUpdatedAtLocally = (chatId: string, updatedAt: string | Date | number): void => {
    let updatedAtStr: string
    if (typeof updatedAt === 'string') updatedAtStr = updatedAt
    else if (typeof updatedAt === 'number') {
      const ms = updatedAt < 1e12 ? updatedAt * 1000 : updatedAt
      updatedAtStr = new Date(ms).toISOString()
    } else updatedAtStr = updatedAt.toISOString()
    updateChatLocally(chatId, { updatedAt: updatedAtStr })
  }

  // Для обратной совместимости – использует updateChatLocally
  const updateLastMessageTime = (chatId: string, timeString: string): void => {
    updateChatLocally(chatId, { updatedAt: timeString })
  }

  const decrementChatUnreadCountLocally = (chatId: string): void => {
    const chat = getChatById(chatId)
    if (!chat) return
    const newCount = Math.max(0, (chat.unreadCount || 0) - 1)
    updateChatLocally(chatId, { unreadCount: newCount })
  }

  const clearChatUnreadCountLocally = (chatId: string): void => {
    updateChatLocally(chatId, { unreadCount: 0 })
  }

  const hasChat = (chatId: string): boolean => chats.value.some((c) => c.id === chatId)

  const clearPendingChats = (): void => {
    for (const [userId, data] of pendingChats.value.entries()) {
      const index = chats.value.findIndex((c) => c.id === data.chat.id)
      if (index !== -1) chats.value.splice(index, 1)
    }
    pendingChats.value.clear()
  }

  return {
    // Состояние
    chats,
    currentChat,
    loading,
    error,
    pendingChats,

    // Геттеры
    getUnreadCountByChatId,
    getLastMessageByChatId,
    allChats,
    sortedChats,
    directChats,
    getChatById,
    getDirectChatWithUser,
    unreadChatsCount,

    // Методы
    clearChatHistory,
    decrementChatUnreadCountLocally,
    clearChatUnreadCountLocally,
    updateChatLocally,
    updateChatLastMessageLocally,
    updateChatUpdatedAtLocally,
    updateChatUnreadCountLocally,
    updateLastMessageTime,
    hasChat,
    fetchChats,
    createLocalChat,
    createOrGetChat,
    addChat,
    confirmChat,
    rollbackChat,
    updateChat,
    deleteChat,
    clearPendingChats,
  }
})
