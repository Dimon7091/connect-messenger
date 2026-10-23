import { httpClient } from './HttpClient'
import type {
  UpdateUserNameRequest,
  UpdateUserProfileRequest,
  User,
  UserBlockResponser,
  UsersGettingByIdsRequest,
} from '@/types'

class UserService {
  /** Получение профиля (теперь только сетевой запрос) */
  async getProfile(): Promise<User> {
    const response = await httpClient.get<User>('/v1/users/me')
    return response.data!
  }

  /** Обновление имя пользователя */
  async updateUserName(userId: string, newUserName: string): Promise<User> {
    const data: UpdateUserNameRequest = { username: newUserName }
    const response = await httpClient.patch<User>(`/v1/users/me/update-username`, data)
    return response.data!
  }

  /** Обновление профиля */
  async updateProfile(userId: string, data: UpdateUserProfileRequest): Promise<User> {
    const response = await httpClient.post<User>(`/v1/users/me/profile/update`, data)
    return response.data!
  }

  /** Получение списка контактов */
  async getContacts(): Promise<User[]> {
    const response = await httpClient.get<User[]>('/v1/users/contacts')
    return response.data!
  }

  /** Поиск (с использованием encodeURIComponent) */
  async searchUsers(query: string): Promise<User[]> {
    const response = await httpClient.get<User[]>(
      `/v1/users/search?username=${encodeURIComponent(query)}`,
    )
    return response.data!
  }

  async getUserById(userId: string): Promise<User> {
    const response = await httpClient.get<User>(`/v1/users/${userId}`)
    return response.data!
  }

  async getUsersById(data: UsersGettingByIdsRequest): Promise<User[]> {
    const response = await httpClient.post<User[]>(`/v1/users/batch-get`, data)
    return response.data!
  }

  /** Загрузка аватара */
  async uploadAvatar(userId: string, file: File): Promise<string> {
    const response = await httpClient.uploadFile<{ avatarUrl: string }>(
      `/v1/users/me/profile/upload-avatar`,
      file,
    )
    return response.data!.avatarUrl
  }

  /** Получить черный список пользователей */
  async getBlacklistedUsers(): Promise<User[]> {
    const response = await httpClient.get<User[]>(`/v1/users/me/blacklist`)
    return response.data!
  }

  /** Добавить в черный список пользователя */
  async addToBlackList(userId: string): Promise<UserBlockResponser> {
    const response = await httpClient.post<UserBlockResponser>(`/v1/users/me/blacklist/${userId}`)
    return response.data!
  }

  /** Удалить из черного списка */
  async removeFromBlackList(userId: string): Promise<string> {
    const response = await httpClient.delete<string>(`/v1/users/me/blacklist/${userId}`)
    return response.data!
  }

  async delete(): Promise<void> {
    await httpClient.delete('/v1/users/me')
  }
}

export const userService = new UserService()
