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
            <!-- Удалить чат -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="openConfirmDelete"
            >
              <span>Удалить чат</span>
            </button>

            <!-- Очистить историю -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="openConfirmClearHistory"
            >
              <span>Очистить историю</span>
            </button>

            <!-- Отключить звук -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="handleMute"
            >
              <span>Отключить уведомления</span>
            </button>

            <!-- Закрепить чат -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="handlePin"
            >
              <span>Закрепить чат</span>
            </button>

            <!-- Блокировка / разблокировка -->
            <button
              class="flex items-center gap-3 px-4 py-3 text-left text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
              @click="openConfirmBlockUser"
            >
              <span>{{ isBlocked ? 'Разблокировать' : 'Заблокировать' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Диалог подтверждения для удаления чата -->
    <ConfirmDialog
      v-model="showConfirmDelete"
      title="Удалить чат?"
      description="Вы уверены, что хотите удалить этот чат? Восстановить его будет невозможно."
      confirm-text="Удалить"
      confirm-button-class="bg-red-600 hover:bg-red-700"
      :loading="isDeleting"
      loading-text="Удаление..."
      @confirm="proceedDelete"
    />

    <!-- Диалог подтверждения для очистки истории -->
    <ConfirmDialog
      v-model="showConfirmClearHistory"
      title="Очистить историю?"
      description="Все сообщения в этом чате будут удалены. Действие необратимо."
      confirm-text="Очистить"
      confirm-button-class="bg-red-600 hover:bg-red-700"
      :loading="isClearing"
      loading-text="Очистка..."
      @confirm="proceedClearHistory"
    />

    <!-- Диалог подтверждения для блокировки/разблокировки -->
    <ConfirmDialog
      v-model="showConfirmBlockUser"
      :title="isBlocked ? 'Разблокировать пользователя?' : 'Заблокировать пользователя?'"
      :description="
        isBlocked
          ? 'Вы уверены, что хотите разблокировать этого пользователя? Он сможет снова отправлять вам сообщения.'
          : 'Вы уверены, что хотите заблокировать этого пользователя? Он не сможет отправлять вам сообщения.'
      "
      :confirm-text="isBlocked ? 'Разблокировать' : 'Заблокировать'"
      :confirm-button-class="
        isBlocked ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'
      "
      :loading="isBlocking"
      :loading-text="isBlocked ? 'Разблокировка...' : 'Блокировка...'"
      @confirm="proceedBlockUser"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Chat } from '@/types/index'
import { useChatStore } from '@/stores/chatStore'
import { useMessageStore } from '@/stores/messageStore'
import { useUserStore } from '@/stores/userStore'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

const props = defineProps<{
  chat: Chat | null
  companionId: string
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
const showConfirmBlockUser = ref(false)

// Состояния загрузки для каждого действия
const isDeleting = ref(false)
const isClearing = ref(false)
const isBlocking = ref(false)

// Вычисляемое свойство – заблокирован ли пользователь
const isBlocked = computed(() => {
  const blackList = userStore.user?.blackListIds || []
  return blackList.some((id) => String(id) === String(props.companionId))
})

let closing = false
const close = () => {
  if (closing) return
  closing = true
  emit('close')
  setTimeout(() => {
    closing = false
  }, 100)
}

// Открытие диалогов
const openConfirmDelete = () => {
  showConfirmDelete.value = true
}
const openConfirmClearHistory = () => {
  showConfirmClearHistory.value = true
}
const openConfirmBlockUser = () => {
  showConfirmBlockUser.value = true
}

// Действия
const proceedDelete = async () => {
  if (!props.chat) return
  const userId = userStore.user?.id
  if (!userId) return
  isDeleting.value = true
  try {
    await chatStore.deleteChat(props.chat.id, userId.toString())
    messageStore.clearMessages(props.chat.id)
    showConfirmDelete.value = false
    close()
  } catch (error) {
    console.error('Ошибка удаления чата', error)
    // диалог остаётся открытым при ошибке
  } finally {
    isDeleting.value = false
  }
}

const proceedClearHistory = async () => {
  if (!props.chat) return
  isClearing.value = true
  try {
    await chatStore.clearChatHistory(props.chat.id)
    showConfirmClearHistory.value = false
    close()
  } catch (error) {
    console.error('Ошибка очистки истории:', error)
  } finally {
    isClearing.value = false
  }
}

const proceedBlockUser = async () => {
  if (!props.chat) return
  isBlocking.value = true
  try {
    if (isBlocked.value) {
      await userStore.removeFromBlackList(props.companionId)
    } else {
      await userStore.addToBlackList(props.companionId)
    }
    showConfirmBlockUser.value = false
    close()
  } catch (error) {
    console.error('Ошибка операции с чёрным списком:', error)
    alert('Не удалось выполнить операцию. Попробуйте позже.')
  } finally {
    isBlocking.value = false
  }
}

const handleMute = () => {
  if (!props.chat) return
  console.log('Отключить уведомления для чата', props.chat.id)
  close()
}

const handlePin = () => {
  if (!props.chat) return
  console.log('Закрепить чат', props.chat.id)
  close()
}
</script>
