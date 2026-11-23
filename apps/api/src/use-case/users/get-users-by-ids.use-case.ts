import type { PublicUser } from '@messenger/database';
import type { UserService } from '../../modules/users/index.js';
import type { UserBaseResponse, UserPrivateResponse } from '@messenger/shared-schema';

export interface GetUserUseCase {
  userService: UserService;
}

export function createGetUsersByIdsUseCase(deps: { userService: UserService }) {
  return {
    async getUsers(ids: string[]): Promise<UserBaseResponse[]> {
      return deps.userService.getByIds(ids);
    },
  };
}
