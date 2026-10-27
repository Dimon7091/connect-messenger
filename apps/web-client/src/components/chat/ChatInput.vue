<template>
  <div
    class="chat-input p-3 bg-gray-50 dark:bg-gray-800 relative border-t border-gray-200 dark:border-gray-700"
  >
    <!-- Плашка ответа (reply) -->
    <div
      v-if="replyingToMessage"
      class="flex gap-2 reply-header bg-gray-100 dark:bg-gray-700 rounded-t-2xl px-3 py-2 mb-2 -mt-2 text-sm"
    >
      <span>
        <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="3"
            d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"
          />
        </svg>
      </span>
      <div class="flex-1 flex justify-between items-start min-w-0">
        <div class="flex-1 overflow-hidden">
          <div class="font-medium text-gray-700 dark:text-gray-300 truncate">
            Ответ для "{{ replySenderName }}"
          </div>
          <div class="text-gray-500 dark:text-gray-400 truncate text-xs mt-0.5">
            {{ replySnippet }}
          </div>
        </div>
        <button
          @click="cancelReply"
          class="shrink-0 ml-2 p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full transition"
          title="Отменить ответ"
        >
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>
    </div>

    <!-- Список выбранных файлов с превью -->
    <div v-if="selectedFiles.length" class="flex flex-col gap-2 mb-2 max-h-40 overflow-y-auto">
      <div
        v-for="(item, index) in selectedFiles"
        :key="index"
        class="flex items-center gap-3 bg-gray-100 dark:bg-gray-700 rounded-lg px-3 py-2"
      >
        <img
          v-if="item.preview"
          :src="item.preview"
          class="h-12 w-12 object-cover rounded"
          alt="Preview"
        />
        <span v-else-if="item.file.type.startsWith('image/')" class="text-2xl">🖼️</span>
        <span v-else-if="item.file.type.startsWith('video/')" class="text-2xl">🎬</span>
        <span v-else class="text-2xl">📎</span>

        <div class="flex-1 min-w-0">
          <div class="text-sm font-medium truncate">{{ item.file.name }}</div>
          <div class="text-xs text-gray-500">{{ formatFileSize(item.file.size) }}</div>
        </div>

        <button
          @click="removeFile(index)"
          class="p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full transition"
          title="Убрать файл"
        >
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>
    </div>

    <!-- ===== ОСНОВНАЯ СТРОКА ВВОДА (исправлено центрирование) ===== -->
    <div class="flex items-center gap-2 relative">
      <!-- Кнопка прикрепления (теперь единого размера и круглая) -->
      <button
        @click="toggleAttachmentMenu"
        :disabled="isUploading"
        class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-200 dark:hover:bg-gray-600 transition shrink-0"
        title="Прикрепить"
      >
        <span class="text-xl">📎</span>
      </button>

      <!-- Модальное окно выбора типа вложения -->
      <div
        v-if="showAttachmentMenu"
        class="absolute bottom-full left-0 mb-2 bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 p-2 z-50"
        style="min-width: 150px"
        @click.stop
      >
        <button
          @click="selectImage"
          class="flex items-center gap-3 w-full px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition"
        >
          <span class="text-2xl">
            <svg viewBox="0 0 24 24" width="30" height="30">
              <path
                d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-5.04-6.71l-2.75 3.54-1.96-2.36L6.5 17h11l-3.54-4.71zM15 9c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5-.67 1.5-1.5 1.5S15 9.83 15 9z"
                fill="currentColor"
              />
            </svg>
          </span>
          <span class="text-sm font-medium">Галерея</span>
        </button>
        <button
          @click="selectFile"
          class="flex items-center gap-3 w-full px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition"
        >
          <span class="text-2xl">
            <svg
              viewBox="0 0 24 24"
              width="30"
              height="30"
              fill="none"
              stroke="#4A5568"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="9" y1="11" x2="15" y2="11"></line>
              <line x1="9" y1="15" x2="15" y2="15"></line>
              <line x1="9" y1="19" x2="15" y2="19"></line>
            </svg>
          </span>

          <span class="text-sm font-medium">Файл</span>
        </button>
      </div>

      <!-- Блок textarea + кнопка отправки -->
      <div class="flex gap-2 grow items-center">
        <textarea
          ref="textareaRef"
          v-model="messageText"
          @keydown="onKeydown"
          @input="onInput"
          :placeholder="placeholder"
          :disabled="disabled"
          rows="1"
          class="flex-1 px-4 py-2 h-10 border border-gray-300 dark:border-gray-600 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white resize-none overflow-y-auto max-h-[200px] hide-scrollbar"
        ></textarea>
        <button
          @click="handleSend"
          :disabled="(!messageText.trim() && !selectedFiles.length) || isUploading"
          class="w-10 h-10 flex items-center justify-center rounded-full bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition shrink-0"
        >
          <svg
            class="w-5 h-5 transform rotate-90"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
            />
          </svg>
        </button>
      </div>
    </div>

    <!-- Прогресс загрузки файлов -->
    <div v-if="isUploading" class="mt-2 text-sm text-gray-600 dark:text-gray-400">
      Загрузка файлов: {{ uploadProgress }}%
      <progress :value="uploadProgress" max="100" class="w-full h-1"></progress>
    </div>

    <!-- Скрытые input'ы -->
    <input
      ref="imageInput"
      type="file"
      accept="image/*,video/*"
      multiple
      style="display: none"
      @change="handleImageSelected"
    />
    <input
      ref="fileInput"
      type="file"
      accept="*/*"
      multiple
      style="display: none"
      @change="handleFileSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, computed, onBeforeUnmount, onMounted } from 'vue'
import { useMessageStore } from '@/stores/messageStore'
import { useUserStore } from '@/stores/userStore'
import { s3UploadService } from '@/services/http/S3UploadService'
import type { Attachment } from '@/types'

const { disabled = false, placeholder = 'Введите сообщение...' } = defineProps<{
  disabled?: boolean
  placeholder?: string
}>()
// ===== Types =====
interface SelectedFile {
  file: File
  preview: string | null // object URL для изображений
}

// ===== Stores =====
const messageStore = useMessageStore()
const userStore = useUserStore()

// ===== Состояния =====
const replyingToMessage = computed(() => messageStore.replyingToMessage)
const messageText = ref('')
const textareaRef = ref<HTMLTextAreaElement>()
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const selectedFiles = ref<SelectedFile[]>([])
const isUploading = ref(false)
const uploadProgress = ref(0)
const showAttachmentMenu = ref(false)
let typingTimeout: ReturnType<typeof setTimeout> | null = null

// ===== Пропсы и эмиты =====
const emit = defineEmits<{
  (e: 'send', text: string, replyToId?: string, attachments?: Attachment[]): void
  (e: 'typing', isTyping: boolean): void
}>()

// ===== Вспомогательные геттеры =====
const replySenderName = computed(() => {
  const msg = replyingToMessage.value
  if (!msg) return 'пользователя'
  if (msg.replyContext?.senderName) return msg.replyContext.senderName
  const user = userStore.getUserFromCache(msg.senderId)
  if (user) {
    if (user.profile.firstName && user.profile.lastName)
      return `${user.profile.firstName} ${user.profile.lastName}`
    return user.profile.firstName || user.userName || 'пользователь'
  }
  return 'пользователь'
})

const replySnippet = computed(() => {
  const msg = replyingToMessage.value
  if (!msg) return ''
  let raw = msg.replyContext?.textSnippet || msg.text || ''
  const maxLen = 50
  if (raw.length > maxLen) raw = raw.substring(0, maxLen).trim() + '…'
  return raw
})

// ===== Форматирование размера =====
const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} Б`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`
  return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`
}

// ===== Управление меню =====
const toggleAttachmentMenu = () => {
  if (isUploading.value) return
  showAttachmentMenu.value = !showAttachmentMenu.value
}

const closeMenu = () => {
  showAttachmentMenu.value = false
}

// ===== Выбор типа вложения =====
const selectImage = () => {
  closeMenu()
  imageInput.value?.click()
}

const selectFile = () => {
  closeMenu()
  fileInput.value?.click()
}

// ===== Общая функция добавления файлов =====
const addFiles = (files: FileList) => {
  const newFiles: SelectedFile[] = []
  for (const file of Array.from(files)) {
    if (file.size > 50 * 1024 * 1024) {
      alert(`Файл "${file.name}" превышает 50 МБ`)
      continue
    }
    let preview: string | null = null
    if (file.type.startsWith('image/')) {
      preview = URL.createObjectURL(file)
    }
    newFiles.push({ file, preview })
  }
  selectedFiles.value = [...selectedFiles.value, ...newFiles]
}

// ===== Обработчики выбора =====
const handleImageSelected = (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return
  addFiles(files)
  input.value = ''
}

const handleFileSelected = (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return
  addFiles(files)
  input.value = ''
}

const removeFile = (index: number) => {
  const removed = selectedFiles.value[index]
  if (removed?.preview) URL.revokeObjectURL(removed.preview)
  selectedFiles.value.splice(index, 1)
}

// ===== Отправка сообщения =====
const handleSend = async () => {
  const text = messageText.value.trim()
  const hasFiles = selectedFiles.value.length > 0

  if (!text && !hasFiles) return
  if (isUploading.value) return

  const replyToId = replyingToMessage.value?.id || undefined
  let attachments: Attachment[] = []

  if (hasFiles) {
    isUploading.value = true
    uploadProgress.value = 0
    const total = selectedFiles.value.length
    let completed = 0

    try {
      const uploadPromises = selectedFiles.value.map(async (item) => {
        const result = await s3UploadService.uploadFile(item.file)
        completed++
        uploadProgress.value = Math.round((completed / total) * 100)
        return {
          fileKey: result.fileKey,
          mimeType: result.mimeType,
          fileName: result.fileName,
          size: parseInt(result.size, 10),
        }
      })
      attachments = await Promise.all(uploadPromises)
    } catch (error) {
      console.error('Ошибка загрузки файлов:', error)
      alert('Не удалось загрузить один или несколько файлов')
      isUploading.value = false
      uploadProgress.value = 0
      return
    } finally {
      isUploading.value = false
      uploadProgress.value = 0
    }
  }

  emit('send', text, replyToId, attachments)

  messageText.value = ''
  selectedFiles.value.forEach((item) => {
    if (item.preview) URL.revokeObjectURL(item.preview)
  })
  selectedFiles.value = []
  cancelReply()

  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  })
}

// ===== Вспомогательные методы =====
const cancelReply = () => {
  messageStore.setReplyingToMessage(null)
}

const onInput = () => {
  autoResize()
  onTyping()
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const autoResize = () => {
  if (!textareaRef.value) return
  const textarea = textareaRef.value
  textarea.style.height = 'auto'
  const newHeight = Math.min(textarea.scrollHeight, 200)
  textarea.style.height = `${newHeight}px`
}

const onTyping = () => {
  emit('typing', true)
  if (typingTimeout) clearTimeout(typingTimeout)
  typingTimeout = setTimeout(() => {
    emit('typing', false)
  }, 1000)
}

// ===== Закрытие меню при клике вне =====
const handleClickOutside = (e: MouseEvent) => {
  if (showAttachmentMenu.value) {
    const menuEl = document.querySelector('.chat-input .absolute.bottom-full')
    const btnEl = document.querySelector('.chat-input .w-10.h-10')
    if (
      menuEl &&
      !menuEl.contains(e.target as Node) &&
      btnEl &&
      !btnEl.contains(e.target as Node)
    ) {
      closeMenu()
    }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  if (typingTimeout) clearTimeout(typingTimeout)
  selectedFiles.value.forEach((item) => {
    if (item.preview) URL.revokeObjectURL(item.preview)
  })
  document.removeEventListener('click', handleClickOutside)
})

defineExpose({ focus: () => textareaRef.value?.focus() })
</script>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}
</style>
