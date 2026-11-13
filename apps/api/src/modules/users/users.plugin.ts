// modules/users/users.plugin.ts
import fp from 'fastify-plugin';
import { createUserRepository } from './user.repository.js';
import { createUserService, type UserService } from './user.service.js';

declare module 'fastify' {
  interface FastifyInstance {
    userService: UserService;
  }
}

export const usersPlugin = fp(async app => {
  const userRepository = createUserRepository(app.db);
  const userService = createUserService(userRepository);

  app.decorate('userService', userService);
});
