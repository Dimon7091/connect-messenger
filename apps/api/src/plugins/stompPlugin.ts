// plugins/stompPlugin.ts
import fp from 'fastify-plugin';
import StompServer from 'stomp-broker-js';
import type { FastifyInstance } from 'fastify';
import http from 'node:http';

export default fp(
  async (app: FastifyInstance) => {
    const stompHttpServer = http.createServer();

    const stomp = new StompServer({
      server: stompHttpServer,
      path: '/ws',
      protocol: 'sockjs',
      heartbeat: [10000, 10000],
    });

    app.decorate('stomp', stomp);
    app.decorate('stompSessions', new Map<string, string>());

    // Заглушка для stompHandlers — пока нет бизнес-логики
    app.decorate('stompHandlers', {
      async handle(
        _event: { type: string; payload: unknown },
        _headers: Record<string, unknown>,
        _stomp: StompServer,
      ): Promise<void> {
        app.log.warn({ event: _event.type }, 'STOMP: no handler registered yet');
      },
    });

    // Мидлвара для аутентификации: проверяем токен при CONNECT-фрейме
    stomp.addMiddleware('connect', (
      socket: { sessionId: string },
      args: { headers: Record<string, string | undefined> },
      next: () => boolean,
    ) => {
      const token = args.headers['Auth-Token'];
      app.log.info({ sessionId: socket.sessionId }, 'STOMP: connect middleware');

      if (!token) {
        app.log.warn({ sessionId: socket.sessionId }, 'STOMP: no Auth-Token in CONNECT');
        return false;
      }

      try {
        const decoded = app.jwt.verify(token) as { sub: string };
        app.stompSessions.set(socket.sessionId, decoded.sub);
        app.log.info({ sessionId: socket.sessionId, userId: decoded.sub }, 'STOMP: auth OK');
        return next();
      } catch (err) {
        app.log.warn({ err, sessionId: socket.sessionId }, 'STOMP: auth failed — invalid token');
        return false;
      }
    });

    stomp.on('connected', (sessionId: string, headers: Record<string, string | undefined>) => {
      app.log.info({ sessionId, headers }, 'STOMP: client connected');
    });

    stomp.on('disconnected', (sessionId: string) => {
      const userId = app.stompSessions.get(sessionId);
      app.stompSessions.delete(sessionId);
      app.log.info({ sessionId, userId }, 'STOMP: disconnected');
    });

    stomp.subscribe('/**', async (msg, headers) => {
      const destination = String(headers.destination ?? '');
      if (destination.startsWith('/user/') || destination.startsWith('/topic/')) return;

      let event: { type: string; payload: unknown };
      try {
        event = JSON.parse(msg);
      } catch {
        app.log.warn({ msg, destination }, 'STOMP: invalid JSON');
        return;
      }

      if (typeof event.type !== 'string') {
        app.log.warn({ event }, 'STOMP: missing type');
        return;
      }

      try {
        await app.stompHandlers.handle(event, headers, stomp);
      } catch (err) {
        app.log.error({ err, event }, 'STOMP: handler error');
      }
    });

    const STOMP_PORT = 8081;
    stompHttpServer.listen(STOMP_PORT, '127.0.0.1', () => {
      app.log.info(`STOMP broker listening on http://127.0.0.1:${STOMP_PORT}/ws`);
    });

    app.addHook('onClose', async () => {
      stompHttpServer.close();
      app.stompSessions.clear();
    });
  },
  { name: 'stomp' },
);
