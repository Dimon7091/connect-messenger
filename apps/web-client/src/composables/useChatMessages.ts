/// <reference types="node" />
import { ref, computed, watch, nextTick, type Ref, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useMessageStore } from '@/stores/messageStore'
import { useWebSocketStore } from '@/stores/webSocketStore'
import { useUserStore } from '@/stores/userStore'
import type { Message } from '@/types/index'

export function useChatMessages(
  chatIdGetter: () => string | undefined,
  messagesContainerRef: Ref<HTMLElement | undefined>,
) {
  const messageStore = useMessageStore()
  const wsStore = useWebSocketStore()
  const userStore = useUserStore()
  const { messages: messagesMap } = storeToRefs(messageStore)

  const isLoadingHistory = ref(false)
  const isLoadingMore = ref(false)
  let observer: IntersectionObserver | null = null
  const readPending = new Set<string>()
  let hasScrolledToFirstUnread = false
  const isUserAtBottom = ref(true)

  const messages = computed(() => {
    const chatId = chatIdGetter()
    if (!chatId) return []
    const map = messagesMap.value
    if (!map) return []
    const chatMessages = map[chatId]
    return chatMessages ? [...chatMessages] : []
  })

  const getContainer = () => messagesContainerRef.value

  const checkScrollPosition = () => {
    const container = getContainer()
    if (!container) return
    const { scrollTop, scrollHeight, clientHeight } = container
    isUserAtBottom.value = scrollHeight - scrollTop - clientHeight < 100
  }

  const scrollToBottom = () => {
    nextTick(() => {
      const container = getContainer()
      if (container) container.scrollTop = container.scrollHeight
    })
  }

  const scrollToFirstUnread = () => {
    if (hasScrolledToFirstUnread) return
    const id = chatIdGetter()
    if (!id) return
    const all = messages.value
    const currentUserId = userStore.user?.id?.toString() || ''
    const idx = all.findIndex((m) => m.senderId !== currentUserId && m.status !== 'READ')
    if (idx !== -1) {
      const targetId = all[idx]?.id
      const targetElement = document.querySelector(`.chat-message[data-message-id="${targetId}"]`)
      const container = getContainer()
      if (targetElement && container) {
        const elementRect = targetElement.getBoundingClientRect()
        const containerRect = container.getBoundingClientRect()
        const relativeTop = elementRect.top - containerRect.top
        container.scrollTop +=
          relativeTop + targetElement.clientHeight - container.clientHeight - 54
        hasScrolledToFirstUnread = true
      } else {
        scrollToBottom()
        hasScrolledToFirstUnread = true
      }
    } else {
      scrollToBottom()
      hasScrolledToFirstUnread = true
    }
  }

  const loadHistory = async () => {
    const id = chatIdGetter()
    if (!id || isLoadingHistory.value) return
    isLoadingHistory.value = true
    try {
      await messageStore.loadMessages(id, 50)
      await nextTick()
      scrollToFirstUnread()
      initReadObserver()
    } finally {
      isLoadingHistory.value = false
    }
  }

  const loadMore = async () => {
    const id = chatIdGetter()
    if (!id || isLoadingMore.value || !messageStore.hasMoreMessages(id)) return
    isLoadingMore.value = true
    const container = getContainer()
    const oldHeight = container?.scrollHeight || 0
    try {
      await messageStore.loadMoreMessages(id, 30)
      await nextTick()
      const newHeight = container?.scrollHeight || 0
      if (container) container.scrollTop = newHeight - oldHeight
    } finally {
      isLoadingMore.value = false
    }
  }

  let debounceTimer: NodeJS.Timeout | null = null
  const pendingUnreadIds = new Set<string>()

  const initReadObserver = () => {
    const id = chatIdGetter()
    if (!id) return
    if (observer) observer.disconnect()
    const elements = document.querySelectorAll('.chat-message[data-sender="other"]')
    if (!elements.length) return

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const messageId = entry.target.getAttribute('data-message-id')
            const status = entry.target.getAttribute('data-status')
            if (messageId && status !== 'READ' && !readPending.has(messageId)) {
              pendingUnreadIds.add(messageId)
            }
          }
        })

        if (pendingUnreadIds.size > 0) {
          if (debounceTimer) clearTimeout(debounceTimer)
          debounceTimer = setTimeout(() => {
            const chatId = id
            if (chatId && pendingUnreadIds.size) {
              pendingUnreadIds.forEach((msgId) => {
                wsStore.markAsRead(chatId, msgId)
                readPending.add(msgId)
              })
              pendingUnreadIds.clear()
            }
            debounceTimer = null
          }, 400)
        }
      },
      { threshold: 0.5, rootMargin: '100px 0px' },
    )

    elements.forEach((el) => observer?.observe(el))
  }

  const destroyObserver = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
    readPending.clear()
  }

  watch(
    () => chatIdGetter(),
    async (newId, oldId) => {
      if (newId && newId !== oldId) {
        destroyObserver()
        hasScrolledToFirstUnread = false
        await loadHistory()
      }
    },
    { immediate: true },
  )

  watch(
    messages,
    async () => {
      await nextTick()
      initReadObserver()
    },
    { deep: true },
  )

  const cleanup = () => destroyObserver()

  return {
    messages,
    isLoadingHistory,
    isLoadingMore,
    isUserAtBottom,
    scrollToBottom,
    loadMore,
    loadHistory,
    scrollToFirstUnread,
    checkScrollPosition,
    cleanup,
  }
}
