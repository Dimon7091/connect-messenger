import type { InvitationCountResponse } from '@messenger/shared-schema';
import type { InviteService } from '../../modules/invite/index.js';

export function createGetActiveCountInvitationUseCase(deps: { inviteService: InviteService }) {
  return {
    async getActiveCount() {
      const activeInvitationsCount = await deps.inviteService.getActiveCount();
      return { activeInvitationsCount } as InvitationCountResponse;
    },
  };
}
