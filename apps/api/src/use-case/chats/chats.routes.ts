import { ChatResponseSchema, CreateOrGetChatSchema } from '@messenger/shared-schema';
import type { FastifyPluginAsync } from 'fastify';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';
import { createGetOrCreateUseCase } from './create-or-get-chat.use-case.js';
import z from 'zod';
import { createGetUserChatsUseCase } from './get-user-chats.use-case.js';

export const chatsRoutes: FastifyPluginAsync = async app => {
  // Инициализируем нужные Use Cases, передавая зависимости из Fastify
  const getOrCreateChatUseCase = createGetOrCreateUseCase({ chatService: app.chatService });
  const getUserChatsUseCase = createGetUserChatsUseCase({ chatService: app.chatService });

  // Защищенные маршруты
  await app.register(async protectedRoute => {
    protectedRoute.addHook('onRequest', app.authenticate);

    // Приводим app к типу, который понимает Zod-провайдер
    const typedRoute = protectedRoute.withTypeProvider<ZodTypeProvider>();

    // POST
    // Создание чата
    typedRoute.post(
      '/',
      {
        schema: {
          body: CreateOrGetChatSchema,
          response: { 201: ChatResponseSchema },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const response = await getOrCreateChatUseCase.getChat(userId, request.body.companionId);
        return reply.code(201).send(response);
      }
    );

    // GET
    // Получить все чаты пользователя
    typedRoute.get(
      '/',
      {
        schema: {
          response: { 200: z.array(ChatResponseSchema) },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const response = await getUserChatsUseCase.getChats(userId);
        return reply.code(200).send(response);
      }
    );
  });
};
