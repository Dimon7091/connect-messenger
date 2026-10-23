<template>
  <Teleport to="#modal-root">
    <!-- Основное меню -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div v-if="chat" class="absolute inset-0 z-50" @click.self="close">
        <div class="absolute inset-0 bg-black/20"></div>
        <div
          class="absolute top-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-lg border-b border-gray-200 dark:border-gray-700"
          @click.stop
        >
          <div
            class="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700"
          >
            <span class="text-lg font-semibold text-gray-800 dark:text-white"
              >Действия с чатом</span
            >
            <button
              @click="close"
              class="p-1 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              title="Закрыть"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-7 h-7 text-gray-500 dark:text-gray-400"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="flex flex-col py-2">
            <!-- Удалить чат (урна) -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="openConfirmDelete"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h-6 text-gray-700 dark:text-gray-300"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
                />
              </svg>
              <span>Удалить чат</span>
            </button>

            <!-- Очистить историю (веник) -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="openConfirmClearHistory"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" class="w-6 h-6">
                <rect width="256" height="256" fill="none" />
                <path
                  d="M112,224a95.2,95.2,0,0,1-29-48"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="16"
                />
                <path
                  d="M192,152c0,31.67,13.31,59,40,72H61A103.65,103.65,0,0,1,32,152c0-28.21,11.23-50.89,29.47-69.64a8,8,0,0,1,8.67-1.81L95.52,90.83a16,16,0,0,0,20.82-9l21-53.11c4.15-10,15.47-15.32,25.63-11.53a20,20,0,0,1,11.51,26.4L153.13,96.69a16,16,0,0,0,8.93,20.76L187,127.29a8,8,0,0,1,5,7.43Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="16"
                />
                <line
                  x1="43.93"
                  y1="105.57"
                  x2="192.8"
                  y2="165.12"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="16"
                />
              </svg>
              <span>Очистить историю</span>
            </button>

            <!-- Отключить звук (динамик) -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="handleMute"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h- text-gray-700 dark:text-gray-300"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M19.114 5.636a9 9 0 010 12.728M16.243 8.243a4.5 4.5 0 010 5.514M12 3l-4 4H5.5a1.5 1.5 0 00-1.5 1.5v5a1.5 1.5 0 001.5 1.5H8l4 4V3z"
                />
              </svg>
              <span>Отключить уведомления</span>
            </button>

            <!-- Закрепить чат (гвоздик) -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="handlePin"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h-6 text-gray-700 dark:text-gray-300"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z"
                />
              </svg>
              <span>Закрепить чат</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Диалог подтверждения для удаления чата -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="showConfirmDelete"
        class="fixed inset-0 z-60 flex items-center justify-center"
        @click.self="closeConfirmDelete"
      >
        <div class="absolute inset-0 bg-black/40"></div>
        <div
          class="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-sm w-full mx-4 p-6"
          @click.stop
        >
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Удалить чат?</h3>
          <p class="text-gray-600 dark:text-gray-300 mb-6">
            Вы уверены, что хотите удалить этот чат? Восстановить его будет невозможно.
          </p>
          <div class="flex justify-end gap-3">
            <button
              @click="closeConfirmDelete"
              class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
            >
              Отмена
            </button>
            <button
              @click="proceedDelete"
              class="px-4 py-2 rounded-md bg-red-600 text-white hover:bg-red-700 transition"
            >
              Удалить
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Диалог подтверждения для очистки истории -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="showConfirmClearHistory"
        class="fixed inset-0 z-60 flex items-center justify-center"
        @click.self="closeConfirmClearHistory"
      >
        <div class="absolute inset-0 bg-black/40"></div>
        <div
          class="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-sm w-full mx-4 p-6"
          @click.stop
        >
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Очистить историю?
          </h3>
          <p class="text-gray-600 dark:text-gray-300 mb-6">
            Все сообщения в этом чате будут удалены. Действие необратимо.
          </p>
          <div class="flex justify-end gap-3">
            <button
              @click="closeConfirmClearHistory"
              class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
            >
              Отмена
            </button>
            <button
              @click="proceedClearHistory"
              class="px-4 py-2 rounded-md bg-red-600 text-white hover:bg-red-700 transition"
            >
              Очистить
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Chat } from '@/types/index'
import { useChatStore } from '@/stores/chatStore'
import { useMessageStore } from '@/stores/messageStore'
import { useUserStore } from '@/stores/userStore'

const props = defineProps<{
  chat: Chat | null
}>()

const emit = defineEmits<{
  close: []
}>()

const chatStore = useChatStore()
const messageStore = useMessageStore()
const userStore = useUserStore()

// Состояния для диалогов подтверждения
const showConfirmDelete = ref(false)
const showConfirmClearHistory = ref(false)

const close = () => {
  emit('close')
}

// Открыть подтверждение удаления чата
const openConfirmDelete = () => {
  showConfirmDelete.value = true
}

const closeConfirmDelete = () => {
  showConfirmDelete.value = false
}

// Открыть подтверждение очистки истории
const openConfirmClearHistory = () => {
  showConfirmClearHistory.value = true
}

const closeConfirmClearHistory = () => {
  showConfirmClearHistory.value = false
}

// Реальное удаление чата
const proceedDelete = async () => {
  if (!props.chat) return
  const userId = userStore.user?.id
  if (!userId) return
  try {
    await chatStore.deleteChat(props.chat.id, userId.toString())
    messageStore.clearMessages(props.chat.id)
  } catch (error) {
    console.error('Ошибка удаления чата', error)
  }
  closeConfirmDelete()
  close()
}

// Реальная очистка истории
const proceedClearHistory = async () => {
  if (!props.chat) return
  try {
    await chatStore.clearChatHistory(props.chat.id)
    closeConfirmClearHistory()
    close()
  } catch (error) {
    console.error('Ошибка очистки истории:', error)
    // можно показать тост или оставить диалог открытым
  }
}

// Отключение уведомлений (без подтверждения)
const handleMute = () => {
  if (!props.chat) return
  console.log('Отключить уведомления для чата', props.chat.id)
  close()
}

// Закрепление чата (без подтверждения)
const handlePin = () => {
  if (!props.chat) return
  console.log('Закрепить чат', props.chat.id)
  close()
}
</script>
