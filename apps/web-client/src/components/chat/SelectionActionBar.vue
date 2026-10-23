<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 -translate-y-8"
    enter-to-class="opacity-100 translate-y-0"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="opacity-100 translate-y-0"
    leave-to-class="opacity-0 -translate-y-8"
  >
    <div
      v-if="selectionMode"
      class="absolute top-0 left-0 right-0 backdrop-blur-md bg-white/90 dark:bg-gray-900/90 shadow-lg p-4 flex justify-between items-center border-b border-gray-200/50 dark:border-gray-700/50 z-20"
    >
      <!-- Левая группа: закрытие и счётчик -->
      <div class="flex items-center gap-2">
        <button
          @click="emit('reset')"
          class="p-2 rounded-full hover:bg-gray-200/80 dark:hover:bg-gray-700/80 transition-all duration-200 active:scale-95"
          title="Отменить выделение"
        >
          <svg
            class="w-6 h-6 text-gray-700 dark:text-gray-300"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
        <span class="text-gray-800 dark:text-white font-semibold text-sm">
          Выбрано: {{ selectedCount }}
        </span>
      </div>

      <!-- Правая группа: кнопки "Ответить" и "Удалить" -->
      <div class="flex items-center gap-2">
        <!-- Кнопка "Ответить" (анимированная, появляется только при выборе одного сообщения) -->
        <Transition
          mode="out-in"
          enter-active-class="transition-all duration-200 ease-out"
          enter-from-class="opacity-0 scale-90"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition-all duration-150 ease-in"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-90"
        >
          <button
            v-if="selectedCount === 1"
            key="reply"
            @click="emit('reply')"
            class="flex items-center gap-2 px-3 py-2 rounded-full bg-gradient-to-r from-blue-500 to-indigo-500 text-white font-medium shadow-md hover:shadow-lg transform transition-all duration-200 hover:scale-105 active:scale-95"
            title="Ответить"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"
              />
            </svg>
          </button>
        </Transition>
        <!-- Кнопка удаления -->
        <button
          @click="openConfirmDelete"
          class="flex items-center gap-2 px-3 py-2 rounded-full bg-gradient-to-r from-red-500 to-rose-500 text-white font-medium shadow-md hover:shadow-lg transform transition-all duration-200 hover:scale-105 active:scale-95"
          title="Удалить выбранные"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
        </button>
      </div>
    </div>
  </Transition>

  <!-- Диалог подтверждения удаления сообщений -->
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
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Удалить сообщения?</h3>
        <p class="text-gray-600 dark:text-gray-300 mb-6">
          Вы уверены, что хотите удалить выбранные сообщения ({{ selectedCount }})? Это действие
          необратимо.
        </p>
        <div class="flex justify-end gap-3">
          <button
            @click="closeConfirmDelete"
            class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
          >
            Отмена
          </button>
          <button
            @click="confirmDelete"
            class="px-4 py-2 rounded-md bg-red-600 text-white hover:bg-red-700 transition"
          >
            Удалить
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  selectedCount: number
  selectionMode: boolean
}>()

const emit = defineEmits<{
  (e: 'reset'): void
  (e: 'reply'): void
  (e: 'delete'): void
}>()

const showConfirmDelete = ref(false)

const openConfirmDelete = () => {
  // ✅ Используем props.selectedCount
  if (props.selectedCount === 0) return
  showConfirmDelete.value = true
}

const closeConfirmDelete = () => {
  showConfirmDelete.value = false
}

const confirmDelete = () => {
  emit('delete')
  closeConfirmDelete()
}
</script>
