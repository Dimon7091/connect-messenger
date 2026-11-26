import type { StompBroker } from '../plugins/stompPlugin.js'; // поправьте путь, если нужно

declare module 'fastify' {
  interface FastifyInstance {
    stomp: StompBroker;
    stompHandlers: {
      handle: (
        event: { type: string; payload: unknown },
        headers: Record<string, unknown>,
        stomp: StompBroker
      ) => Promise<void>;
    };
  }
}
