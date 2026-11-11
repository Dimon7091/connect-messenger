import { fastifyEnv } from '@fastify/env';
import type { FastifyPluginAsync } from 'fastify';
import fp from 'fastify-plugin';
import path from 'node:path';

type NodeEnv = 'development' | 'production' | 'test';

declare module 'fastify' {
  export interface FastifyInstance {
    config: {
      NODE_ENV: NodeEnv;
      GLOBAL_DOMAIN: string;
      API_PORT: number;
      API_HOST: string;
      DB_URL: string;
      DB_MAX_POOLS: number;
      JWT_SECRET: string;
      FASTIFY_CLOSE_GRACE_DELAY: number;
    };
  }
}

const baseProperties = {
  NODE_ENV: { type: 'string', enum: ['development', 'production', 'test'] },
  GLOBAL_DOMAIN: { type: 'string' },
  API_PORT: { type: 'integer', default: 8080 },
  API_HOST: { type: 'string', default: '0.0.0.0' },
  DB_URL: { type: 'string' },
  DB_MAX_POOLS: { type: ['integer', 'string'], default: 5 },
  JWT_SECRET: { type: 'string' },
  FASTIFY_CLOSE_GRACE_DELAY: { type: 'string' },
} as const;

const envPluginCore: FastifyPluginAsync = async fastify => {
  const mode = process.env.NODE_ENV;
  if (!mode || !['development', 'production', 'test'].includes(mode)) {
    throw new Error('NODE_ENV is required: development | production | test');
  }

  const isProd = mode === 'production';
  const envFilePath = path.resolve(process.cwd(), '../../.env');

  const schema = {
    type: 'object',
    properties: baseProperties,
    required: isProd ? ['NODE_ENV', 'GLOBAL_DOMAIN', 'DB_URL', 'JWT_SECRET'] : ['NODE_ENV'],
  };

  await fastify.register(fastifyEnv, {
    schema,
    data: process.env,
    dotenv: { path: envFilePath },
  });
};

export const envPlugin = fp(envPluginCore, { name: 'config' });
