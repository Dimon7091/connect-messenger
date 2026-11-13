import { defineStore } from 'pinia'
import { ref, readonly } from 'vue'
import { usersService } from '@/services/http/UsersService'
import type { UsersStat } from '@/services/http/UsersService'

export const useStatisticsStore = defineStore('statistics', () => {
  // ========== STATE ==========
  const totalUsers = ref<number>(0)
  const onlineUsers = ref<number>(0)
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)
  const lastUpdated = ref<Date | null>(null)

  // Храним идентификатор интервала
  const updateInterval = ref<number | null>(null)

  // ========== GETTERS ==========
  const hasError = () => error.value !== null
  const isDataLoaded = () => totalUsers.value > 0 || onlineUsers.value > 0

  // ========== ACTIONS ==========

  /** Загрузка статистики с сервера */
  const fetchStatistics = async () => {
    if (isLoading.value) return

    isLoading.value = true
    error.value = null

    try {
      const data: UsersStat = await usersService.getUsersStat()
      totalUsers.value = data.totalUsers || 0
      onlineUsers.value = data.onlineUsers || 0
      lastUpdated.value = new Date()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Неизвестная ошибка'
      console.error('Ошибка при загрузке статистики:', err)
    } finally {
      isLoading.value = false
    }
  }

  /** Запуск автоматического обновления (по умолчанию – каждые 60 секунд) */
  const startAutoUpdate = (intervalMs: number = 60 * 1000) => {
    if (typeof window === 'undefined') return
    stopAutoUpdate()

    const id = window.setInterval(() => {
      fetchStatistics()
    }, intervalMs)

    updateInterval.value = id
  }

  /** Остановка автоматического обновления */
  const stopAutoUpdate = () => {
    if (updateInterval.value !== null) {
      clearInterval(updateInterval.value)
      updateInterval.value = null
    }
  }

  /** Инициализация: сразу загружаем данные и запускаем автообновление */
  const init = async () => {
    await fetchStatistics()
    startAutoUpdate()
  }

  /** Полная очистка стора (при выходе из системы) */
  const destroy = () => {
    stopAutoUpdate()
    totalUsers.value = 0
    onlineUsers.value = 0
    isLoading.value = false
    error.value = null
    lastUpdated.value = null
  }

  // ========== EXPORT ==========
  return {
    // state (только для чтения)
    totalUsers: readonly(totalUsers),
    onlineUsers: readonly(onlineUsers),
    isLoading: readonly(isLoading),
    error: readonly(error),
    lastUpdated: readonly(lastUpdated),

    // getters
    hasError,
    isDataLoaded,

    // actions
    fetchStatistics,
    startAutoUpdate,
    stopAutoUpdate,
    init,
    destroy,
  }
})
