import type { UserService } from '../../modules/users/index.js';
import type { UserBaseResponse } from '@messenger/shared-schema';

export interface SearchUsersUseCase {
  userService: UserService;
}

export function createSearchUsersUseCase(deps: { userService: UserService }) {
  return {
    async findUsers(userName: string): Promise<UserBaseResponse[]> {
      return deps.userService.findByUserNameStartingWith(userName);
    },
  };
}
