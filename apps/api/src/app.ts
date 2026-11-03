import Fastify from 'fastify';
import {
  serializerCompiler,
  validatorCompiler,
  type ZodTypeProvider,
} from 'fastify-type-provider-zod';
import { dbPlugin } from './plugins/dbPlugin.js';
import type { DbInstance, DbOptions } from '@messenger/database';
import { envPlugin } from './plugins/envPlugin.js';
import fastifyRateLimit from '@fastify/rate-limit';
import { errorHandlerPlugin } from './plugins/errorHandler.js';
import jwtPlugin from './plugins/jwt.js';

export interface AppOptions {
  db?: DbInstance;
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

export async function buildApp(overrideOptions: AppOptions = {}) {
  const app = Fastify({
    logger: overrideOptions.logger ?? getLoggerOptions(),
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

  // db: тесты передают готовый, dev/prod — поднимаем через плагин
  if (overrideOptions.db) {
    app.decorate('db', overrideOptions.db);
  } else {
    const dbOptions: DbOptions =
      app.config.NODE_ENV === 'production'
        ? {
            driver: 'postgres',
            connectionString: app.config.DB_URL!,
            maxPool: app.config.DB_MAX_POOLS,
          }
        : {
            driver: 'pglite',
            dataDir: './.pgdata',
          };

    await app.register(dbPlugin, dbOptions);
  }

  // rate-limit ДО errorHandlerPlugin
  await app.register(fastifyRateLimit, { global: true });
  await app.register(errorHandlerPlugin);
  await app.register(jwtPlugin);

  app.get('/health', async () => {
    return { status: 'ok', timeStamp: new Date().toISOString() };
  });

  return app;
}
