// services/http/AuthService.ts
import { httpClient } from './HttpClient';
import type { LoginResponse } from '@/types';
import type { AuthRequestDto, AuthResponse, CreateUserDto, User } from '@messenger/shared-schema';

class AuthService {
  async login(request: AuthRequestDto): Promise<AuthResponse> {
    const response = await httpClient.post<AuthResponse>('/v1/auth/login', request);
    return response.data!; // Возвращает { user: User, token: string }
  }

  async register(request: CreateUserDto): Promise<AuthResponse> {
    const response = await httpClient.post<AuthResponse>('/v1/auth/register', request);
    return response.data!;
  }

  async getCurrentUser(): Promise<User> {
    const response = await httpClient.get<User>('/v1/users/me');
    return response.data!;
  }
}

export const authService = new AuthService();
