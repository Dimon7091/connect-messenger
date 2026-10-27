<template>
  <div class="p-4 bg-white dark:bg-gray-900 rounded-lg shadow m-2">
    <div class="mb-4">
      <div class="text-center mb-2">
        <span class="inline-block px-3 py-1 text-sl font-medium text-gray-700 dark:text-gray-200">
          {{ activeThemeName }}
        </span>
      </div>

      <div class="relative flex bg-gray-100 dark:bg-gray-700 rounded-full p-1">
        <div
          class="absolute top-1 bottom-1 w-1/3 bg-white dark:bg-gray-600 rounded-full shadow-sm transition-all duration-200 ease-out"
          :style="{ left: sliderLeft }"
        ></div>

        <button
          @click="settingsStore.setThemeMode('light')"
          class="relative flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-full text-sm font-medium transition-colors z-10 cursor-pointer"
          :class="getButtonClass('light')"
        >
          <span class="text-base">☀️</span>
          <span class="hidden sm:inline">Светлая</span>
        </button>

        <button
          @click="settingsStore.setThemeMode('auto')"
          class="relative flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-full text-sm font-medium transition-colors z-10 cursor-pointer"
          :class="getButtonClass('auto')"
        >
          <span class="text-base">🌓</span>
          <span class="hidden sm:inline">Системная</span>
        </button>

        <button
          @click="settingsStore.setThemeMode('dark')"
          class="relative flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-full text-sm font-medium transition-colors z-10 cursor-pointer"
          :class="getButtonClass('dark')"
        >
          <span class="text-base">🌙</span>
          <span class="hidden sm:inline">Тёмная</span>
        </button>
      </div>
    </div>

    <div>
      <label class="block text-base font-medium text-gray-700 dark:text-gray-200 mb-1">
        Размер шрифта: {{ settingsStore.fontSize }}px
      </label>
      <input
        type="range"
        min="12"
        max="20"
        step="1"
        :value="settingsStore.fontSize"
        @input="updateFontSize"
        :style="rangeStyle"
        class="w-full range-input"
      />
    </div>
  </div>

  <!-- Черный список -->
  <div
    class="flex justify-between p-4 bg-white dark:bg-gray-900 rounded-lg shadow m-2 cursor-pointer"
  >
    <div @click="goToBlackList" class="flex gap-2">
      <div>
        <svg
          class="dark:text-gray-200"
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="8" y1="12" x2="16" y2="12" />
        </svg>
      </div>
      <h2 class="text-sl font-medium text-gray-700 dark:text-gray-200">Черный список</h2>
    </div>
    <span class="font-medium text-blue-400">{{ blackListIds }} </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

const activeThemeName = computed(() => {
  switch (settingsStore.themeMode) {
    case 'light':
      return 'Светлая тема'
    case 'dark':
      return 'Тёмная тема'
    case 'auto':
      return 'Системная тема'
    default:
      return 'Тема'
  }
})

const sliderLeft = computed(() => {
  switch (settingsStore.themeMode) {
    case 'light':
      return '1.2%'
    case 'auto':
      return '33.333333%'
    case 'dark':
      return '65.666667%'
    default:
      return '0%'
  }
})

const getButtonClass = (mode: string) => {
  const isActive = settingsStore.themeMode === mode
  return [
    isActive
      ? 'text-gray-900 dark:text-white'
      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200',
  ]
}

// --- Добавляем прогресс для ползунка ---
const progress = computed(() => {
  const min = 12,
    max = 20
  const val = settingsStore.fontSize
  return ((val - min) / (max - min)) * 100
})

const rangeStyle = computed(() => ({
  background: `linear-gradient(to right, #3b82f6 0%, #3b82f6 ${progress.value}%, #e5e7eb ${progress.value}%, #e5e7eb 100%)`,
}))

const updateFontSize = (event: Event) => {
  const target = event.target as HTMLInputElement
  settingsStore.setFontSize(parseInt(target.value, 10))
}

const blackListIds = computed(() => {
  const blackListArray = userStore.user?.blackListIds
  return blackListArray?.length
})

const goToBlackList = () => router.push('/blacklist')
</script>

<style scoped>
/* Базовые стили – фон делаем прозрачным, чтобы градиент был виден */
input[type='range'] {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 6px;
  background: transparent; /* важно для градиента */
  border-radius: 9999px;
  outline: none;
}

/* Трек WebKit: прозрачный, чтобы не перекрывать градиент */
input[type='range']::-webkit-slider-runnable-track {
  width: 100%;
  height: 6px;
  background: transparent;
  border-radius: 9999px;
}

/* Бегунок WebKit */
input[type='range']::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  background: #3b82f6;
  border-radius: 50%;
  cursor: pointer;
  margin-top: -6px;
  transition: background 0.15s;
}
input[type='range']::-webkit-slider-thumb:hover {
  background: #2563eb;
}

/* Трек Firefox: прозрачный */
input[type='range']::-moz-range-track {
  width: 100%;
  height: 6px;
  background: transparent;
  border-radius: 9999px;
}

/* Бегунок Firefox */
input[type='range']::-moz-range-thumb {
  width: 16px;
  height: 16px;
  background: #3b82f6;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background 0.15s;
}
input[type='range']::-moz-range-thumb:hover {
  background: #2563eb;
}

/* Для IE – можно оставить с серым треком, но IE не поддерживает градиент, так что оставим как fallback */
input[type='range']::-ms-track {
  width: 100%;
  height: 6px;
  background: #e5e7eb;
  border-radius: 9999px;
  border: none;
  color: transparent;
}
input[type='range']::-ms-thumb {
  width: 16px;
  height: 16px;
  background: #3b82f6;
  border-radius: 50%;
  cursor: pointer;
}
</style>
