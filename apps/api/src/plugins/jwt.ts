import fp from 'fastify-plugin';
import fjwt from '@fastify/jwt';
import type { FastifyReply, FastifyRequest } from 'fastify';
import { UnauthorizedError, ForbiddenError } from '../lib/errors.js';

declare module 'fastify' {
  interface FastifyInstance {
    authenticate: (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
    requireAdmin: (request: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }
}

declare module '@fastify/jwt' {
  interface FastifyJWT {
    payload: { sub: string; preferred_username: string; role: 'admin' | 'user' };
    user: { sub: string; preferred_username: string; role: 'admin' | 'user' };
  }
}

export default fp(
  async app => {
    await app.register(fjwt, {
      secret: app.config.JWT_SECRET,
      sign: { expiresIn: '1h' },
    });

    // Проверяет, что токен валиден. Кладёт payload в request.user.
    app.decorate('authenticate', async function (request: FastifyRequest, _reply: FastifyReply) {
      try {
        await request.jwtVerify();
      } catch (err) {
        const name = (err as Error).name;
        if (name === 'TokenExpiredError') throw new UnauthorizedError('Token expired');
        throw new UnauthorizedError('Invalid or missing token');
      }
    });

    // Вызывать ПОСЛЕ authenticate. Проверяет роль admin.
    app.decorate('requireAdmin', async function (request: FastifyRequest, _reply: FastifyReply) {
      const user = request.user as { role?: string } | undefined;
      if (user?.role !== 'admin') throw new ForbiddenError('Admin only');
    });
  },
  { name: 'jwt', dependencies: ['config'] }
);
