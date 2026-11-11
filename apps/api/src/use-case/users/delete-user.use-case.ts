import type { UserService } from '../../modules/users/index.js';

export function createDeleteUserUseCase(deps: { userService: UserService }) {
  return {
    async softDelete(id: string) {
      return deps.userService.softDelete(id);
    },
  };
}
