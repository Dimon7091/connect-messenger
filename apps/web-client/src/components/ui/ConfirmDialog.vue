<!-- components/ui/ConfirmDialog.vue -->
<template>
  <Teleport to="#modal-root">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-60 flex items-center justify-center"
        @click.self="close"
      >
        <div class="absolute inset-0 bg-black/40"></div>
        <div
          class="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-sm w-full mx-4 p-6"
          @click.stop
        >
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {{ title }}
          </h3>
          <p class="text-gray-600 dark:text-gray-300 mb-6">
            {{ description }}
          </p>
          <div class="flex justify-between gap-6">
            <button
              v-if="showCancel"
              @click="close"
              class="px-4 py-2 border border-gray-200 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition"
            >
              {{ cancelText }}
            </button>
            <button
              @click="confirm"
              class="px-4 py-2 rounded-md text-white transition flex items-center gap-2 justify-center min-w-[100px]"
              :class="confirmButtonClass"
              :disabled="loading"
            >
              <!-- Спиннер -->
              <svg
                v-if="loading"
                class="animate-spin h-5 w-5 text-white"
                xmlns="http://www.w3.org/2000/svg"
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
              <span v-if="loading">{{ loadingText }}</span>
              <span v-else>{{ confirmText }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    description: string
    confirmText?: string
    cancelText?: string
    confirmButtonClass?: string | Record<string, boolean>
    loading?: boolean
    loadingText?: string
    showCancel?: boolean
  }>(),
  {
    confirmText: 'Подтвердить',
    cancelText: 'Отмена',
    confirmButtonClass: 'bg-blue-600 hover:bg-blue-700',
    loading: false,
    loadingText: 'Загрузка...',
    showCancel: true,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const close = () => {
  if (!props.loading) {
    emit('update:modelValue', false)
  }
}

const confirm = () => {
  emit('confirm')
}
</script>
