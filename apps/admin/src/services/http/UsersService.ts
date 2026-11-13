import { httpClient } from './HttpClient';
import type { UserAdminResponse } from '@messenger/shared-schema';
import type { InvitationCountResponse, InvitationResponse } from '@messenger/shared-schema';

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: { sorted: boolean; unsorted: boolean; empty: boolean };
  numberOfElements: number;
  empty: boolean;
}

export interface UsersStat {
  totalUsers: number;
  onlineUsers: number;
}

export interface UpdateIsBannedUserRequest {
  isBlocked: boolean;
}

export interface UserQueryParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  sortBy?: string;
  sortDir?: string;
}

class UsersService {
  /** Получение списка пользователей с пагинацией, фильтрацией и сортировкой */
  async getUsers(params: UserQueryParams): Promise<PageResponse<UserAdminResponse>> {
    const searchParams = new URLSearchParams();

    if (params.page !== undefined) searchParams.set('page', String(params.page));
    if (params.size !== undefined) searchParams.set('size', String(params.size));
    if (params.search) searchParams.set('userName', params.search);
    if (params.status) searchParams.set('status', params.status);
    if (params.sortBy) searchParams.set('sortBy', params.sortBy);
    if (params.sortDir) searchParams.set('sortDir', params.sortDir);

    const query = searchParams.toString();
    return httpClient.get<PageResponse<UserAdminResponse>>(`/v1/admin/users${query ? `?${query}` : ''}`);
  }

  /** Блокировка / разблокировка пользователя */
  async setUserBlock(userId: string, isBanned: boolean): Promise<UserAdminResponse> {
    return httpClient.patch<UserAdminResponse>(`/v1/admin/users/${userId}/block`, { isBanned });
  }

  /** Мягкое удаление пользователя */
  async deleteUser(userId: string): Promise<UserAdminResponse> {
    return httpClient.patch<UserAdminResponse>(`/v1/admin/users/${userId}/delete`);
  }

  /** Статистика пользователей (всего / онлайн) */
  async getUsersStat(): Promise<UsersStat> {
    return httpClient.get<UsersStat>('/v1/admin/users/stat');
  }
}

export const usersService = new UsersService();