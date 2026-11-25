// types/fastify.d.ts
import 'fastify';
import type StompServer from 'stomp-broker-js';

export interface StompHandlers {
  async handle(
    event: { type: string; payload: unknown },
    headers: Record<string, unknown>,
    stomp: StompServer,
  ): Promise<void>;
}

declare module 'fastify' {
  interface FastifyInstance {
    stomp: StompServer;
    stompSessions: Map<string, string>;
    stompHandlers: StompHandlers;
  }
}
