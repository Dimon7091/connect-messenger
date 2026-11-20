import type { PublicUser } from '@messenger/database';
import type { UserService } from '../../modules/users/index.js';
import type {
  PaginatedResponse,
  UserAdminResponse,
  UserPrivateResponse,
  UsersOffsetPaginationQuery,
} from '@messenger/shared-schema';

export interface GetUserUseCase {
  userService: UserService;
}

export function createGetUsersUseCase(deps: { userService: UserService }) {
  // ==============================================
  // TODO: временная заглушка для недостающих полей UserAdminResponse
  // ==============================================
  function toAdminResponse(user: PublicUser): UserAdminResponse {
    return {
      ...user,
      lastSeen: user.createdAt,
      status: 'OFFLINE',
    };
  }

  return {
    async getById(id: string): Promise<UserPrivateResponse> {
      const user = await deps.userService.getById(id);
      // TODO: временная заглушка черного списка
      return { blackListIds: [], ...user };
    },

    async findPaginatedForAdmin(
      query: UsersOffsetPaginationQuery
    ): Promise<PaginatedResponse<UserAdminResponse>> {
      const result = await deps.userService.findPaginated(query);

      const adminUsers = result.items.map(toAdminResponse);
      return { items: adminUsers, meta: result.meta };
    },
  };
}
