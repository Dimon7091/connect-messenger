import fp from 'fastify-plugin';
import { createTxManager } from '../../infra/tx-manager.ts.js';
import { createMessageService, type MessageService } from './message.service.js';
import { createMessageRepository } from './message.repository.js';

declare module 'fastify' {
  interface FastifyInstance {
    messageService: MessageService;
  }
}

export const messagePlugin = fp(async app => {
  const txManager = createTxManager(app.db);
  const messageRepository = createMessageRepository(txManager.getDb);
  const messageService = createMessageService(messageRepository);

  app.decorate('messageService', messageService);
});
