<template>
  <Teleport to="#toast-root">
    <div
      aria-live="polite"
      aria-atomic="true"
      class="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none"
    >
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 translate-x-8"
        enter-to-class="opacity-100 translate-x-0"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-x-0"
        leave-to-class="opacity-0 translate-x-8"
      >
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          :class="[
            'pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg text-sm font-medium max-w-sm',
            bgClass(toast.type),
            textClass(toast.type),
          ]"
        >
          <span class="flex-1">{{ toast.message }}</span>
          <button
            @click="toastStore.remove(toast.id)"
            class="shrink-0 ml-2 opacity-70 hover:opacity-100 transition text-current"
          >
            ✕
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useToastStore, type ToastType } from '@/stores/toastStore';

const toastStore = useToastStore();

function bgClass(type: ToastType): string {
  switch (type) {
    case 'success':
      return 'bg-green-600';
    case 'error':
      return 'bg-red-600';
    case 'warning':
      return 'bg-yellow-500';
    case 'info':
      return 'bg-blue-600';
  }
}

function textClass(type: ToastType): string {
  switch (type) {
    case 'warning':
      return 'text-yellow-900';
    default:
      return 'text-white';
  }
}
</script>