import Fastify from 'fastify';
import {
  serializerCompiler,
  validatorCompiler,
  type ZodTypeProvider,
} from 'fastify-type-provider-zod';
import { dbPlugin } from './plugins/dbPlugin.js';
import { envPlugin } from './plugins/envPlugin.js';
import jwtPlugin from './plugins/jwt.js';
import { errorHandlerPlugin } from './plugins/errorHandler.js';
import { runMigrations, type DbInstance, type DbOptions } from '@messenger/database';
import fastifyRateLimit from '@fastify/rate-limit';
import { usersRoutes } from './use-case/users/users.routes.js';
import { authRoutes } from './use-case/auth/auth.routes.js';
import { hashPlugin } from './plugins/hash.plugin.js';
import { usersPlugin } from './modules/users/users.plugin.js';
import { invitePlugin } from './modules/invite/invite.plugin.js';
import { invitationRouters } from './use-case/invite/invitation.routes.js';
import { seedAdminPlugin } from './modules/users/seedAdmin.plugin.js';

export interface AppOptions {
  logger?: boolean | object;
}

function getLoggerOptions(): object | boolean {
  if (process.stdout.isTTY) {
    return {
      level: 'info',
      transport: {
        target: 'pino-pretty',
        options: { translateTime: 'HH:MM:ss Z', ignore: 'pid,hostname' },
      },
    };
  }
  return { level: process.env.LOG_LEVEL ?? 'silent' };
}

export async function buildApp(options: AppOptions = {}) {
  const app = Fastify({
    logger: options.logger ?? getLoggerOptions(),
    connectionTimeout: 120_000,
    requestTimeout: 60_000,
    keepAliveTimeout: 10_000,
    http: { headersTimeout: 15_000 },
    ajv: {
      customOptions: {
        coerceTypes: 'array',
        removeAdditional: 'all',
      },
    },
  }).withTypeProvider<ZodTypeProvider>();

  app.setValidatorCompiler(validatorCompiler);
  app.setSerializerCompiler(serializerCompiler);

  await app.register(envPlugin);

  // Инициализация базы данных в зависимости от режима
  const isProd = app.config.NODE_ENV === 'production';
  const dbOptions: DbOptions =
    app.config.NODE_ENV === 'production'
      ? {
          driver: 'postgres',
          connectionString: app.config.DB_URL!,
          maxPool: app.config.DB_MAX_POOLS,
        }
      : app.config.NODE_ENV === 'test'
        ? {
            driver: 'pglite',
            dataDir: 'memory://', // Для тестов инициализируем в памяти
          }
        : {
            driver: 'pglite',
            dataDir: './.pgdata', // Для dev — на диске
          };

  await app.register(dbPlugin, dbOptions);

  // Миграции для dev и test
  if (!isProd) {
    await app.after();
    await runMigrations(app.db);
  }

  // rate-limit ДО errorHandlerPlugin
  await app.register(fastifyRateLimit, { global: true });
  await app.register(errorHandlerPlugin);

  await app.register(jwtPlugin);
  await app.register(hashPlugin);

  // Модули
  await app.register(usersPlugin);
  await app.register(invitePlugin);

  if (app.config.NODE_ENV !== 'test') {
    await app.register(seedAdminPlugin);
  }

  app.get('/health', async () => {
    return { status: 'ok', timeStamp: new Date().toISOString() };
  });

  await app.register(authRoutes, { prefix: '/api/v1/auth' });
  await app.register(usersRoutes, { prefix: '/api/v1/users' });
  await app.register(invitationRouters, { prefix: '/api/v1/invitations' });

  return app;
}
