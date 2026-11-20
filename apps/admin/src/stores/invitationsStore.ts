import { defineStore } from 'pinia';
import { ref, readonly } from 'vue';
import { invitationsService } from '@/services/http/InvitationsService';

export const useInvitationsStore = defineStore('invitations', () => {
  // ========== STATE ==========
  const count = ref<number>(0);
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
  const isDataLoaded = () => count.value > 0;

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

  /** Загрузка количества активных приглашений */
  const fetchActiveInvitations = async () => {
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
      const data = await invitationsService.getActiveInvitationsCount();
      // Приводим к числу, так как сервер может вернуть строку
      count.value = Number(data.activeInvitationsCount) || 0;
      lastUpdated.value = new Date();
    } catch (err) {
      if (isNetworkFailure(err)) {
        isNetworkError.value = true;
        error.value = 'Нет подключения к интернету. Проверьте сеть.';
      } else {
        error.value = err instanceof Error ? err.message : 'Неизвестная ошибка';
      }
      console.error('Ошибка при загрузке активных приглашений:', err);
    } finally {
      isLoading.value = false;
    }
  };

  /** Удаление всех активных приглашений */
  const deleteAllInvitations = async (): Promise<void> => {
    if (isLoading.value || count.value === 0) return;

    isLoading.value = true;
    error.value = null;
    isNetworkError.value = false;

    try {
      await invitationsService.deleteAllInvitations();
      // Сервер вернул 204 — сбрасываем счётчик
      count.value = 0;
      lastUpdated.value = new Date();
    } catch (err) {
      if (isNetworkFailure(err)) {
        isNetworkError.value = true;
        error.value = 'Нет подключения к интернету. Проверьте сеть.';
      } else {
        error.value = err instanceof Error ? err.message : 'Неизвестная ошибка';
      }
      console.error('Ошибка при удалении всех приглашений:', err);
      throw err; // Пробрасываем, чтобы компонент мог показать тост
    } finally {
      isLoading.value = false;
    }
  };

  /** Запуск автоматического обновления (по умолчанию – каждые 60 секунд) */
  const startAutoUpdate = (intervalMs: number = 60 * 1000) => {
    if (typeof window === 'undefined') return;
    stopAutoUpdate();

    const id = window.setInterval(() => {
      fetchActiveInvitations();
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
    await fetchActiveInvitations();
    startAutoUpdate();
  };

  /** Полная очистка стора (при выходе из системы) */
  const destroy = () => {
    stopAutoUpdate();
    destroyNetworkListeners();
    count.value = 0;
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
      fetchActiveInvitations();
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
    count: readonly(count),
    isLoading: readonly(isLoading),
    error: readonly(error),
    lastUpdated: readonly(lastUpdated),
    isOffline: readonly(isOffline),
    isNetworkError: readonly(isNetworkError),

    // getters
    hasError,
    isDataLoaded,

    // actions
    fetchActiveInvitations,
    deleteAllInvitations,
    startAutoUpdate,
    stopAutoUpdate,
    init,
    destroy,
    initNetworkListeners,
    destroyNetworkListeners,
  };
});
