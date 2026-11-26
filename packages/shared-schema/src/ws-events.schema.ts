import { z } from 'zod';
import {
  AllMessagesReadRequestSchema,
  AllMessagesReadResponseSchema,
  MarkMessageAsDeliveredRequestSchema,
  MarkMessageAsReadRequestSchema,
  MessageDeletedResponseSchema,
  MessageDeliveredResponseSchema,
  MessageNewResponseSchema,
  MessageReadResponseSchema,
  MessageSentResponseSchema,
  SendMessageRequestSchema,
  TypingRequestSchema,
  TypingResponseSchema,
} from './message.schema.js';

export const WsEventTypeSchema = z.enum([
  'connection',
  'message_new',
  'message_sent',
  'message_delivered',
  'message_read',
  'all_messages_read',
  'message_deleted',
  'chat_history_cleared', // ← добавлено: клиент обрабатывает это событие
  'user_status_subscribe',
  'user_status',
  'typing_start',
  'typing_stop',
  'chat_store',
  'error',
  'pong',
]);

// Вспомогательная фабрика для создания схемы обертки WSEvent<T>
export const createWsEventSchema = <K extends WsEventType, T extends z.ZodType>(
  type: K,
  payloadSchema: T
) =>
  z.object({
    type: z.literal(type),
    payload: payloadSchema,
  });

export const WsErrorResponseSchema = z.object({
  message: z.string(),
  body: z.string().nullish(),
});

// === Переиспользуемые схемы payload-ов ===

// Клиент отправляет { chatId, userId } — раньше сервер принимал только chatId

// Подписка на статус пользователя
export const UserStatusRequestSchema = z.object({
  userId: z.string(),
});

// Статус соединения: клиент ждёт { status: 'connected' | 'disconnected' }
export const ConnectionPayloadSchema = z.object({
  status: z.enum(['connected', 'disconnected']),
});

// Статус пользователя: клиент использует userId, status, lastSeen
export const UserStatusPayloadSchema = z.object({
  userId: z.string(),
  status: z.string(),
  lastSeen: z.string().nullish(),
});

// История чата очищена: клиент ждёт { chatId }
export const ChatHistoryClearedPayloadSchema = z.object({
  chatId: z.string(),
});

// --- Входящие события от клиента ---
export const IncomingWsEventSchema = z.discriminatedUnion('type', [
  createWsEventSchema('message_sent', SendMessageRequestSchema),
  createWsEventSchema('message_delivered', MarkMessageAsDeliveredRequestSchema),
  createWsEventSchema('message_read', MarkMessageAsReadRequestSchema),
  createWsEventSchema('all_messages_read', AllMessagesReadRequestSchema),
  createWsEventSchema('typing_start', TypingRequestSchema),
  createWsEventSchema('typing_stop', TypingRequestSchema),
  createWsEventSchema('user_status_subscribe', UserStatusRequestSchema),
]);

export type IncomingWsEvent = z.infer<typeof IncomingWsEventSchema>;

// --- Исходящие события с сервера ---
export const OutgoingWsSchemas = {
  connection: createWsEventSchema('connection', ConnectionPayloadSchema),
  message_sent: createWsEventSchema('message_sent', MessageSentResponseSchema),
  message_new: createWsEventSchema('message_new', MessageNewResponseSchema),
  message_delivered: createWsEventSchema('message_delivered', MessageDeliveredResponseSchema),
  message_read: createWsEventSchema('message_read', MessageReadResponseSchema),
  all_messages_read: createWsEventSchema('all_messages_read', AllMessagesReadResponseSchema),
  message_deleted: createWsEventSchema('message_deleted', MessageDeletedResponseSchema),
  chat_history_cleared: createWsEventSchema(
    'chat_history_cleared',
    ChatHistoryClearedPayloadSchema
  ),
  user_status: createWsEventSchema('user_status', UserStatusPayloadSchema),
  typing_start: createWsEventSchema('typing_start', TypingResponseSchema),
  typing_stop: createWsEventSchema('typing_stop', TypingResponseSchema),
  error: createWsEventSchema('error', WsErrorResponseSchema),
} as const;

export type WsEventType = z.infer<typeof WsEventTypeSchema>;
export type ErrorResponse = z.infer<typeof WsErrorResponseSchema>;
