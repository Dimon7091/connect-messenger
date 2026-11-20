import type { PublicUser } from '@messenger/database';
import type { UserService } from '../../modules/users/index.js';
import type { UserPrivateResponse, UsersStatisticResponse } from '@messenger/shared-schema';

export interface GetUserUseCase {
  userService: UserService;
}

export function createGetUsersStatisticUseCase(deps: { userService: UserService }) {
  return {
    async getUsersStatistic(): Promise<UsersStatisticResponse> {
      const total = await deps.userService.countAllUsers();
      // ==============================================
      // TODO: Временная заглушка для онлайн статистики
      // ==============================================
      const online = 1;
      const deleted = await deps.userService.countDeletedUsers();
      const active = total - deleted;
      const result: UsersStatisticResponse = {
        totalUsers: total,
        activeUsers: active,
        onlineUsers: online,
        deletedUsers: deleted,
      };
      return result;
    },
  };
}
