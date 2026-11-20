import { httpClient } from './HttpClient';
import type {
  PaginatedResponse,
  UserAdminResponse,
  UsersStatisticResponse,
} from '@messenger/shared-schema';

export interface UserQueryParams {
  page?: number;
  limit?: number;
  search?: string;
  sortBy?: string;
  sortOrder?: string;
}

class UsersService {
  /** Получение списка пользователей с пагинацией, фильтрацией и сортировкой */
  async getUsers(params: UserQueryParams): Promise<PaginatedResponse<UserAdminResponse>> {
    const searchParams = new URLSearchParams();

    if (params.page !== undefined) searchParams.set('page', String(params.page));
    if (params.limit !== undefined) searchParams.set('limit', String(params.limit));
    if (params.search) searchParams.set('search', params.search);
    if (params.sortBy) searchParams.set('sortBy', params.sortBy);
    if (params.sortOrder) searchParams.set('sortOrder', params.sortOrder);

    const query = searchParams.toString();
    return httpClient.get<PaginatedResponse<UserAdminResponse>>(
      `/v1/users${query ? `?${query}` : ''}`
    );
  }

  /** Блокировка / разблокировка пользователя */
  async setUserBlock(userId: string, isBanned: boolean): Promise<UserAdminResponse> {
    return httpClient.patch<UserAdminResponse>(`/v1/admin/users/${userId}/block`, { isBanned });
  }

  /** Мягкое удаление пользователя */
  async deleteUser(userId: string): Promise<UserAdminResponse> {
    return httpClient.post<UserAdminResponse>(`/v1/users/${userId}/anonymize`);
  }

  /** Статистика пользователей (всего / онлайн) */
  async getUsersStatistic(): Promise<UsersStatisticResponse> {
    return httpClient.get<UsersStatisticResponse>('/v1/users/statistic');
  }
}

export const usersService = new UsersService();
