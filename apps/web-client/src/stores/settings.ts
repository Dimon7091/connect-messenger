// stores/settings.ts
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

type ThemeMode = 'light' | 'dark' | 'auto'

export const useSettingsStore = defineStore('settings', () => {
  const themeMode = ref<ThemeMode>('auto')
  const fontSize = ref<number>(16) // базовый размер шрифта в px

  function setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
  }

  function setFontSize(size: number) {
    fontSize.value = size
  }

  // Применяет тему (добавляет/удаляет класс 'dark' на <html>)
  function applyTheme() {
    let isDark = false
    if (themeMode.value === 'auto') {
      isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    } else {
      isDark = themeMode.value === 'dark'
    }

    if (isDark) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  // Применяет глобальный размер шрифта
  function applyFontSize() {
    document.documentElement.style.fontSize = `${fontSize.value}px`
  }

  // Загрузка из localStorage
  function loadFromLocalStorage() {
    const savedTheme = localStorage.getItem('app-theme-mode')
    if (savedTheme === 'light' || savedTheme === 'dark' || savedTheme === 'auto') {
      themeMode.value = savedTheme
    }
    const savedFontSize = localStorage.getItem('app-font-size')
    if (savedFontSize) {
      const size = parseInt(savedFontSize, 10)
      if (!isNaN(size) && size >= 12 && size <= 24) fontSize.value = size
    }
  }

  // Следим за изменениями
  watch(
    [themeMode, fontSize],
    () => {
      localStorage.setItem('app-theme-mode', themeMode.value)
      localStorage.setItem('app-font-size', fontSize.value.toString())
      applyTheme()
      applyFontSize()
    },
    { deep: true },
  )

  // Инициализация
  loadFromLocalStorage()
  applyTheme()
  applyFontSize()

  // Слушаем изменения системной темы, если выбран режим 'auto'
  if (window.matchMedia) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', () => {
      if (themeMode.value === 'auto') applyTheme()
    })
  }

  return { themeMode, fontSize, setThemeMode, setFontSize }
})
