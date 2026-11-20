import type { UserAdminResponse } from '@messenger/shared-schema';
import type { UserService } from '../../modules/users/index.js';
import type { PublicUser } from '@messenger/database';

export function createDeleteUserByAdminUseCase(deps: { userService: UserService }) {
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
    async softDelete(id: string): Promise<UserAdminResponse> {
      const deletedUser = await deps.userService.softDelete(id);
      return toAdminResponse(deletedUser);
    },
  };
}
