// plugins/stompPlugin.ts
import fp from 'fastify-plugin';
import websocket from '@fastify/websocket';
import type { FastifyInstance } from 'fastify';
import type { WebSocket } from '@fastify/websocket';

export interface StompBroker {
  send: (destination: string, headers: Record<string, string>, body: string) => void;
}

interface ClientSession {
  socket: WebSocket;
  userId: string;
  subscriptions: Map<string, string>; // subId -> destination
  sendSockJs: (rawStompFrame: string) => void;
}

// Формирование исходящего STOMP-фрейма с правильным подсчётом байт UTF-8
function buildFrame(command: string, headers: Record<string, string>, body = ''): string {
  const lines = [command];
  for (const [k, v] of Object.entries(headers)) {
    lines.push(`${k}:${v}`);
  }
  if (body) {
    lines.push(`content-length:${Buffer.byteLength(body, 'utf8')}`);
  }
  return `${lines.join('\n')}\n\n${body}\0`;
}

// Парсер входящего STOMP-фрейма
function parseFrame(raw: string) {
  const clean = raw.replace(/\0+$/, '');
  const splitIdx = clean.indexOf('\n\n');
  if (splitIdx === -1) return null;

  const headerLines = clean.slice(0, splitIdx).split(/\r?\n/);
  const command = headerLines.shift()?.trim();
  if (!command) return null;

  const headers: Record<string, string> = {};
  for (const line of headerLines) {
    const colon = line.indexOf(':');
    if (colon !== -1) {
      headers[line.slice(0, colon).trim()] = line.slice(colon + 1).trim();
    }
  }

  const body = clean.slice(splitIdx + 2);
  return { command, headers, body };
}

export default fp(
  async (app: FastifyInstance) => {
    await app.register(websocket);

    const sessions = new Set<ClientSession>();

    const stomp: StompBroker = {
      send(destination, headers, body) {
        for (const session of sessions) {
          for (const [subId, subDest] of session.subscriptions.entries()) {
            if (subDest === destination && session.socket.readyState === 1) {
              const frame = buildFrame(
                'MESSAGE',
                {
                  ...headers,
                  destination,
                  subscription: subId,
                  'message-id': `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
                  'content-type': 'application/json',
                },
                body
              );
              session.sendSockJs(frame);
            }
          }
        }
      },
    };

    app.decorate('stomp', stomp);
    app.decorate('stompHandlers', {
      async handle() {
        app.log.warn('STOMP: no handler registered yet');
      },
    });

    // 1. Обязательный HTTP-эндпоинт для handshake библиотеки sockjs-client
    app.get('/ws/info', async () => {
      return {
        websocket: true,
        origins: ['*:*'],
        cookie_needed: false,
        entropy: Math.floor(Math.random() * 1_000_000_000),
      };
    });

    // 2. WebSocket-эндпоинт в формате SockJS: /ws/{serverId}/{sessionId}/websocket
    app.get('/ws/:serverId/:sessionId/websocket', { websocket: true }, socket => {
      let session: ClientSession | null = null;
      let buffer = '';

      // SockJS требует сразу отправить кадр открытия 'o'
      socket.send('o');

      // SockJS heartbeat каждые 25 секунд, чтобы соединение не рвалось по таймауту
      const sockJsHeartbeat = setInterval(() => {
        if (socket.readyState === 1) {
          socket.send('h');
        }
      }, 25000);

      // Отправка STOMP-фрейма в обёртке SockJS: a["..."]
      const sendSockJs = (rawStompFrame: string) => {
        if (socket.readyState === 1) {
          socket.send(`a${JSON.stringify([rawStompFrame])}`);
        }
      };

      socket.on('message', async (data: Buffer) => {
        const rawText = data.toString('utf8');
        if (!rawText) return;

        // SockJS оборачивает входящие сообщения от клиента в JSON-массив: ["..."]
        let messages: string[] = [];
        try {
          const parsed = JSON.parse(rawText);
          messages = Array.isArray(parsed) ? parsed : [parsed];
        } catch {
          messages = [rawText];
        }

        for (const text of messages) {
          // Игнорируем STOMP heartbeat-пинги (\n)
          if (text === '\n' || text === '\r\n') continue;

          buffer += text;

          let nullIdx: number;
          while ((nullIdx = buffer.indexOf('\0')) !== -1) {
            const rawFrame = buffer.slice(0, nullIdx);
            buffer = buffer.slice(nullIdx + 1);

            const frame = parseFrame(rawFrame);
            if (!frame) continue;

            // --- CONNECT ---
            if (frame.command === 'CONNECT' || frame.command === 'STOMP') {
              const token = frame.headers['Auth-Token'];
              if (!token) {
                sendSockJs(buildFrame('ERROR', { message: 'Missing Auth-Token' }));
                socket.close();
                return;
              }
              try {
                const decoded = app.jwt.verify(token) as { sub: string };
                session = {
                  socket,
                  userId: decoded.sub,
                  subscriptions: new Map(),
                  sendSockJs,
                };
                sessions.add(session);

                sendSockJs(
                  buildFrame('CONNECTED', {
                    version: '1.2',
                    'heart-beat': '10000,10000',
                  })
                );
                app.log.info({ userId: decoded.sub }, 'STOMP: client connected');
              } catch {
                sendSockJs(buildFrame('ERROR', { message: 'Invalid token' }));
                socket.close();
                return;
              }
              continue;
            }

            if (!session) {
              socket.close();
              return;
            }

            // --- SUBSCRIBE ---
            if (frame.command === 'SUBSCRIBE') {
              const subId = frame.headers['id'];
              let dest = frame.headers['destination'];
              if (!subId || !dest) continue;

              // Поддерживаем любые личные очереди вида /user/queue/... (private, status и т.д.)
              if (dest.startsWith('/user/queue/')) {
                const queueName = dest.slice('/user/queue/'.length);
                dest = `/user/${session.userId}/queue/${queueName}`;
              } else if (dest.startsWith('/user/')) {
                // Запрещаем прямую подписку на /user/{чужойId}/...
                app.log.warn({ userId: session.userId, dest }, 'STOMP: forbidden subscription');
                continue;
              }

              session.subscriptions.set(subId, dest);
              app.log.info({ userId: session.userId, dest }, 'STOMP: subscribed');
              continue;
            }

            // --- UNSUBSCRIBE ---
            if (frame.command === 'UNSUBSCRIBE') {
              const subId = frame.headers['id'];
              if (subId) session.subscriptions.delete(subId);
              continue;
            }

            // --- SEND ---
            if (frame.command === 'SEND') {
              const dest = frame.headers['destination'] ?? '';
              if (!dest.startsWith('/app/')) continue;

              let event: { type: string; payload: unknown };
              try {
                event = JSON.parse(frame.body);
              } catch {
                app.log.warn({ body: frame.body }, 'STOMP: invalid JSON');
                continue;
              }

              if (typeof event.type !== 'string') continue;

              try {
                await app.stompHandlers.handle(
                  event,
                  { ...frame.headers, 'x-user-id': session.userId },
                  stomp
                );
              } catch (err) {
                app.log.error({ err, event }, 'STOMP: handler error');
              }
              continue;
            }

            // --- DISCONNECT ---
            if (frame.command === 'DISCONNECT') {
              socket.close();
            }
          }
        }
      });

      socket.on('close', () => {
        clearInterval(sockJsHeartbeat);
        if (session) {
          sessions.delete(session);
          app.log.info({ userId: session.userId }, 'STOMP: disconnected');
        }
      });
    });
  },
  { name: 'stomp' }
);
