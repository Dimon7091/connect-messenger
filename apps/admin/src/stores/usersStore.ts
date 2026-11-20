import { defineStore } from 'pinia';
import { ref } from 'vue';
import { usersService } from '@/services/http/UsersService';
import type { UserAdminResponse, PaginatedResponse } from '@messenger/shared-schema';

export const useUsersStore = defineStore('users', () => {
  // State
  const users = ref<UserAdminResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Сеть
  const isOffline = ref(!navigator.onLine);
  const isNetworkError = ref(false); // ошибка именно из-за отсутствия сети

  // Пагинация
  const currentPage = ref(1);
  const pageSize = ref(10);
  const totalElements = ref(0);
  const totalPages = ref(0);

  // Фильтры
  const searchQuery = ref('');
  const statusFilter = ref('');

  // Сортировка
  const sortBy = ref<string | null>(null);
  const sortOrder = ref<'asc' | 'desc' | null>(null);

  // Маппинг полей для сортировки
  const sortFieldMap: Record<string, string> = {
    firstName: 'firstName',
    userName: 'userName',
    status: 'status',
    lastSeen: 'lastSeen',
    createdAt: 'createdAt',
  };

  function getSortField(field: string | null): string | null {
    if (!field) return null;
    return sortFieldMap[field] || field;
  }

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

  // Actions
  const fetchUsers = async () => {
    loading.value = true;
    error.value = null;
    isNetworkError.value = false;

    try {
      const data: PaginatedResponse<UserAdminResponse> = await usersService.getUsers({
        page: currentPage.value,
        limit: pageSize.value,
        search: searchQuery.value || undefined,
        sortBy: getSortField(sortBy.value) ?? undefined,
        sortOrder: sortOrder.value ?? undefined,
      });

      users.value = data.items;
      totalElements.value = data.meta.total;
      totalPages.value = data.meta.totalPages;
      currentPage.value = data.meta.page;
    } catch (err) {
      if (isNetworkFailure(err)) {
        isNetworkError.value = true;
        error.value = 'Нет подключения к интернету. Проверьте сеть.';
      } else {
        error.value = err instanceof Error ? err.message : 'Неизвестная ошибка';
      }
      console.error('Ошибка при загрузке пользователей:', err);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Блокировка / разблокировка пользователя.
   * При ошибке (в т.ч. сетевой) — ошибка пробрасывается в компонент.
   */
  const setUserBlock = async (userId: string, isBanned: boolean): Promise<UserAdminResponse> => {
    try {
      const updatedUser = await usersService.setUserBlock(userId, isBanned);

      const index = users.value.findIndex(u => u.id === updatedUser.id);
      if (index !== -1) {
        users.value[index] = updatedUser;
      }

      return updatedUser;
    } catch (err) {
      if (isNetworkFailure(err)) isNetworkError.value = true;
      throw err;
    }
  };

  /**
   * Мягкое удаление пользователя (анонимизация).
   * При ошибке (в т.ч. сетевой) — ошибка пробрасывается в компонент.
   */
  const deleteUser = async (userId: string): Promise<UserAdminResponse> => {
    try {
      const updatedUser = await usersService.deleteUser(userId);

      const index = users.value.findIndex(u => u.id === updatedUser.id);
      if (index !== -1) {
        users.value[index] = updatedUser;
      }

      return updatedUser;
    } catch (err) {
      if (isNetworkFailure(err)) isNetworkError.value = true;
      throw err;
    }
  };

  const setSort = (field: string) => {
    if (sortBy.value === field) {
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
    } else {
      sortBy.value = field;
      sortOrder.value = 'asc';
    }
    currentPage.value = 1;
    fetchUsers();
  };

  const resetSort = () => {
    sortBy.value = null;
    sortOrder.value = null;
  };

  const setPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
      fetchUsers();
    }
  };

  const nextPage = () => {
    if (currentPage.value < totalPages.value) {
      currentPage.value++;
      fetchUsers();
    }
  };

  const prevPage = () => {
    if (currentPage.value > 1) {
      currentPage.value--;
      fetchUsers();
    }
  };

  const setSearchQuery = (query: string) => {
    searchQuery.value = query;
    currentPage.value = 1;
    fetchUsers();
  };

  const setStatusFilter = (status: string) => {
    statusFilter.value = status;
    currentPage.value = 1;
    fetchUsers();
  };

  const resetFilters = () => {
    searchQuery.value = '';
    statusFilter.value = '';
    sortBy.value = null;
    sortOrder.value = null;
    currentPage.value = 1;
    fetchUsers();
  };

  const setPageSize = (size: number) => {
    pageSize.value = size;
    currentPage.value = 1;
    fetchUsers();
  };

  // ========== Сетевые события ==========

  const handleOnline = () => {
    isOffline.value = false;
    // как только связь вернулась — пробуем перезагрузить, если была ошибка
    if (error.value && isNetworkError.value) {
      fetchUsers();
    }
  };

  const handleOffline = () => {
    isOffline.value = true;
  };

  /** Вызывать один раз при монтировании layout/страницы. */
  const initNetworkListeners = () => {
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
  };

  /** Вызывать при размонтировании, чтобы не было утечек. */
  const destroyNetworkListeners = () => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };

  // ========== Getters ==========

  const getInitials = (name: string) => name.charAt(0).toUpperCase();

  const getFullName = (user: UserAdminResponse): string => {
    const firstName = user.firstName || '';
    const lastName = user.lastName || '';
    if (firstName && lastName) {
      return `${firstName} ${lastName}`;
    }
    return firstName || lastName || 'Без имени';
  };

  const getStatusColor = (status: string) => {
    return status === 'ONLINE' ? 'bg-green-400 text-gray-900' : 'bg-gray-200 text-gray-600';
  };

  const getStatusDotColor = (status: string) => {
    return status === 'ONLINE' ? 'bg-green-800' : 'bg-gray-500';
  };

  const getAvatarColor = (name: string) => {
    const colors = [
      'from-blue-400 to-blue-600',
      'from-green-400 to-green-600',
      'from-purple-400 to-purple-600',
      'from-pink-400 to-pink-600',
      'from-orange-400 to-orange-600',
      'from-red-400 to-red-600',
      'from-indigo-400 to-indigo-600',
    ];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  };

  return {
    // State
    users,
    loading,
    error,
    isOffline,
    isNetworkError,
    currentPage,
    pageSize,
    totalElements,
    totalPages,
    searchQuery,
    statusFilter,
    sortBy,
    sortOrder,

    // Actions
    setUserBlock,
    deleteUser,
    fetchUsers,
    setPage,
    nextPage,
    prevPage,
    setSearchQuery,
    setStatusFilter,
    resetFilters,
    setPageSize,
    setSort,
    resetSort,
    initNetworkListeners,
    destroyNetworkListeners,

    // Getters
    getInitials,
    getFullName,
    getStatusColor,
    getStatusDotColor,
    getAvatarColor,
  };
});
