import type { FastifyPluginAsync } from 'fastify';
import { InvitationResponseSchema, InvitationsCountResponseSchema } from '@messenger/shared-schema';
import { createCreateInvitationUseCase } from './create-invitation.use-case.js';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';
import { createGetActiveCountInvitationUseCase } from './get-active-invitations.use-case.js';

export const invitationRouters: FastifyPluginAsync = async app => {
  // Инициализируем нужные Use Cases, передавая зависимости из Fastify
  const createInvitationUseCase = createCreateInvitationUseCase({
    inviteService: app.inviteService,
    globalDomain: app.config.GLOBAL_DOMAIN,
  });

  const getActiveCountInvitationsUseCase = createGetActiveCountInvitationUseCase({
    inviteService: app.inviteService,
  });

  // Защищенные маршруты c ролью администратора
  await app.register(async protectedRoute => {
    protectedRoute.addHook('onRequest', app.authenticate);
    protectedRoute.addHook('onRequest', app.requireAdmin);

    const typedRoute = protectedRoute.withTypeProvider<ZodTypeProvider>();

    // POST создание приглашения
    typedRoute.post(
      '/',
      {
        schema: {
          response: { 201: InvitationResponseSchema },
        },
      },
      async (request, reply) => {
        const response = await createInvitationUseCase.createInvitation();
        return reply.code(201).send(response);
      }
    );

    // GET получение количества активных приглашений
    typedRoute.get(
      '/',
      {
        schema: {
          response: { 200: InvitationsCountResponseSchema },
        },
      },
      async (request, reply) => {
        const response = await getActiveCountInvitationsUseCase.getActiveCount();
        return reply.code(200).send(response);
      }
    );
  });
};
