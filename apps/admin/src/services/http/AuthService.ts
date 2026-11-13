import { httpClient } from './HttpClient';
import type { AuthRequestDto, AuthResponse } from '@messenger/shared-schema';

class AuthService {
  async login(request: AuthRequestDto): Promise<AuthResponse> {
    return httpClient.post<AuthResponse>('/v1/auth/login', request);
  }

  async getCurrentUser(): Promise<AuthResponse['user']> {
    return httpClient.get<AuthResponse['user']>('/v1/users/me');
  }
}

export const authService = new AuthService();