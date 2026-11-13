import type { FastifyPluginAsync } from 'fastify';
import { createAuthUseCase } from './auth.use-case.js';
import { AuthRequestSchema, AuthResponseSchema, CreateUserSchema } from '@messenger/shared-schema';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';

export const authRoutes: FastifyPluginAsync = async app => {
  const authUseCase = createAuthUseCase({
    userService: app.userService,
    inviteService: app.inviteService,
    jwtTokenService: {
      generate: payload => app.jwt.sign(payload),
    },
    hashService: app.hashService,
  });

  const typedApp = app.withTypeProvider<ZodTypeProvider>();

  // POST регистрация нового пользователя
  typedApp.post(
    '/register',
    {
      schema: {
        body: CreateUserSchema,
        response: { 201: AuthResponseSchema },
      },
    },
    async (request, reply) => {
      const response = await authUseCase.register(request.body);
      return reply.code(201).send(response);
    }
  );

  typedApp.post(
    '/login',
    {
      schema: {
        body: AuthRequestSchema,
        response: { 200: AuthResponseSchema },
      },
    },
    async (request, reply) => {
      const response = await authUseCase.login(request.body);
      return reply.code(200).send(response);
    }
  );
};
