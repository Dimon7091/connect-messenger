// stores/userStore.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { httpClient } from '@/services/http/HttpClient'
import { authService } from '@/services/http/AuthService'
import { userService } from '@/services/http/UserService'
import type { UpdateUserProfileRequest } from '@/types/index'
import type { User, UserStatus, UsersGettingByIdsRequest } from '@messenger/shared-schema'
import { wsClient } from '@/services/websocket/WebSocketClient'

// ==================== Вспомогательная функция нормализации ====================
function normalizeUser(user: User): User {
  if (!user) return user
  return {
    ...user,
    id: String(user.id),
    blackListIds: (user.blackListIds || []).map((id: string) => String(id)),
  }
}

export const useUserStore = defineStore('user', () => {
  // ==================== Состояние ====================
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))
  const isInitialized = ref(false)
  const loading = ref(false)

  // Кеш пользователей
  const usersCache = ref<Map<string, User>>(new Map())
  const userStatuses = ref<Record<string, UserStatus>>({})

  // ==================== Геттеры ====================
  const fullName = computed(() => {
    if (!user.value) return ''
    if (user.value.profile.firstName || user.value.profile.lastName) {
      return `${user.value.profile.firstName || ''} ${user.value.profile.lastName || ''}`.trim()
    }
    return user.value.userName
  })

  const isAuthenticated = computed(() => !!token.value && !!user.value)

  // ==================== Действия ====================

  async function login(username: string, password: string) {
    loading.value = true
    try {
      const { user: userData, token: authToken } = await authService.login(username, password)
      setSession(userData, authToken)
      return { success: true }
    } catch (error) {
      clearSession()
      throw error
    } finally {
      loading.value = false
    }
  }

  async function register(userData: {
    userName: string
    password: string
    firstName?: string
    lastName?: string
    invitationToken: string
  }): Promise<User | null> {
    loading.value = true
    try {
      const { user: registeredUser, token: authToken } = await authService.register(userData)
      setSession(registeredUser, authToken)
      return registeredUser
    } catch (error) {
      console.error('Ошибка регистрации:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function deleteAccount() {
    loading.value = true
    try {
      await userService.delete()
      clearSession()
    } catch (error) {
      console.error('Ошибка удаления аккаунта:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function initAuth() {
    if (!token.value) {
      isInitialized.value = true
      return
    }

    loading.value = true
    try {
      httpClient.setToken(token.value)
      const currentUser = await authService.getCurrentUser()
      const normalized = normalizeUser(currentUser)
      user.value = normalized
      usersCache.value.set(normalized.id, normalized)
    } catch (error) {
      console.error('Ошибка при инициализации сессии:', error)
      clearSession()
    } finally {
      isInitialized.value = true
      loading.value = false
    }
  }

  async function logout() {
    loading.value = true
    try {
      clearSession()
      wsClient.disconnect()
    } catch (error) {
      console.error('Ошибка при выходе:', error)
    } finally {
      loading.value = false
    }
  }

  function setSession(userData: User, authToken: string) {
    const normalized = normalizeUser(userData)
    user.value = normalized
    token.value = authToken
    localStorage.setItem('token', authToken)
    httpClient.setToken(authToken)
    usersCache.value.set(normalized.id, normalized)
  }

  function clearSession() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    httpClient.setToken(null)
  }

  // ==================== Методы для работы с кешем ====================

  function getUserFromCache(userId: string): User | null {
    const id = String(userId)
    if (user.value?.id === id) {
      return user.value
    }
    return usersCache.value.get(id) || null
  }

  async function getUserById(userId: string): Promise<User | null> {
    const id = String(userId)
    const cachedUser = getUserFromCache(id)
    if (cachedUser) return cachedUser

    try {
      loading.value = true
      const fetchedUser = await userService.getUserById(id)
      if (fetchedUser) {
        const normalized = normalizeUser(fetchedUser)
        usersCache.value.set(id, normalized)
        return normalized
      }
      return null
    } catch (error) {
      console.error(`Ошибка загрузки пользователя ${id}:`, error)
      return null
    } finally {
      loading.value = false
    }
  }

  async function getUsersByIds(userIds: string[]): Promise<Map<string, User>> {
    const result = new Map<string, User>()
    const uncachedIds: string[] = []

    // Собираем кешированных и определяем, кого нужно запросить
    for (const userId of userIds) {
      const id = String(userId)
      const cachedUser = getUserFromCache(id)
      if (cachedUser) {
        result.set(id, cachedUser)
      } else {
        uncachedIds.push(id)
      }
    }

    // Запрашиваем только недостающих пользователей
    if (uncachedIds.length > 0) {
      try {
        const data: UsersGettingByIdsRequest = {
          usersIds: uncachedIds, // ← исправлено
        }
        const fetchedUsers = await userService.getUsersById(data)

        // Обрабатываем полученный массив пользователей
        if (fetchedUsers && Array.isArray(fetchedUsers)) {
          for (const fetchedUser of fetchedUsers) {
            const normalized = normalizeUser(fetchedUser)
            const id = normalized.id
            usersCache.value.set(id, normalized) // сохраняем в кэш
            result.set(id, normalized) // добавляем в результат
          }
        }
      } catch (error) {
        console.error('Ошибка загрузки пользователей по IDs:', error)
        // Можно пробросить ошибку, но чтобы не ломать интерфейс, просто логируем
        // и возвращаем то, что удалось получить из кэша
      }
    }

    return result
  }

  function updateUserInCache(userId: string, updatedUser: User) {
    const id = String(userId)
    const normalized = normalizeUser(updatedUser)
    if (user.value?.id === id) {
      user.value = normalized
    }
    usersCache.value.set(id, normalized)
  }

  async function updateUserName(newUserName: string): Promise<User | null> {
    if (!user.value) return null
    loading.value = true
    try {
      const updatedUser = await userService.updateUserName(user.value.id, newUserName)
      updateUserInCache(updatedUser.id, updatedUser)
      return updatedUser
    } catch (error) {
      console.error('Ошибка обновления имени пользователя:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(profileData: Partial<User['profile']>): Promise<User | null> {
    if (!user.value) return null
    loading.value = true
    try {
      const data: UpdateUserProfileRequest = {
        firstName: profileData.firstName,
        lastName: profileData.lastName,
      }
      const updatedUser = await userService.updateProfile(user.value.id, data)
      updateUserInCache(updatedUser.id, updatedUser)
      return updatedUser
    } catch (error) {
      console.error('Ошибка обновления профиля:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function uploadAvatar(file: File): Promise<string | null> {
    if (!user.value) return null
    loading.value = true
    try {
      const avatarUrl = await userService.uploadAvatar(user.value.id, file)
      const updatedUser = {
        ...user.value,
        profile: {
          ...user.value.profile,
          avatarUrl,
        },
      }
      const normalized = normalizeUser(updatedUser)
      user.value = normalized
      usersCache.value.set(normalized.id, normalized)
      await refreshUser()
      return avatarUrl
    } catch (error) {
      console.error('Ошибка загрузки аватара:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function refreshUser(): Promise<User | null> {
    if (!token.value) return null
    loading.value = true
    try {
      const currentUser = await authService.getCurrentUser()
      if (currentUser) {
        const normalized = normalizeUser(currentUser)
        updateUserInCache(normalized.id, normalized)
        return normalized
      }
      return null
    } catch (error) {
      console.error('Ошибка обновления пользователя:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // ==================== Черный список (с приведением типов) ====================
  async function fetchBlacklistedUsers(): Promise<User[]> {
    loading.value = true
    try {
      const users = await userService.getBlacklistedUsers()
      const normalizedUsers = users.map((u) => normalizeUser(u))
      // Сохраняем в кеш
      for (const user of normalizedUsers) {
        usersCache.value.set(user.id, user)
      }
      return normalizedUsers
    } catch (error) {
      console.error('Ошибка загрузки заблокированных пользователей:', error)
      return []
    } finally {
      loading.value = false
    }
  }

  async function addToBlackList(userId: string): Promise<User | null> {
    if (!user.value) return null
    loading.value = true
    try {
      // blockedId может быть числом (Long)
      const data = await userService.addToBlackList(userId)
      const blockedIdStr = data.blockedUserId

      const currentBlackList = user.value.blackListIds || []
      if (!currentBlackList.includes(blockedIdStr)) {
        const updatedUser = {
          ...user.value,
          blackListIds: [...currentBlackList, blockedIdStr],
        }
        user.value = updatedUser
        usersCache.value.set(updatedUser.id, updatedUser)
      } else {
        usersCache.value.set(user.value.id, user.value)
      }
      return user.value
    } catch (error) {
      console.error('Ошибка добавления в черный список:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function removeFromBlackList(userId: string): Promise<User | null> {
    if (!user.value) return null
    loading.value = true
    try {
      await userService.removeFromBlackList(userId)
      const userIdStr = String(userId)
      const currentBlackList = user.value.blackListIds || []
      const updatedBlackList = currentBlackList.filter((id) => id !== userIdStr)
      const updatedUser = {
        ...user.value,
        blackListIds: updatedBlackList,
      }
      user.value = updatedUser
      usersCache.value.set(updatedUser.id, updatedUser)
      await fetchBlacklistedUsers()
      return user.value
    } catch (error) {
      console.error('Ошибка удаления из черного списка:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // ==================== Статусы пользователей ====================

  function updateUserStatus(
    userId: string,
    status: UserStatus['status'],
    lastSeen?: UserStatus['lastSeen'],
  ) {
    userStatuses.value[userId] = { userId, status, lastSeen }
  }

  function getUserStatus(userId: string): string {
    return userStatuses.value[userId]?.status ?? 'OFFLINE'
  }

  function getUserLastSeen(userId: string): string | null {
    return userStatuses.value[userId]?.lastSeen ?? null
  }

  function clearUsersCache() {
    usersCache.value.clear()
    userStatuses.value = {}
  }

  return {
    // Состояние
    user,
    token,
    isInitialized,
    loading,
    usersCache,
    userStatuses,

    // Геттеры
    fullName,
    isAuthenticated,

    // Действия
    login,
    register,
    deleteAccount,
    initAuth,
    logout,
    setSession,
    clearSession,

    // Методы для работы с пользователями
    fetchBlacklistedUsers,
    getUserFromCache,
    getUserStatus,
    getUserLastSeen,
    getUserById,
    getUsersByIds,
    updateUserStatus,
    clearUsersCache,

    // Черный список
    addToBlackList,
    removeFromBlackList,

    // Обновление профиля
    updateUserName,
    updateProfile,
    uploadAvatar,
    refreshUser,
    updateUserInCache,
  }
})
