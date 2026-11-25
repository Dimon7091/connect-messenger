import { z } from 'zod';

export const WsEventTypeSchema = z.enum([
  'connection',
  'message_new',
  'message_sent',
  'message_delivered',
  'message_read',
  'all_messages_read',
  'message_deleted',
  'user_status_subscribe',
  'user_status',
  'typing_start',
  'typing_stop',
  'chat_store',
  'error',
  'pong',
]);

export type WsEventType = z.infer<typeof WsEventTypeSchema>;

// Вспомогательная фабрика для создания схемы обертки WSEvent<T>
export const createWsEventSchema = <T extends z.ZodType>(type: WsEventType, payloadSchema: T) =>
  z.object({
    type: z.literal(type),
    payload: payloadSchema,
  });

export const WsErrorResponseSchema = z.object({
  message: z.string(),
  body: z.string().nullish(),
});

export type ErrorResponse = z.infer<typeof WsErrorResponseSchema>;
