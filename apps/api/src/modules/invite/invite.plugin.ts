import fp from 'fastify-plugin';
import { createInviteService, type InviteService } from './invite.service.js';
import { createInviteRepository } from './invite.repository.js';

declare module 'fastify' {
  interface FastifyInstance {
    inviteService: InviteService;
  }
}

export const invitePlugin = fp(async app => {
  const inviteRepository = createInviteRepository(app.db);
  const inviteService = createInviteService(inviteRepository);

  app.decorate('inviteService', inviteService);
});
