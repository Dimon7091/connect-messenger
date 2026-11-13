import type { UpdateUserDto, UserPrivateResponse } from '@messenger/shared-schema';
import type { UserService } from '../../modules/users/index.js';
import type { User } from '@messenger/database';

export function createUpdateUserUseCase(deps: { userService: UserService }) {
  // ==============================================
  // TODO: временная заглушка для поля blackListIds
  // ==============================================
  function addMissingFields(user: User) {
    return { blackListIds: [], ...user };
  }

  return {
    async update(id: string, dto: UpdateUserDto): Promise<UserPrivateResponse> {
      const updatedUser = await deps.userService.update(id, dto);
      return addMissingFields(updatedUser);
    },
  };
}
