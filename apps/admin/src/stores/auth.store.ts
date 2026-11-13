import { defineStore } from 'pinia';
import { ref, computed, readonly } from 'vue';
import { jwtDecode } from 'jwt-decode';
import { authService } from '@/services/http/AuthService';
import { httpClient } from '@/services/http/HttpClient';
import type { AuthRequestDto } from '@messenger/shared-schema';

/**
 * Интерфейс пользователя, возвращаемый AuthResponse.
 * AuthResponseSchema содержит UserPrivateResponse, у которого есть поле `role`.
 */
export interface AdminUser {
  id: string;
  userName: string;
  profile: {
    firstName: string;
    lastName: string | null;
    avatarUrl?: string | null;
    avatarThumbUrl?: string | null;
  } | null;
  role: string;
  createdAt: string;
  isDeleted: boolean;
  isBanned: boolean;
  blackListIds: string[];
}

export const useAuthStore = defineStore('admin', () => {
  // ========== STATE ==========
  const user = ref<AdminUser | null>(null);
  const isLoading = ref(false);
  const isInitialized = ref(false);
  const error = ref<string | null>(null);
  const updateInterval = ref<number | null>(null);

  // ========== GETTERS ==========
  const userName = computed(() => user.value?.userName);
  const fullName = computed(() => {
    if (!user.value) return 'Администратор';
    const { firstName, lastName } = user.value.profile || {};
    return `${firstName ?? ''} ${lastName ?? ''}`.trim() || 'Администратор';
  });

  const firstName = computed(() => user.value?.profile?.firstName ?? 'Администратор');
  const lastName = computed(() => user.value?.profile?.lastName ?? '');
  const firstLetter = computed(() => user.value?.profile?.firstName?.charAt(0).toUpperCase() ?? '');

  const isAdmin = computed<boolean>(() => {
    // В UserPrivateResponse поле называется `role` — это строка вида 'ROLE_ADMIN'
    return user.value?.role?.includes('admin') ?? false;
  });

  const isAuthenticated = computed(() => !!user.value);

  // ========== PRIVATE HELPERS ==========
  /**
   * Декодирует JWT и извлекает идентификатор пользователя.
   */
  function getUserIdFromToken(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
      const payload = jwtDecode<{ userId?: string; sub?: string }>(token);
      return payload.userId ?? payload.sub ?? null;
    } catch {
      return null;
    }
  }

  // ========== ACTIONS ==========

  /**
   * Авторизация администратора.
   * Отправляет AuthRequestDto, получает AuthResponse, проверяет роль 'admin'.
   */
  async function login(request: AuthRequestDto): Promise<AdminUser> {
    isLoading.value = true;
    error.value = null;

    try {
      const authResponse = await authService.login(request);
      const { user: loggedUser, token } = authResponse;

      // Проверяем, что у пользователя есть роль администратора
      if (!loggedUser.role?.toLowerCase().includes('admin')) {
        throw new Error('Доступ запрещён. Только администраторы могут войти в панель управления.');
      }

      httpClient.setToken(token);
      user.value = loggedUser as AdminUser;
      isInitialized.value = true;

      return loggedUser as AdminUser;
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Ошибка подключения к серверу';
      error.value = message;
      isInitialized.value = true;
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * Загружает данные текущего пользователя с сервера и проверяет роль.
   */
  async function fetchAdmin(): Promise<boolean> {
    if (isLoading.value) return false;

    const token = localStorage.getItem('token');
    if (!token) {
      destroy();
      return false;
    }

    isLoading.value = true;
    error.value = null;

    try {
      const currentUser = await authService.getCurrentUser();

      if (!currentUser.role?.toLowerCase().includes('admin')) {
        throw new Error('Доступ запрещён. Только администраторы могут использовать панель.');
      }

      user.value = currentUser as AdminUser;
      return true;
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Неизвестная ошибка при загрузке';
      console.error('Ошибка в fetchAdmin:', err);
      user.value = null;
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * Запускает периодическое обновление данных администратора.
   */
  function startAutoUpdate(intervalMs: number = 3 * 60 * 1000) {
    if (typeof window === 'undefined') return;
    stopAutoUpdate();

    const id = window.setInterval(() => {
      fetchAdmin();
    }, intervalMs);

    updateInterval.value = id;
  }

  /** Останавливает периодическое обновление */
  function stopAutoUpdate() {
    if (updateInterval.value !== null) {
      clearInterval(updateInterval.value);
      updateInterval.value = null;
    }
  }

  /** Инициализация стора: загрузка данных + запуск автообновления */
  async function init() {
    if (isInitialized.value) return;
    await fetchAdmin();
    if (user.value) {
      startAutoUpdate();
    }
    isInitialized.value = true;
  }

  /** Выход из системы */
  function logout() {
    destroy();
    httpClient.clearToken();
  }

  /** Полная очистка стора при выходе из системы */
  function destroy() {
    stopAutoUpdate();
    user.value = null;
    isLoading.value = false;
    isInitialized.value = false;
    error.value = null;
  }

  // ========== EXPORT ==========
  return {
    // state (только для чтения)
    user: readonly(user),
    isLoading: readonly(isLoading),
    isInitialized: readonly(isInitialized),
    error: readonly(error),

    // getters
    userName,
    fullName,
    firstName,
    lastName,
    firstLetter,
    isAdmin,
    isAuthenticated,

    // actions
    login,
    fetchAdmin,
    startAutoUpdate,
    stopAutoUpdate,
    init,
    logout,
    destroy,

    // вспомогательный метод
    getUserIdFromToken,
  };
});
