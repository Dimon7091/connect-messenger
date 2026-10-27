<template>
  <Teleport to="#app">
    <!-- Фиксированный контейнер на весь экран, но с pointer-events: none, чтобы клики проходили сквозь фон -->
    <div class="fixed bottom-25 left-0 right-0 pointer-events-none flex justify-center">
      <!-- Центрированный блок с ограничением ширины -->
      <div class="w-full max-w-5xl mx-auto pointer-events-auto relative h-0">
        <!-- Сама кнопка – абсолютно позиционирована внутри центрированного блока -->
        <button
          @click="goToSearch"
          class="absolute bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-[#50a2e9] text-white shadow-xl transition-all active:scale-90"
        >
          <div class="relative flex items-center justify-center w-6 h-6">
            <span class="absolute block w-full h-0.5 bg-white rounded-full"></span>
            <span class="absolute block h-full w-0.5 bg-white rounded-full"></span>
          </div>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const goToSearch = () => router.push('/search')
</script>
