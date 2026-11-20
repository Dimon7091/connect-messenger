import fp from 'fastify-plugin';
import { createChatRepository } from './chat.repository.js';
import { createChatService, type ChatService } from './chat.service.js';
import { createTxManager } from '../../infra/tx-manager.ts.js';

declare module 'fastify' {
  interface FastifyInstance {
    chatService: ChatService;
  }
}

export const chatPlugin = fp(async app => {
  const txManager = createTxManager(app.db);
  const chatRepository = createChatRepository(txManager.getDb);
  const chatService = createChatService(chatRepository);

  app.decorate('chatService', chatService);
});
