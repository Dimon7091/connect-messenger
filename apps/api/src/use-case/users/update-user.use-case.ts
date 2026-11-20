import type {
  UpdateUserDto,
  UserAdminResponse,
  UserPrivateResponse,
} from '@messenger/shared-schema';
import type { UserService } from '../../modules/users/index.js';
import type { PublicUser } from '@messenger/database';

export function createUpdateUserUseCase(deps: { userService: UserService }) {
  // ==============================================
  // TODO: временная заглушка для недостающих полей UserAdminResponse
  // =============================================
  return {
    async update(id: string, dto: UpdateUserDto): Promise<UserPrivateResponse> {
      const updatedUser = await deps.userService.update(id, dto);
      return { blackListIds: [], ...updatedUser };
    },
  };
}
