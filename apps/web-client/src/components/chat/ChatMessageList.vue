<template>
  <main
    ref="container"
    :style="{ backgroundImage: `url(${chatBg})` }"
    class="flex-1 overflow-y-auto bg-no-repeat bg-center bg-cover dark:bg-gray-900 pb-6"
    @scroll="handleScroll"
  >
    <!-- Заглушка, если сообщений нет и загрузка закончена -->
    <div
      v-if="!isLoadingHistory && messages.length === 0"
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
            d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
          />
        </svg>
      </div>
      <p class="text-gray-500 dark:text-gray-400 font-medium text-xl">Нет сообщений</p>
      <p class="text-gray-400 dark:text-gray-500 mt-1">Начните новый диалог</p>
    </div>

    <template v-for="group in groupedMessages" :key="group.dateKey">
      <!-- Разделитель даты -->
      <div class="flex justify-center my-4">
        <div
          class="bg-gray-200 dark:bg-gray-700 rounded-full px-4 py-1 text-gray-700 dark:text-gray-200 shadow-sm"
        >
          {{ group.dateLabel }}
        </div>
      </div>
      <!-- Сообщения группы -->
      <TransitionGroup name="message" tag="div" class="space-y-1">
        <ChatMessage
          v-for="message in group.messages"
          :key="message.id"
          :message="message"
          :currentUserId="currentUserId"
          :isSelectionMode="selectionMode"
          :isSelected="selectedIds.has(message.id)"
          @select="toggleSelection"
          @longPress="enableSelectionMode"
        />
      </TransitionGroup>
    </template>
    <div v-if="isLoadingMore" class="flex justify-center py-2">
      <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-gray-500"></div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Message } from '@/types/index'
import ChatMessage from './ChatMessage.vue'
import chatBg from '@/assets/images/img2.akspic.ru-minimalizm-atmosfera-mir-priroda-lazurnyj-1080x1920.jpg'

const props = defineProps<{
  messages: Message[]
  currentUserId: string
  isLoadingHistory?: boolean
  isLoadingMore?: boolean
}>()

const emit = defineEmits<{
  (e: 'loadMore'): void
  (e: 'scroll', event: Event): void
  (e: 'selectionChange', selectedIds: Set<string>): void
  (e: 'deleteMessages', ids: string[]): void
}>()

const container = ref<HTMLElement>()
const selectionMode = ref(false)
const selectedIds = ref(new Set<string>())

// Универсальный парсер даты сообщения
const parseMessageDate = (dateValue: unknown): Date | null => {
  if (!dateValue) return null
  if (dateValue instanceof Date) return dateValue
  if (typeof dateValue === 'number') {
    // Если timestamp в секундах (меньше 10^10) – умножаем на 1000
    const timestamp = dateValue < 10000000000 ? dateValue * 1000 : dateValue
    const d = new Date(timestamp)
    return isNaN(d.getTime()) ? null : d
  }
  if (typeof dateValue === 'string') {
    const d = new Date(dateValue)
    return isNaN(d.getTime()) ? null : d
  }
  return null
}

// Форматирование заголовка даты (как в Telegram)
const formatDateLabel = (date: Date): string => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  if (messageDate.getTime() === today.getTime()) {
    return 'Сегодня'
  } else if (messageDate.getTime() === yesterday.getTime()) {
    return 'Вчера'
  } else {
    // Проверяем разницу в днях больше года (365 дней)
    const msPerDay = 1000 * 60 * 60 * 24
    const diffDays = Math.floor((now.getTime() - date.getTime()) / msPerDay)
    const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'long' }
    if (diffDays > 365) {
      options.year = 'numeric'
    }
    return date.toLocaleDateString('ru-RU', options)
  }
}

// Группировка сообщений по датам
const groupedMessages = computed(() => {
  const groups: { dateKey: string; dateLabel: string; messages: Message[] }[] = []
  for (const message of props.messages) {
    const messageDate = parseMessageDate(message.createdAt)
    if (!messageDate) continue // пропускаем некорректные даты
    const dateKey = messageDate.toDateString()
    const dateLabel = formatDateLabel(messageDate)
    const lastGroup = groups[groups.length - 1]
    if (lastGroup && lastGroup.dateKey === dateKey) {
      lastGroup.messages.push(message)
    } else {
      groups.push({ dateKey, dateLabel, messages: [message] })
    }
  }
  return groups
})

// Остальная логика (выделение, удаление, скролл) без изменений
const enableSelectionMode = (messageId: string) => {
  if (!selectionMode.value) {
    selectionMode.value = true
    selectedIds.value.clear()
    selectedIds.value.add(messageId)
    emit('selectionChange', selectedIds.value)
  }
}

const toggleSelection = (messageId: string) => {
  if (selectionMode.value) {
    if (selectedIds.value.has(messageId)) {
      selectedIds.value.delete(messageId)
    } else {
      selectedIds.value.add(messageId)
    }
    emit('selectionChange', selectedIds.value)
    if (selectedIds.value.size === 0) {
      selectionMode.value = false
    }
  }
}

const resetSelection = () => {
  selectionMode.value = false
  selectedIds.value.clear()
  emit('selectionChange', selectedIds.value)
}

const deleteSelected = () => {
  if (selectedIds.value.size === 0) return
  emit('deleteMessages', Array.from(selectedIds.value))
  resetSelection()
}

const handleScroll = (event: Event) => {
  emit('scroll', event)
  const target = event.target as HTMLElement
  if (target.scrollTop === 0 && !props.isLoadingMore) emit('loadMore')
}

defineExpose({ container, resetSelection, deleteSelected, selectionMode, selectedIds })
</script>

<style scoped>
.message-enter-active,
.message-leave-active {
  transition: all 0.5s ease;
}
.message-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.message-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
.message-move {
  transition: transform 0.3s ease;
}
</style>
