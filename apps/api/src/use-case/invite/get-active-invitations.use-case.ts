import type { InvitationCountResponse } from '@messenger/shared-schema';
import type { InviteService } from '../../modules/invite/index.js';

export function createGetActiveCountInvitationUseCase(deps: { inviteService: InviteService }) {
  return {
    async getActiveCount(): Promise<InvitationCountResponse> {
      const activeInvitationsCount = await deps.inviteService.getActiveCount();
      return { activeInvitationsCount };
    },
  };
}
