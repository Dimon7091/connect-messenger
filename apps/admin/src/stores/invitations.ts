import { defineStore } from 'pinia'
import { ref, readonly } from 'vue'
import { invitationsService } from '@/services/http/InvitationsService'

export const useInvitationsStore = defineStore('invitations', () => {
  // ========== STATE ==========
  const count = ref<number>(0)
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)
  const lastUpdated = ref<Date | null>(null)

  // Храним идентификатор интервала
  const updateInterval = ref<number | null>(null)

  // ========== GETTERS ==========
  const hasError = () => error.value !== null
  const isDataLoaded = () => count.value > 0

  // ========== ACTIONS ==========

  /** Загрузка количества активных приглашений */
  const fetchActiveInvitations = async () => {
    if (isLoading.value) return

    isLoading.value = true
    error.value = null

    try {
      const data = await invitationsService.getActiveInvitationsCount()
      // Приводим к числу, так как сервер может вернуть строку
      count.value = Number(data.activeInvitationsCount) || 0
      lastUpdated.value = new Date()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Неизвестная ошибка'
      console.error('Ошибка при загрузке активных приглашений:', err)
    } finally {
      isLoading.value = false
    }
  }

  /** Запуск автоматического обновления (по умолчанию – каждые 60 секунд) */
  const startAutoUpdate = (intervalMs: number = 60 * 1000) => {
    if (typeof window === 'undefined') return
    stopAutoUpdate()

    const id = window.setInterval(() => {
      fetchActiveInvitations()
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
    await fetchActiveInvitations()
    startAutoUpdate()
  }

  /** Полная очистка стора (при выходе из системы) */
  const destroy = () => {
    stopAutoUpdate()
    count.value = 0
    isLoading.value = false
    error.value = null
    lastUpdated.value = null
  }

  // ========== EXPORT ==========
  return {
    // state (только для чтения)
    count: readonly(count),
    isLoading: readonly(isLoading),
    error: readonly(error),
    lastUpdated: readonly(lastUpdated),

    // getters
    hasError,
    isDataLoaded,

    // actions
    fetchActiveInvitations,
    startAutoUpdate,
    stopAutoUpdate,
    init,
    destroy,
  }
})
