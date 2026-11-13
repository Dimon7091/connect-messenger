import type { User } from '@messenger/database';
import type { UserService } from '../../modules/users/index.js';
import type { UserPrivateResponse } from '@messenger/shared-schema';

export interface GetUserUseCase {
  userService: UserService;
}

export function createGetUserUseCase(deps: { userService: UserService }) {
  // ==============================================
  // TODO: временная заглушка для поля blackListIds
  // ==============================================
  function addMissingFields(user: User) {
    return { blackListIds: [], ...user };
  }

  return {
    async getById(id: string): Promise<UserPrivateResponse> {
      const user = await deps.userService.getById(id);
      return addMissingFields(user);
    },
  };
}
