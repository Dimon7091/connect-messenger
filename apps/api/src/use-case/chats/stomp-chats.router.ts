// transport/stomp/stomp.router.ts
import { z } from 'zod';
import fp from 'fastify-plugin';
import type { FastifyInstance } from 'fastify';
import {
  IncomingWsEventSchema,
  OutgoingWsSchemas,
  type AllMessagesReadResponse,
  type ErrorResponse,
  type MessageDeletedResponse,
  type MessageDeliveredResponse,
  type MessageReadResponse,
  type TypingResponse,
} from '@messenger/shared-schema';
import type { StompBroker } from '../../plugins/stompPlugin.js';
import { createSendMessageUseCase } from './stomp-use-case/send-message.use-case.js';
import { createUpdateMessageStatusUseCase } from './stomp-use-case/update-message-status.use-case.js';
import { createAllChatMessagesReadUseCase } from './stomp-use-case/all-chat-messages-read.use-case.js';
import { createCheckChatAccessUseCase } from './stomp-use-case/check-chat-acces.use-case.js';

function sendWsEvent<TSchema extends z.ZodType>(
  stomp: StompBroker,
  destination: string,
  schema: TSchema,
  data: z.input<TSchema>
): void {
  const validated = schema.parse(data);
  stomp.send(destination, {}, JSON.stringify(validated));
}

export const stompChatRouter = fp(async (app: FastifyInstance) => {
  const sendMessageUseCase = createSendMessageUseCase({
    messageService: app.messageService,
    chatService: app.chatService,
  });
  const updateMessageStatusUseCase = createUpdateMessageStatusUseCase({
    messageService: app.messageService,
    chatService: app.chatService,
  });

  const allChatMessagesReadUseCase = createAllChatMessagesReadUseCase({
    messageService: app.messageService,
    chatService: app.chatService,
  });
  const checkChatAccessUseCase = createCheckChatAccessUseCase({ chatService: app.chatService });

  app.stompHandlers = {
    async handle(rawEvent, headers, stomp) {
      const userId = String(headers['x-user-id'] ?? '');
      if (!userId) {
        app.log.warn({ eventType: rawEvent.type }, 'STOMP: unauthenticated frame');
        return;
      }

      try {
        const event = IncomingWsEventSchema.parse(rawEvent);

        switch (event.type) {
          // MARK: - Sent message
          case 'message_sent': {
            app.log.debug('STOMP: new message received');
            // Заменяем id отправителя на id из jwt
            const request = { ...event.payload, senderId: userId };
            const response = await sendMessageUseCase.send(request);

            // Отправляем подтверждение об отправке отправителю
            if (request.messageId) {
              sendWsEvent(stomp, `/user/${userId}/queue/private`, OutgoingWsSchemas.message_sent, {
                type: 'message_sent',
                payload: {
                  messageId: request.messageId,
                  serverMessageId: response.message.id.toString(),
                  status: response.message.status,
                  timestamp: response.message.createdAt,
                },
              });
            }

            // Отправляем сообщение получателю
            sendWsEvent(
              stomp,
              `/user/${request.receiverId}/queue/private`,
              OutgoingWsSchemas.message_new,
              {
                type: 'message_new',
                payload: response,
              }
            );
            break;
          }

          // MARK: - Message delivered
          case 'message_delivered': {
            const request = event.payload;
            const result = await updateMessageStatusUseCase.execute(
              request.messageId,
              userId,
              'DELIVERED'
            );

            if (result) {
              const response: MessageDeliveredResponse = {
                messageId: result.id.toString(),
                chatId: result.chatId.toString(),
              };

              sendWsEvent(
                stomp,
                `/user/${result.senderId}/queue/private`,
                OutgoingWsSchemas.message_delivered,
                {
                  type: 'message_delivered',
                  payload: response,
                }
              );
            } else {
              app.log.debug('STOMP: message_delivered skipped (already updated or forbidden)');
            }

            break;
          }

          // MARK: - Message read
          case 'message_read': {
            const request = event.payload;
            const result = await updateMessageStatusUseCase.execute(
              request.messageId,
              userId,
              'READ'
            );

            if (result) {
              const response: MessageReadResponse = {
                messageId: result.id.toString(),
                chatId: result.chatId.toString(),
              };

              sendWsEvent(
                stomp,
                `/user/${result.senderId}/queue/private`,
                OutgoingWsSchemas.message_read,
                {
                  type: 'message_read',
                  payload: response,
                }
              );

              sendWsEvent(
                stomp,
                `/user/${result.receiverId}/queue/private`,
                OutgoingWsSchemas.message_read,
                {
                  type: 'message_read',
                  payload: response,
                }
              );
            } else {
              app.log.debug('STOMP: message_read skipped (already updated or forbidden)');
            }

            break;
          }

          // MARK: - All messages read
          case 'all_messages_read': {
            const request = event.payload;
            const senderId = await allChatMessagesReadUseCase.execute(request.chatId, userId);

            if (senderId) {
              const response: AllMessagesReadResponse = {
                chatId: request.chatId.toString(),
              };

              sendWsEvent(
                stomp,
                `/user/${senderId}/queue/private`,
                OutgoingWsSchemas.all_messages_read,
                {
                  type: 'all_messages_read',
                  payload: response,
                }
              );

              sendWsEvent(
                stomp,
                `/user/${userId}/queue/private`,
                OutgoingWsSchemas.all_messages_read,
                {
                  type: 'all_messages_read',
                  payload: response,
                }
              );
              app.log.info('STOMP: all_message_read sent to sender!');
            } else {
              app.log.debug('STOMP: all_message_read skipped');
            }

            break;
          }

          // MARK: - Typing
          case 'typing_start': {
            const request = event.payload;
            await checkChatAccessUseCase.execute(request.chatId, userId, request.receiverId);

            const response: TypingResponse = {
              chatId: request.chatId.toString(),
              targetUserId: userId,
            };

            sendWsEvent(
              stomp,
              `/user/${request.receiverId}/queue/private`,
              OutgoingWsSchemas.typing_start,
              {
                type: 'typing_start',
                payload: response,
              }
            );

            break;
          }

          case 'typing_stop': {
            const request = event.payload;
            await checkChatAccessUseCase.execute(request.chatId, userId, request.receiverId);

            const response: TypingResponse = {
              chatId: request.chatId.toString(),
              targetUserId: userId,
            };

            sendWsEvent(
              stomp,
              `/user/${request.receiverId}/queue/private`,
              OutgoingWsSchemas.typing_stop,
              {
                type: 'typing_stop',
                payload: response,
              }
            );

            break;
          }

          default: {
            app.log.warn({ event }, 'STOMP: unhandled event type');
            break;
          }
        }
      } catch (err) {
        app.log.error({ err, userId, rawEvent }, 'STOMP: handler error');

        const errorPayload: ErrorResponse =
          err instanceof z.ZodError
            ? { message: 'Validation error', body: JSON.stringify(err.issues) }
            : { message: err instanceof Error ? err.message : 'Internal server error', body: null };

        sendWsEvent(stomp, `/user/${userId}/queue/private`, OutgoingWsSchemas.error, {
          type: 'error',
          payload: errorPayload,
        });
      }
    },
  };
});
