<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0 scale-95"
    enter-to-class="opacity-100 scale-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100 scale-100"
    leave-to-class="opacity-0 scale-95"
  >
    <div
      v-if="visible"
      class="fixed inset-0 z-50 flex items-center justify-center"
      @click.self="$emit('close')"
    >
      <div class="absolute inset-0 bg-black/70"></div>
      <div class="relative max-w-[95vw] max-h-[90vh]">
        <img
          :src="imageUrl"
          :alt="alt || 'Аватар'"
          class="max-w-full max-h-screen object-contain rounded-lg shadow-2xl"
          @error="onImageError"
        />
        <button
          @click="$emit('close')"
          class="absolute -top-10 right-0 text-white hover:text-gray-300 transition"
          title="Закрыть"
        >
          <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
  </Transition>
</template>

<script setup lang="ts">
const props = defineProps<{
  visible: boolean
  imageUrl: string
  alt?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'error'): void
}>()

const onImageError = () => {
  emit('error')
}
</script>

<style scoped>
/* Дополнительные стили, если нужны */
</style>
