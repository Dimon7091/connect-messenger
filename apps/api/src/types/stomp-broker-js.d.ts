// types/stomp-broker-js.d.ts
declare module 'stomp-broker-js' {
  import type { Server } from 'http';

  export interface StompOptions {
    server: Server;
    path?: string;
    protocol: string;
    heartbeat?: [number, number];
  }

  export default class StompServer {
    constructor(options: StompOptions);

    /** Подписка на события библиотеки */
    on(
      event: 'connecting',
      cb: (sessionId: string) => void,
    ): this;
    on(
      event: 'connected' | 'disconnected',
      cb: (sessionId: string, headers?: Record<string, string | undefined>) => void,
    ): this;
    on(event: string, cb: (...args: any[]) => any): this;

    /** Добавить middleware для перехвата команд STOMP (connect, send, subscribe, etc.) */
    addMiddleware(
      command: 'connect' | 'disconnect' | 'send' | 'subscribe' | 'unsubscribe',
      handler: (
        socket: { sessionId: string },
        args: { headers: Record<string, string | undefined> },
        next: () => boolean,
      ) => boolean,
    ): void;

    /** Серверная подписка на destination (wildcard поддерживается) */
    subscribe(
      destination: string,
      cb: (msg: string, headers: Record<string, unknown>) => void,
    ): string;

    /** Отправка сообщения от сервера */
    send(destination: string, headers: Record<string, unknown>, body: string): void;
  }
}
