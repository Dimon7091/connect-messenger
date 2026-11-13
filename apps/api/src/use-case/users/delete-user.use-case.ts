import type { UserService } from '../../modules/users/index.js';

export function createDeleteUserUseCase(deps: { userService: UserService }) {
  return {
    async softDelete(id: string): Promise<void> {
      return deps.userService.softDelete(id);
    },
  };
}
