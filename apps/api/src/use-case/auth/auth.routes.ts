import type { FastifyPluginAsync } from 'fastify';
import { createAuthUseCase } from './auth.use-case.js';
import {
  AuthRequestSchema,
  AuthResponseSchema,
  CreateUserSchema,
  type AuthRequestDto,
  type AuthResponse,
  type CreateUserDto,
} from '@messenger/shared-schema';

export const authRouter: FastifyPluginAsync = async app => {
  const authUseCase = createAuthUseCase({
    userService: app.userService,
    jwtTokenService: {
      generate: payload => app.jwt.sign(payload),
    },
    hashService: app.hashService,
  });

  // POST регистрация нового пользователя
  app.post<{ Body: CreateUserDto; Reply: AuthResponse }>(
    '/register',
    {
      schema: {
        body: CreateUserSchema,
        response: { 201: AuthResponseSchema },
      },
    },
    async (request, reply) => {
      const newUser = await authUseCase.register(request.body);
      return reply.code(201).send(newUser);
    }
  );

  app.post<{ Body: AuthRequestDto; Reply: AuthResponse }>(
    '/login',
    {
      schema: {
        body: AuthRequestSchema,
        response: { 200: AuthResponseSchema },
      },
    },
    async (request, reply) => {
      const newUser = await authUseCase.login(request.body);
      return reply.code(200).send(newUser);
    }
  );
};
