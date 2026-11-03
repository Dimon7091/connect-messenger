import { createDb, type DbOptions, type DbInstance } from '@messenger/database';
import fp from 'fastify-plugin';
import type { FastifyPluginAsync } from 'fastify';

declare module 'fastify' {
  interface FastifyInstance {
    db: DbInstance;
  }
}

const dbPluginCore: FastifyPluginAsync<DbOptions> = async (fastify, options) => {
  const { db, close } = await createDb(options);

  fastify.decorate('db', db as DbInstance);

  fastify.addHook('onClose', async () => {
    await close();
  });
};

export const dbPlugin = fp(dbPluginCore);
