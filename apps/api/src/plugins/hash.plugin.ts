import { scryptSync, randomBytes, timingSafeEqual } from 'node:crypto';
import fp from 'fastify-plugin';
import type { FastifyPluginAsync } from 'fastify';

declare module 'fastify' {
  interface FastifyInstance {
    hashService: HashService;
  }
}

export interface HashService {
  hash(password: string): string;
  compare(password: string, hash: string): boolean;
}

export const hashService: HashService = {
  hash(password: string): string {
    const salt = randomBytes(16).toString('hex');
    const derivedKey = scryptSync(password, salt, 64).toString('hex');
    return `${salt}:${derivedKey}`;
  },

  compare(password: string, passwordHash: string): boolean {
    const [salt, hashKey] = passwordHash.split(':');
    if (!salt || !hashKey) return false;

    const currentKey = scryptSync(password, salt, 64);
    const targetKey = Buffer.from(hashKey, 'hex');

    // Защита от атак по времени (Timing Attacks)
    return timingSafeEqual(currentKey, targetKey);
  },
};

export const hashPlugin: FastifyPluginAsync = fp(async fastify => {
  fastify.decorate('hashService', hashService);
});
