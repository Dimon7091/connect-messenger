import fp from 'fastify-plugin';
import { createChatRepository } from './chat.repository.js';
import { createChatService, type ChatService } from './chat.service.js';

declare module 'fastify' {
  interface FastifyInstance {
    chatService: ChatService;
  }
}

export const invitePlugin = fp(async app => {
  const chatRepository = createChatRepository(app.db);
  const chatService = createChatService(chatRepository);

  app.decorate('chatService', chatService);
});
