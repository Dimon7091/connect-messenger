import type { FastifyPluginAsync } from 'fastify';
import { UpdateUserSchema, UserPrivateResponseSchema } from '@messenger/shared-schema';
import { createUpdateUserUseCase } from './update-user.use-case.js';
import { createGetUserUseCase } from './get-user.use-case.js';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';
import { createDeleteUserUseCase } from './delete-user.use-case.js';

export const usersRoutes: FastifyPluginAsync = async app => {
  // Инициализируем нужные Use Cases, передавая зависимости из Fastify
  const updateUserUseCase = createUpdateUserUseCase({ userService: app.userService });
  const getUserUseCase = createGetUserUseCase({ userService: app.userService });
  const deleteUserUserCase = createDeleteUserUseCase({ userService: app.userService });

  // Защищенные маршруты
  await app.register(async protectedRoute => {
    protectedRoute.addHook('onRequest', app.authenticate);

    // Приводим app к типу, который понимает Zod-провайдер
    const typedRoute = protectedRoute.withTypeProvider<ZodTypeProvider>();

    // GET получение текущего пользователя
    typedRoute.get(
      '/me',
      {
        schema: {
          response: { 200: UserPrivateResponseSchema },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const currentUser = await getUserUseCase.getById(userId);
        return reply.code(200).send(currentUser);
      }
    );

    // PATCH обновление текущего пользователя
    typedRoute.patch(
      '/me',
      {
        schema: {
          body: UpdateUserSchema,
          response: { 200: UserPrivateResponseSchema },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const response = await updateUserUseCase.update(userId, request.body);
        return reply.code(200).send(response);
      }
    );

    // DELETE мягкое удаление
    typedRoute.delete('/me', async (request, reply) => {
      const userId = request.user.sub;
      await deleteUserUserCase.softDelete(userId);
      return reply.code(204).send();
    });
  });
};
