import type { InviteService } from '../../modules/invite/index.js';

export function createDeleteInvitationsUseCase(deps: { inviteService: InviteService }) {
  return {
    async deleteAll(): Promise<void> {
      await deps.inviteService.deleteAll();
    },
  };
}
