import { defineStore } from 'pinia';
import { ref, readonly } from 'vue';
import { usersService } from '@/services/http/UsersService';
import type { UsersStatisticResponse } from '@messenger/shared-schema';

export const useStatisticsStore = defineStore('statistics', () => {
  // ========== STATE ==========
  const totalUsers = ref<number>(0);
  const activeUsers = ref<number>(0);
  const onlineUsers = ref<number>(0);
  const deletedUsers = ref<number>(0);
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);
  const lastUpdated = ref<Date | null>(null);

  // Сеть
  const isOffline = ref(!navigator.onLine);
  const isNetworkError = ref(false); // ошибка именно из-за отсутствия сети

  // Храним идентификатор интервала
  const updateInterval = ref<number | null>(null);

  // ========== GETTERS ==========
  const hasError = () => error.value !== null;
  const isDataLoaded = () =>
    totalUsers.value > 0 ||
    onlineUsers.value > 0 ||
    activeUsers.value > 0 ||
    deletedUsers.value > 0;

  /**
   * Отличает сетевую ошибку от серверной.
   * fetch при обрыве бросает TypeError, axios — ERR_NETWORK / status 0.
   */
  function isNetworkFailure(err: unknown): boolean {
    if (!navigator.onLine) return true;
    if (err instanceof TypeError) return true;
    const anyErr = err as any;
    return (
      anyErr?.code === 'ERR_NETWORK' ||
      anyErr?.name === 'NetworkError' ||
      anyErr?.status === 0 ||
      anyErr?.message === 'Failed to fetch'
    );
  }

  // ========== ACTIONS ==========

  /** Загрузка статистики с сервера */
  const fetchStatistics = async () => {
    if (isLoading.value) return;

    // Не дёргаем сервер, если браузер явно offline
    if (isOffline.value) {
      isNetworkError.value = true;
      error.value = 'Нет подключения к интернету. Проверьте сеть.';
      return;
    }

    isLoading.value = true;
    error.value = null;
    isNetworkError.value = false;

    try {
      const data: UsersStatisticResponse = await usersService.getUsersStatistic();
      totalUsers.value = data.totalUsers || 0;
      activeUsers.value = data.activeUsers || 0;
      onlineUsers.value = data.onlineUsers || 0;
      deletedUsers.value = data.deletedUsers || 0;
      lastUpdated.value = new Date();
    } catch (err) {
      if (isNetworkFailure(err)) {
        isNetworkError.value = true;
        error.value = 'Нет подключения к интернету. Проверьте сеть.';
      } else {
        error.value = err instanceof Error ? err.message : 'Неизвестная ошибка';
      }
      console.error('Ошибка при загрузке статистики:', err);
    } finally {
      isLoading.value = false;
    }
  };

  /** Запуск автоматического обновления (по умолчанию – каждые 60 секунд) */
  const startAutoUpdate = (intervalMs: number = 60 * 1000) => {
    if (typeof window === 'undefined') return;
    stopAutoUpdate();

    const id = window.setInterval(() => {
      fetchStatistics();
    }, intervalMs);

    updateInterval.value = id;
  };

  /** Остановка автоматического обновления */
  const stopAutoUpdate = () => {
    if (updateInterval.value !== null) {
      clearInterval(updateInterval.value);
      updateInterval.value = null;
    }
  };

  /** Инициализация: сразу загружаем данные и запускаем автообновление */
  const init = async () => {
    await fetchStatistics();
    startAutoUpdate();
  };

  /**
   * Инкрементирует счётчик удалённых и декрементирует счётчик активных
   * без запроса на сервер. Используется после успешного удаления пользователя
   * через admin-панель, чтобы статистика обновилась мгновенно.
   */
  const removeActiveUser = () => {
    if (deletedUsers.value >= 0) {
      deletedUsers.value++;
    }
    if (activeUsers.value > 0) {
      activeUsers.value--;
    }
  };

  /** Полная очистка стора (при выходе из системы) */
  const destroy = () => {
    stopAutoUpdate();
    destroyNetworkListeners();
    totalUsers.value = 0;
    activeUsers.value = 0;
    onlineUsers.value = 0;
    deletedUsers.value = 0;
    isLoading.value = false;
    error.value = null;
    lastUpdated.value = null;
    isNetworkError.value = false;
  };

  // ========== Сетевые события ==========

  const handleOnline = () => {
    isOffline.value = false;
    // Связь вернулась — если была сетевая ошибка, перезапрашиваем сразу
    if (isNetworkError.value) {
      fetchStatistics();
    }
  };

  const handleOffline = () => {
    isOffline.value = true;
  };

  /** Вызывать один раз при монтировании layout/страницы. */
  const initNetworkListeners = () => {
    if (typeof window === 'undefined') return;
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
  };

  /** Вызывать при размонтировании, чтобы не было утечек. */
  const destroyNetworkListeners = () => {
    if (typeof window === 'undefined') return;
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };

  // ========== EXPORT ==========
  return {
    // state (только для чтения)
    totalUsers: readonly(totalUsers),
    activeUsers: readonly(activeUsers),
    onlineUsers: readonly(onlineUsers),
    deletedUsers: readonly(deletedUsers),
    isLoading: readonly(isLoading),
    error: readonly(error),
    lastUpdated: readonly(lastUpdated),
    isOffline: readonly(isOffline),
    isNetworkError: readonly(isNetworkError),

    // getters
    hasError,
    isDataLoaded,

    // actions
    fetchStatistics,
    startAutoUpdate,
    stopAutoUpdate,
    init,
    destroy,
    removeActiveUser,
    initNetworkListeners,
    destroyNetworkListeners,
  };
});
