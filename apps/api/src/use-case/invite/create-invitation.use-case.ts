import type { InvitationResponse } from '@messenger/shared-schema';
import type { InviteService } from '../../modules/invite/index.js';

export function createCreateInvitationUseCase(deps: {
  inviteService: InviteService;
  globalDomain: string;
}) {
  return {
    async createInvitation() {
      const invitation = await deps.inviteService.create();

      // Создание ссылки приглашения для клиента
      const normalizeDomainUrl = deps.globalDomain.trim().replace(/\/+$/, '');
      const query = `?open=register&invitationToken=${invitation.token}`;
      return { invitationUrl: `${normalizeDomainUrl}/login${query}` } as InvitationResponse;
    },
  };
}
