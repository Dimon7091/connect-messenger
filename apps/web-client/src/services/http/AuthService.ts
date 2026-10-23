// services/http/AuthService.ts
import { httpClient } from './HttpClient'
import type { User, LoginResponse } from '@/types'

class AuthService {
  async login(username: string, password: string): Promise<LoginResponse> {
    const response = await httpClient.post<LoginResponse>('/v1/auth/login', { username, password })
    return response.data! // Возвращает { user: User, token: string }
  }

  async register(data: {
    userName: string
    password: string
    firstName?: string
    lastName?: string
  }) {
    const response = await httpClient.post<{ user: User; token: string }>('/v1/auth/register', data)
    return response.data!
  }

  async getCurrentUser(): Promise<User> {
    const response = await httpClient.get<User>('/v1/users/me')
    return response.data!
  }
}

export const authService = new AuthService()
