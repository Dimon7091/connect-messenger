import { defineStore } from 'pinia'
import { ref } from 'vue'
import { usersService } from '@/services/http/UsersService'
import type { UserAdminResponse } from '@messenger/shared-schema'
import type { PageResponse } from '@/services/http/UsersService'

export const useUsersStore = defineStore('users', () => {
  // State
  const users = ref<UserAdminResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Пагинация
  const currentPage = ref(0)
  const pageSize = ref(10)
  const totalElements = ref(0)
  const totalPages = ref(0)

  // Фильтры
  const searchQuery = ref('')
  const statusFilter = ref('')

  // Сортировка
  const sortBy = ref<string | null>(null)
  const sortOrder = ref<'asc' | 'desc' | null>(null)

  // Маппинг полей для сортировки
  const sortFieldMap: Record<string, string> = {
    firstName: 'profile.firstName',
    userName: 'userName',
    status: 'userStatus.status',
    lastSeen: 'userStatus.lastSeen',
    createdAt: 'createdAt',
  }

  function getSortField(field: string | null): string | null {
    if (!field) return null
    return sortFieldMap[field] || field
  }

  // Actions
  const fetchUsers = async () => {
    loading.value = true
    error.value = null

    try {
      const data: PageResponse<UserAdminResponse> = await usersService.getUsers({
        page: currentPage.value,
        size: pageSize.value,
        search: searchQuery.value || undefined,
        status: statusFilter.value || undefined,
        sortBy: getSortField(sortBy.value) ?? undefined,
        sortDir: sortOrder.value ?? undefined,
      })

      users.value = data.content
      totalElements.value = data.totalElements
      totalPages.value = data.totalPages
      currentPage.value = data.number
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Неизвестная ошибка'
      console.error('Ошибка при загрузке пользователей:', err)
    } finally {
      loading.value = false
    }
  }

  const setUserBlock = async (userId: string, isBanned: boolean): Promise<UserAdminResponse> => {
    const updatedUser = await usersService.setUserBlock(userId, isBanned)

    // Обновляем пользователя в списке users (для таблицы)
    const index = users.value.findIndex((u) => u.id === updatedUser.id)
    if (index !== -1) {
      users.value[index] = updatedUser
    }

    return updatedUser
  }

  const deleteUser = async (userId: string): Promise<UserAdminResponse> => {
    let updatedUser: UserAdminResponse

    try {
      updatedUser = await usersService.deleteUser(userId)
    } catch {
      // Если сервер вернул ошибку или 204 — обновляем локального пользователя
      const currentUser = users.value.find((u) => u.id === userId)
      if (currentUser) {
        updatedUser = { ...currentUser, isDeleted: true }
      } else {
        // Если пользователя нет в списке — создаём заглушку
        updatedUser = {
          id: userId,
          isDeleted: true,
          userName: 'Удалённый пользователь',
          profile: {
            firstName: 'Удалён',
            lastName: '',
            avatarThumbUrl: null,
          },
          status: 'OFFLINE',
          roles: [],
          isBanned: false,
          createdAt: new Date().toISOString(),
          lastSeen: new Date().toISOString(),
          email: '', // обязательное поле, если есть
        } as UserAdminResponse
      }
    }

    // Обновляем список в сторе
    const index = users.value.findIndex((u) => u.id === updatedUser.id)
    if (index !== -1) {
      users.value[index] = updatedUser
    }

    return updatedUser
  }

  const setSort = (field: string) => {
    if (sortBy.value === field) {
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortBy.value = field
      sortOrder.value = 'asc'
    }
    currentPage.value = 0
    fetchUsers()
  }

  const resetSort = () => {
    sortBy.value = null
    sortOrder.value = null
  }

  const setPage = (page: number) => {
    if (page >= 0 && page < totalPages.value) {
      currentPage.value = page
      fetchUsers()
    }
  }

  const nextPage = () => {
    if (currentPage.value + 1 < totalPages.value) {
      currentPage.value++
      fetchUsers()
    }
  }

  const prevPage = () => {
    if (currentPage.value > 0) {
      currentPage.value--
      fetchUsers()
    }
  }

  const setSearchQuery = (query: string) => {
    searchQuery.value = query
    currentPage.value = 0
    fetchUsers()
  }

  const setStatusFilter = (status: string) => {
    statusFilter.value = status
    currentPage.value = 0
    fetchUsers()
  }

  const resetFilters = () => {
    searchQuery.value = ''
    statusFilter.value = ''
    sortBy.value = null
    sortOrder.value = null
    currentPage.value = 0
    fetchUsers()
  }

  const setPageSize = (size: number) => {
    pageSize.value = size
    currentPage.value = 0
    fetchUsers()
  }

  // ========== Getters ==========

  // Инициалы – первая буква имени
  const getInitials = (name: string) => {
    return name.charAt(0).toUpperCase()
  }

  // Полное имя (имя + фамилия), если фамилия есть – с пробелом
  const getFullName = (user: UserAdminResponse): string => {
    const firstName = user.profile?.firstName || ''
    const lastName = user.profile?.lastName || ''
    if (firstName && lastName) {
      return `${firstName} ${lastName}`
    }
    return firstName || lastName || 'Без имени'
  }

  const getStatusColor = (status: string) => {
    return status === 'ONLINE' ? 'bg-green-400 text-gray-900' : 'bg-gray-200 text-gray-600'
  }

  const getStatusDotColor = (status: string) => {
    return status === 'ONLINE' ? 'bg-green-800' : 'bg-gray-500'
  }

  const getAvatarColor = (name: string) => {
    const colors = [
      'from-blue-400 to-blue-600',
      'from-green-400 to-green-600',
      'from-purple-400 to-purple-600',
      'from-pink-400 to-pink-600',
      'from-orange-400 to-orange-600',
      'from-red-400 to-red-600',
      'from-indigo-400 to-indigo-600',
    ]
    const index = name.charCodeAt(0) % colors.length
    return colors[index]
  }

  return {
    // State
    users,
    loading,
    error,
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

    // Getters
    getInitials,
    getFullName, // <-- новый геттер
    getStatusColor,
    getStatusDotColor,
    getAvatarColor,
  }
})
