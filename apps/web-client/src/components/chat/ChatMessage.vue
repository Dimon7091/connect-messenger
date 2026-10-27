<template>
  <div
    class="flex w-full chat-message relative px-3 py-0.5 transition-colors duration-900"
    :class="[
      message.senderId === currentUserId ? 'justify-end' : 'justify-start',
      isSelected || (message.senderId !== currentUserId && message.status !== 'READ')
        ? 'bg-blue-100/40 dark:bg-blue-900/50'
        : '',
    ]"
    :data-message-id="message.id"
    :data-sender="message.senderId === currentUserId ? 'self' : 'other'"
    :data-status="message.status"
    @contextmenu.prevent="onLongPress"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
    @click="onClick"
  >
    <!-- Чекбокс -->
    <div
      v-if="isSelectionMode"
      class="absolute left-0 top-1/2 -translate-y-1/2 z-10 ml-2"
      @click.stop
    >
      <input
        type="checkbox"
        :checked="isSelected"
        @change="$emit('select', message.id)"
        class="w-5 h-5 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
      />
    </div>

    <!-- Блок сообщения -->
    <div
      :class="[
        'relative max-w-[85%] min-w-0 shadow-[0_0_2px_rgba(0,0,0,0.3)] dark:shadow-gray-500 px-3 py-2 transition-all',
        message.senderId === currentUserId
          ? 'bg-[#cae5fc] dark:bg-[#53299b] dark:text-white own-message rounded-t-2xl rounded-l-2xl shadow-2xl text-[#000000]'
          : 'bg-white dark:bg-gray-800 text-gray-900 dark:text-white other-message rounded-t-2xl rounded-r-2xl',
        isSelectionMode ? 'ml-7' : '',
      ]"
    >
      <!-- Блок цитаты (reply) -->
      <div
        v-if="message.replyContext"
        :class="[
          'border-l-4 rounded-md px-2 py-1 mb-1 text-sl cursor-pointer transition',
          message.senderId === currentUserId
            ? 'border-blue-300 bg-gray-100 dark:bg-gray-900/40 hover:bg-blue-200 dark:hover:bg-blue-800/60'
            : 'border-blue-400 bg-gray-100 dark:bg-gray-700/60 hover:bg-gray-200 dark:hover:bg-gray-700',
        ]"
        @click.stop="scrollToOriginalMessage"
      >
        <div
          :class="[
            'font-semibold',
            message.senderId === currentUserId
              ? 'text-blue-700 dark:text-blue-300'
              : 'text-blue-600 dark:text-blue-400',
          ]"
        >
          {{ message.replyContext.senderName }}
        </div>
        <div class="truncate text-gray-700 dark:text-gray-300">
          {{ message.replyContext.textSnippet }}
        </div>
      </div>

      <!-- ===== КОЛЛАЖ ИЗОБРАЖЕНИЙ ===== -->
      <div v-if="imageAttachments.length" class="grid grid-cols-2 gap-1 mb-2">
        <div
          v-for="(att, idx) in imageAttachments"
          :key="att.fileKey"
          :class="[
            imageAttachments.length === 1 ? 'col-span-2' : 'col-span-1',
            imageAttachments.length === 3 && idx === 2 ? 'col-span-2' : '',
          ]"
          class="min-w-35 min-h-35 relative aspect-square overflow-hidden rounded-lg bg-gray-200 dark:bg-gray-700"
        >
          <!-- Скелетон -->
          <div
            v-if="attachmentLoading[att.fileKey]"
            class="absolute inset-0 flex flex-col items-center justify-center bg-gray-200 dark:bg-gray-700 animate-pulse"
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
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <span class="mt-1 text-xs text-gray-500 dark:text-gray-400">Загрузка...</span>
          </div>

          <!-- Ошибка -->
          <div
            v-else-if="attachmentError[att.fileKey]"
            class="absolute inset-0 flex flex-col items-center justify-center bg-gray-100 dark:bg-gray-800 cursor-pointer"
            @click="retryLoad(att.fileKey)"
          >
            <svg
              class="w-10 h-10 text-red-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <span class="text-xs text-red-500 mt-1">Ошибка</span>
            <svg
              class="w-6 h-6 text-blue-500 mt-1 hover:scale-110 transition-transform"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
            <span class="text-xs text-blue-500">Повторить</span>
          </div>

          <!-- Изображение -->
          <img
            v-else-if="mediaStore.urlCache[att.fileKey]?.url"
            :src="mediaStore.urlCache[att.fileKey]?.url"
            :alt="att.fileName"
            class="w-full h-full object-cover cursor-pointer"
            @click="openImage(att.fileKey)"
            @error="handleImageError(att.fileKey)"
          />

          <div v-else class="absolute inset-0 bg-gray-200 dark:bg-gray-700 animate-pulse"></div>
        </div>
      </div>

      <!-- ===== ОСТАЛЬНЫЕ ВЛОЖЕНИЯ ===== -->
      <div v-if="otherAttachments.length" class="flex flex-col gap-2 mb-2">
        <div v-for="attachment in otherAttachments" :key="attachment.fileKey" class="w-full">
          <!-- Видео -->
          <div v-if="attachment.mimeType.startsWith('video/')">
            <div
              v-if="attachmentLoading[attachment.fileKey]"
              class="flex flex-col items-center justify-center h-40 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"
            >
              <svg
                class="w-12 h-12 text-gray-400 dark:text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"
                />
              </svg>
              <span class="mt-1 text-xs text-gray-500 dark:text-gray-400">Загрузка видео...</span>
            </div>
            <div
              v-else-if="attachmentError[attachment.fileKey]"
              class="flex flex-col items-center justify-center h-40 bg-gray-100 dark:bg-gray-800 rounded-lg cursor-pointer"
              @click="retryLoad(attachment.fileKey)"
            >
              <svg
                class="w-10 h-10 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span class="text-xs text-red-500 mt-1">Ошибка</span>
              <svg
                class="w-6 h-6 text-blue-500 mt-1 hover:scale-110 transition-transform"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              <span class="text-xs text-blue-500">Повторить</span>
            </div>
            <video
              v-else-if="mediaStore.urlCache[attachment.fileKey]?.url"
              controls
              class="max-w-full max-h-90 rounded-lg"
              :key="mediaStore.urlCache[attachment.fileKey]?.url"
            >
              <source
                :src="mediaStore.urlCache[attachment.fileKey]?.url"
                :type="attachment.mimeType"
              />
            </video>
            <div
              v-else
              class="flex flex-col items-center justify-center h-40 bg-gray-200 dark:bg-gray-700 rounded-lg animate-pulse"
            >
              <svg
                class="w-12 h-12 text-gray-400 dark:text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"
                />
              </svg>
              <span class="mt-1 text-xs text-gray-500 dark:text-gray-400">Видео</span>
            </div>
          </div>

          <!-- Документ -->
          <div v-else>
            <!-- Скелетон -->
            <div
              v-if="attachmentLoading[attachment.fileKey]"
              class="flex items-center gap-3 p-3 h-16 bg-gray-100 dark:bg-gray-700 rounded-lg animate-pulse"
            >
              <svg
                class="w-8 h-8 text-gray-400 dark:text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                />
              </svg>
              <div class="flex-1 min-w-0">
                <div class="h-4 w-3/4 bg-gray-300 dark:bg-gray-600 rounded"></div>
                <div class="h-3 w-1/4 bg-gray-300 dark:bg-gray-600 rounded mt-1"></div>
              </div>
            </div>
            <!-- Ошибка -->
            <div
              v-else-if="attachmentError[attachment.fileKey]"
              class="flex items-center gap-3 p-3 h-16 bg-gray-100 dark:bg-gray-700 rounded-lg cursor-pointer"
              @click="retryLoad(attachment.fileKey)"
            >
              <svg
                class="w-8 h-8 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span class="text-red-500 text-sm">Ошибка</span>
              <svg
                class="w-5 h-5 text-blue-500 ml-auto hover:scale-110 transition-transform"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              <span class="text-blue-500 text-sm">Повторить</span>
            </div>
            <!-- Кликабельный блок документа -->
            <div
              v-else-if="mediaStore.urlCache[attachment.fileKey]?.url"
              class="flex items-center gap-3 p-3 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition cursor-pointer"
              @click="openFileOptions(attachment)"
            >
              <svg
                class="w-8 h-8 text-gray-500 dark:text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                />
              </svg>
              <div class="flex-1 min-w-0">
                <div class="font-medium truncate">{{ attachment.fileName }}</div>
                <div class="text-xs text-gray-500">{{ formatSize(attachment.size) }}</div>
              </div>
              <svg
                class="w-5 h-5 text-gray-400 flex-shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </div>
            <!-- Заглушка -->
            <div
              v-else
              class="flex items-center gap-3 p-3 h-16 bg-gray-100 dark:bg-gray-700 rounded-lg animate-pulse"
            >
              <svg
                class="w-8 h-8 text-gray-400 dark:text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"
                />
              </svg>
              <div class="flex-1 min-w-0">
                <div class="h-4 w-3/4 bg-gray-300 dark:bg-gray-600 rounded"></div>
                <div class="h-3 w-1/4 bg-gray-300 dark:bg-gray-600 rounded mt-1"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Текст -->
      <p v-if="message.text" class="text-base break-words pr-2">{{ message.text }}</p>

      <!-- Время и статусы -->
      <div
        :class="[
          'flex items-center mt-1',
          message.senderId === currentUserId ? 'justify-end' : 'justify-start',
        ]"
      >
        <span class="text-xs mr-1 opacity-80">{{ formattedTime }}</span>
        <template v-if="message.senderId === currentUserId">
          <span
            v-if="['SENT', 'DELIVERED', 'READ'].includes(message.status)"
            class="text-xs text-green-600"
            >✓</span
          >
          <span v-if="['DELIVERED', 'READ'].includes(message.status)" class="text-xs text-green-600"
            >✓</span
          >
          <span v-if="message.status === 'READ'" class="text-xs ml-1">👁️</span>
        </template>
      </div>
    </div>

    <!-- ===== МОДАЛЬНОЕ ОКНО ДЛЯ ФАЙЛА (teleport внутри корневого элемента) ===== -->
    <Teleport to="body">
      <div
        v-if="fileModalShow"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        @click.self="closeFileModal"
      >
        <div
          class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl p-6 max-w-sm w-full mx-4 transform transition-all"
        >
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Действие с файлом</h3>
            <button
              @click="closeFileModal"
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>

          <div class="mb-4 text-sm text-gray-600 dark:text-gray-300 break-all">
            <span class="font-medium">{{ selectedFile?.fileName }}</span>
            <span class="ml-2 text-gray-400">({{ formatSize(selectedFile?.size) }})</span>
          </div>

          <!-- Сообщение об ошибке скачивания -->
          <div
            v-if="downloadError"
            class="mb-3 p-2 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm rounded-lg"
          >
            {{ downloadError }}
          </div>

          <div class="flex flex-col gap-3">
            <button
              @click="openFile"
              class="flex items-center justify-center gap-3 px-4 py-3 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition font-medium"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                />
              </svg>
              Открыть
            </button>

            <button
              @click="downloadFile"
              :disabled="isDownloading"
              class="flex items-center justify-center gap-3 px-4 py-3 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed text-gray-800 dark:text-white rounded-lg transition font-medium"
            >
              <svg
                v-if="isDownloading"
                class="w-5 h-5 animate-spin"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                />
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                />
              </svg>
              {{ isDownloading ? 'Скачивание...' : 'Скачать' }}
            </button>
          </div>

          <button
            @click="closeFileModal"
            class="mt-3 w-full text-center text-sm text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition"
          >
            Отмена
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useMediaStore } from '@/stores/mediaStore'
import { useGalleryStore } from '@/stores/galleryStore'
import type { Message, Attachment } from '@/types/index'

const props = defineProps<{
  message: Message
  currentUserId: string
  isSelectionMode: boolean
  isSelected: boolean
}>()

const emit = defineEmits<{
  (e: 'select', messageId: string): void
  (e: 'longPress', messageId: string): void
  (e: 'scrollToMessage', messageId: string): void
}>()

const mediaStore = useMediaStore()
const galleryStore = useGalleryStore()

// ===== Локальные состояния =====
const attachmentLoading = ref<Record<string, boolean>>({})
const attachmentError = ref<Record<string, boolean>>({})

// ===== Модальное окно =====
const fileModalShow = ref(false)
const selectedFile = ref<Attachment | null>(null)
const isDownloading = ref(false)
const downloadError = ref<string | null>(null) // 👈 ошибка скачивания

// ===== Форматирование размера =====
const formatSize = (size?: string | number) => {
  if (!size) return ''
  const bytes = typeof size === 'string' ? parseInt(size, 10) : size
  if (bytes < 1024) return `${bytes} Б`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`
  return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`
}

// ===== Загрузка URL =====
const loadUrl = async (fileKey: string) => {
  if (mediaStore.urlCache[fileKey]?.url) return
  if (attachmentLoading.value[fileKey]) return

  attachmentLoading.value[fileKey] = true
  attachmentError.value[fileKey] = false

  try {
    await mediaStore.getDownloadUrl(fileKey)
    const url = mediaStore.urlCache[fileKey]?.url
    if (url) {
      const resp = await fetch(url, { method: 'HEAD' })
      if (!resp.ok) {
        const { [fileKey]: _, ...rest } = mediaStore.urlCache
        mediaStore.urlCache = rest
        throw new Error(`HTTP ${resp.status}`)
      }
    }
  } catch (error) {
    console.error(`Failed to load URL for ${fileKey}:`, error)
    attachmentError.value[fileKey] = true
  } finally {
    attachmentLoading.value[fileKey] = false
  }
}

// ===== Обработчики ошибок =====
const handleImageError = (fileKey: string) => {
  attachmentError.value[fileKey] = true
  const { [fileKey]: _, ...rest } = mediaStore.urlCache
  mediaStore.urlCache = rest
}

const retryLoad = (fileKey: string) => {
  attachmentError.value[fileKey] = false
  loadUrl(fileKey)
}

// ===== Функции для файлового модального окна =====
const openFileOptions = (attachment: Attachment) => {
  if (!mediaStore.urlCache[attachment.fileKey]?.url) return
  selectedFile.value = attachment
  fileModalShow.value = true
  downloadError.value = null // сбрасываем ошибку при открытии
}

const closeFileModal = () => {
  fileModalShow.value = false
  selectedFile.value = null
  isDownloading.value = false
  downloadError.value = null
}

const openFile = () => {
  if (!selectedFile.value) return
  const url = mediaStore.urlCache[selectedFile.value.fileKey]?.url
  if (url) {
    window.open(url, '_blank')
  }
  closeFileModal()
}

// ===== Улучшенная функция скачивания =====
const downloadFile = async () => {
  if (!selectedFile.value) return
  const fileKey = selectedFile.value.fileKey

  // 1. Если URL нет в кэше — пытаемся загрузить
  let url = mediaStore.urlCache[fileKey]?.url
  if (!url) {
    downloadError.value = 'Ссылка устарела, обновляем...'
    await loadUrl(fileKey)
    url = mediaStore.urlCache[fileKey]?.url
    if (!url) {
      downloadError.value = 'Не удалось получить ссылку на файл. Попробуйте позже.'
      return
    }
  }

  isDownloading.value = true
  downloadError.value = null

  try {
    // 2. Пробуем скачать файл через fetch
    const response = await fetch(url, { mode: 'cors' })
    if (!response.ok) {
      // Если ошибка 403 или 401 — возможно, ссылка истекла
      if (response.status === 403 || response.status === 401) {
        downloadError.value = 'Ссылка истекла, обновляем...'
        // Принудительно сбрасываем кэш и загружаем заново
        const { [fileKey]: _, ...rest } = mediaStore.urlCache
        mediaStore.urlCache = rest
        await loadUrl(fileKey)
        const newUrl = mediaStore.urlCache[fileKey]?.url
        if (!newUrl) {
          downloadError.value = 'Не удалось обновить ссылку. Попробуйте позже.'
          return
        }
        // Повторяем запрос с новой ссылкой
        const retryResponse = await fetch(newUrl, { mode: 'cors' })
        if (!retryResponse.ok) {
          throw new Error(`HTTP ${retryResponse.status}`)
        }
        const blob = await retryResponse.blob()
        const blobUrl = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = blobUrl
        link.download = selectedFile.value.fileName || 'file'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(blobUrl)
        closeFileModal()
        return
      } else {
        throw new Error(`HTTP ${response.status}`)
      }
    }

    // 3. Успешный ответ
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = selectedFile.value.fileName || 'file'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(blobUrl)

    // 4. Закрываем модалку после успешного скачивания
    closeFileModal()
  } catch (error: any) {
    console.error('Download failed:', error)
    downloadError.value = `Ошибка: ${error.message || 'Неизвестная ошибка'}. Попробуйте открыть файл в новой вкладке.`
  } finally {
    isDownloading.value = false
  }
}

// ===== ГАЛЕРЕЯ =====
const imageAttachments = computed(() =>
  (props.message.attachments || []).filter((att: Attachment) => att.mimeType?.startsWith('image/')),
)
const otherAttachments = computed(() =>
  (props.message.attachments || []).filter(
    (att: Attachment) => !att.mimeType?.startsWith('image/'),
  ),
)

const openImage = (fileKey: string) => {
  if (!imageAttachments.value.length) return
  const index = imageAttachments.value.findIndex((att: Attachment) => att.fileKey === fileKey)
  if (index === -1) return
  const images = imageAttachments.value.map((att: Attachment) => ({
    url: mediaStore.urlCache[att.fileKey]?.url || '',
    fileKey: att.fileKey,
    fileName: att.fileName || 'Изображение',
  }))
  galleryStore.openGallery(images, index)
}

// ===== Прокрутка к оригинальному сообщению =====
const scrollToOriginalMessage = () => {
  const originalId = props.message.replyContext?.originalMessageId
  if (originalId) emit('scrollToMessage', originalId)
}

// ===== Следим за attachments =====
watch(
  () => props.message.attachments,
  (newAttachments) => {
    if (newAttachments?.length) {
      newAttachments.forEach((att: Attachment) => {
        if (att.fileKey) loadUrl(att.fileKey)
      })
    }
  },
  { immediate: true, deep: true },
)

// ===== Форматирование времени =====
const formattedTime = computed(() => {
  const value = props.message.createdAt
  if (!value) return ''

  let date: Date | null = null

  if (typeof value === 'string') {
    const d = new Date(value)
    if (!isNaN(d.getTime())) date = d
  } else if (typeof value === 'number') {
    const ms = value < 1e12 ? value * 1000 : value
    const d = new Date(ms)
    if (!isNaN(d.getTime())) date = d
  } else if (value && typeof value === 'object' && 'getTime' in value) {
    // Для объектов, похожих на Date (включая Date)
    const d = value as Date
    if (!isNaN(d.getTime())) date = d
  }

  if (!date) return ''
  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
})

// ===== Обработчики касаний =====
const touchStartPos = ref<{ x: number; y: number } | null>(null)
let touchTimer: ReturnType<typeof setTimeout> | null = null

const onTouchStart = (event: TouchEvent) => {
  if (props.isSelectionMode) return
  const touch = event.touches[0]
  if (!touch) return
  touchStartPos.value = { x: touch.clientX, y: touch.clientY }
  touchTimer = setTimeout(() => {
    emit('longPress', props.message.id)
    touchTimer = null
  }, 500)
}

const onTouchMove = (event: TouchEvent) => {
  if (!touchStartPos.value) return
  const touch = event.touches[0]
  if (!touch) return
  const dx = touch.clientX - touchStartPos.value.x
  const dy = touch.clientY - touchStartPos.value.y
  const distance = Math.sqrt(dx * dx + dy * dy)
  if (distance > 10) {
    if (touchTimer) {
      clearTimeout(touchTimer)
      touchTimer = null
    }
    touchStartPos.value = null
  }
}

const onTouchEnd = () => {
  if (touchTimer) {
    clearTimeout(touchTimer)
    touchTimer = null
  }
  touchStartPos.value = null
}

const onLongPress = (event: MouseEvent) => {
  event.preventDefault()
  if (props.isSelectionMode) return
  emit('longPress', props.message.id)
}

const onClick = () => {
  if (props.isSelectionMode) {
    emit('select', props.message.id)
  }
}

onBeforeUnmount(() => {
  if (touchTimer) {
    clearTimeout(touchTimer)
    touchTimer = null
  }
})
</script>

<style scoped>
/* Хвостики – без изменений */
.own-message::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: -7px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-left-color: #c5e3fc;
  border-right: 0;
  border-bottom: 0;
  filter: drop-shadow(0 0px 3px rgba(0, 0, 0, 0.3)) drop-shadow(0 0px 15px rgba(0, 0, 0, 0.3));
  clip-path: inset(-50px -50px -50px 0);
}
.other-message::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: -7px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-right-color: white;
  border-left: 0;
  border-bottom: 0;
  filter: drop-shadow(0 0px 3px rgba(0, 0, 0, 0.3)) drop-shadow(0 0px 3px rgba(0, 0, 0, 0.3));
  clip-path: inset(-50px 0 -50px -50px);
}
.dark .other-message::after {
  border-right-color: #1f2937;
}
.dark .own-message::after {
  border-left-color: #4b1c9c;
}
</style>

<style>
/* Тени для хвостиков в тёмной теме */
.dark .own-message::after,
.dark .other-message::after {
  filter: drop-shadow(0 0px 3px rgba(255, 255, 255, 0.15))
    drop-shadow(0 0px 15px rgba(255, 255, 255, 0.15));
}
</style>
