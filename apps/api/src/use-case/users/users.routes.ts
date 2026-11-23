import type { FastifyPluginAsync } from 'fastify';
import {
  UpdateUserSchema,
  UserAdminResponseSchema,
  UserBaseResponseSchema,
  UserIdParamsSchema,
  UserNameQuerySchema,
  UserPrivateResponseSchema,
  UsersIdsSchema,
  UsersOffsetPaginationAdminResponse,
  UsersOffsetPaginationQuerySchema,
  UsersStatisticResponseSchema,
} from '@messenger/shared-schema';
import { createUpdateUserUseCase } from './update-user.use-case.js';
import { createGetUsersUseCase } from './get-users.use-case.js';
import type { ZodTypeProvider } from 'fastify-type-provider-zod';
import { createDeleteUserUseCase } from './delete-user.use-case.js';
import { createGetUsersStatisticUseCase } from './get-users-statistic.use-case.js';
import { createDeleteUserByAdminUseCase } from './delete-user-by-admin.use-case.js';
import z from 'zod';
import { createSearchUsersUseCase } from './search-users.use-case.js';
import { createGetUsersByIdsUseCase } from './get-users-by-ids.use-case.js';

export const usersRoutes: FastifyPluginAsync = async app => {
  // Инициализируем нужные Use Cases, передавая зависимости из Fastify
  const updateUserUseCase = createUpdateUserUseCase({ userService: app.userService });
  const getUsersUseCase = createGetUsersUseCase({ userService: app.userService });
  const getUsersByIdsUseCase = createGetUsersByIdsUseCase({ userService: app.userService });
  const getUsersStatisticUseCase = createGetUsersStatisticUseCase({ userService: app.userService });
  const deleteUserUserCase = createDeleteUserUseCase({ userService: app.userService });
  const deleteUserByAdminUseCase = createDeleteUserByAdminUseCase({ userService: app.userService });
  const searchUsersUseCase = createSearchUsersUseCase({ userService: app.userService });

  // Защищенные маршруты
  await app.register(async protectedRoute => {
    protectedRoute.addHook('onRequest', app.authenticate);

    // Приводим app к типу, который понимает Zod-провайдер
    const typedRoute = protectedRoute.withTypeProvider<ZodTypeProvider>();

    // POST
    // Мягкое удаление admin эндпоинт
    typedRoute.post(
      '/:id/anonymize',
      {
        preHandler: app.requireAdmin,
        schema: {
          params: UserIdParamsSchema,
        },
      },
      async (request, reply) => {
        const { id: userId } = request.params;
        const response = await deleteUserByAdminUseCase.softDelete(userId);
        return reply.code(200).send(response);
      }
    );

    // Получить пользователей по списку id
    typedRoute.post(
      '/batch',
      {
        schema: {
          body: UsersIdsSchema,
          response: { 200: z.array(UserBaseResponseSchema) },
        },
      },
      async (request, reply) => {
        const response = await getUsersByIdsUseCase.getUsers(request.body.ids);
        return reply.code(200).send(response);
      }
    );

    // GET
    // получение текущего пользователя
    typedRoute.get(
      '/me',
      {
        schema: {
          response: { 200: UserPrivateResponseSchema },
        },
      },
      async (request, reply) => {
        const userId = request.user.sub;
        const response = await getUsersUseCase.getById(userId);
        return reply.code(200).send(response);
      }
    );

    // Поиск пользователей (собеседника)
    typedRoute.get(
      '/search',
      {
        schema: {
          querystring: UserNameQuerySchema,
          response: { 200: z.array(UserBaseResponseSchema) },
        },
      },
      async (request, reply) => {
        const { username } = request.query;
        const response = await searchUsersUseCase.findUsers(username);
        return reply.code(200).send(response);
      }
    );

    // Список пользователей с пагинацией admin эндпоинт
    typedRoute.get(
      '/',
      {
        preHandler: app.requireAdmin,
        schema: {
          querystring: UsersOffsetPaginationQuerySchema,
          response: { 200: UsersOffsetPaginationAdminResponse },
        },
      },
      async (request, reply) => {
        const response = await getUsersUseCase.findPaginatedForAdmin(request.query);
        return reply.code(200).send(response);
      }
    );

    // Статистика пользователей admin эндпоинт
    typedRoute.get(
      '/statistic',
      {
        preHandler: app.requireAdmin,
        schema: {
          response: { 200: UsersStatisticResponseSchema },
        },
      },
      async (request, reply) => {
        const response = await getUsersStatisticUseCase.getUsersStatistic();
        return reply.code(200).send(response);
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

    // DELETE

    // Мягкое удаление своего аккаунта
    typedRoute.delete('/me', async (request, reply) => {
      const userId = request.user.sub;
      await deleteUserUserCase.softDelete(userId);
      return reply.code(204).send();
    });
  });
};
