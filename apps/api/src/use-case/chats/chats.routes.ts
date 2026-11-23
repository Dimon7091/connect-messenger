import {
  ChatHistoryQuerySchema,
  ChatIdParamSchema,
  ChatResponseSchema,
  CreateOrGetChatSchema,
  MessageResponseSchema,
} from '@messenger/shared-schema';
import type { FastifyPluginAsync } from 'fastify';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';
import { createGetOrCreateUseCase } from './create-or-get-chat.use-case.js';
import z from 'zod';
import { createGetUserChatsUseCase } from './get-user-chats.use-case.js';
import { createGetChatHistoryUseCase } from './get-chat-history.use-case.js';

export const chatsRoutes: FastifyPluginAsync = async app => {
  // Инициализируем нужные Use Cases, передавая зависимости из Fastify
  const getOrCreateChatUseCase = createGetOrCreateUseCase({ chatService: app.chatService });
  const getUserChatsUseCase = createGetUserChatsUseCase({ chatService: app.chatService });
  const getChatHistoryUseCase = createGetChatHistoryUseCase({ messageService: app.messageService });

  // Защищенные маршруты
  await app.register(async protectedRoute => {
    protectedRoute.addHook('onRequest', app.authenticate);

    // Приводим app к типу, который понимает Zod-провайдер
    const typedRoute = protectedRoute.withTypeProvider<ZodTypeProvider>();

    // POST /chats — Создание чата
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

    // GET /chats — Получить все чаты пользователя
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

    // ----- Маршруты сообщений -----

    // GET /chats/:chatId/messages — Получить историю сообщений
    typedRoute.get(
      '/:chatId/messages',
      {
        schema: {
          params: ChatIdParamSchema,
          querystring: ChatHistoryQuerySchema,
          response: { 200: z.array(MessageResponseSchema) },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const { chatId } = request.params;
        const { limit } = request.query;
        const { beforeTimestamp } = request.query;

        const response = await getChatHistoryUseCase.getChatHistory({
          chatId,
          userId,
          beforeTimestamp,
          limit,
        });
        return reply.code(200).send(response);
      }
    );

    // typedRoute.delete(
    //   '/:chatId/messages',
    //   {
    //     schema: {
    //       params: ChatParamsSchema,
    //       body: z
    //         .object({
    //           messageIds: z.array(z.string().uuid()).min(1).optional(),
    //           forAll: z.boolean().default(false),
    //         })
    //         .optional(),
    //       response: { 204: z.null() },
    //     },
    //   },
    //   async (request, reply) => {
    //     const userId = request.user.sub;
    //     const { chatId } = request.params;
    //     const { messageIds, forAll } = request.body ?? {};

    //     // await deleteMessagesUseCase.execute({ userId, chatId, messageIds, forAll });
    //     return reply.code(204).send();
    //   }
    // );
  });
};
