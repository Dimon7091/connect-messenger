import type { UserService } from '../../modules/users/index.js';

export function createDeleteUserUseCase(deps: { userService: UserService }) {
  return {
    async softDelete(id: string): Promise<void> {
      await deps.userService.softDelete(id);
    },
  };
}
