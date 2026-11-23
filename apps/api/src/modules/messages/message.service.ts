import type { Message, MessageAttachment, MessageStatus } from '@messenger/database';
import type { MessageRepository, FindMessagesParams } from './message.repository.js';
import { BadRequestError, ConflictError, ForbiddenError, NotFoundError } from '../../lib/errors.js';
import type { SendMessageRequest } from '@messenger/shared-schema';

export interface MessageService {
  createMessage(input: SendMessageRequest): Promise<Message>;
  getMessageById(messageId: bigint): Promise<Message>;
  getChatHistory(params: FindMessagesParams): Promise<Message[]>;
  getLastVisibleMessage(chatId: bigint, userId: string): Promise<Message | undefined>;
  // editMessage(messageId: bigint, userId: string, newText: string): Promise<Message>;
  updateMessageStatus(messageId: bigint, status: MessageStatus): Promise<Message>;
  markMessagesAsRead(chatId: bigint, userId: string, upToMessageId: bigint): Promise<void>;
  deleteMessageForSelf(messageId: bigint, userId: string): Promise<void>;
  deleteMessageForAll(messageId: bigint, userId: string): Promise<Message>;
}

export const createMessageService = (messageRepo: MessageRepository): MessageService => {
  return {
    // MARK: createMessage
    async createMessage(input): Promise<Message> {
      const trimmedText = input.text?.trim() ?? null;
      const hasAttachments = Boolean(input.attachments && input.attachments.length > 0);

      if (!trimmedText && !hasAttachments) {
        throw new BadRequestError('Message must contain text or at least one attachment');
      }

      // Проверяем существование цитируемого сообщения, если передан replyToId
      if (input.replyToId) {
        const replyTarget = await messageRepo.findById(input.replyToId);
        if (!replyTarget || replyTarget.chatId !== input.chatId) {
          throw new BadRequestError('Invalid replyToId: message not found in this chat');
        }
      }

      const now = new Date();

      return messageRepo.create({
        chatId: input.chatId,
        senderId: input.senderId,
        receiverId: input.receiverId ?? null,
        text: trimmedText,
        timestamp: now,
        createdAt: now,
        status: 'SENT',
        replyToId: input.replyToId ?? null,
        attachments: input.attachments ?? [],
        deletedBy: [],
        readBy: [input.senderId], // Отправитель по умолчанию уже прочитал свое сообщение
      });
    },

    // MARK: getMessageById
    async getMessageById(messageId: bigint): Promise<Message> {
      const message = await messageRepo.findById(messageId);
      if (!message) {
        throw new NotFoundError('Message', messageId.toString());
      }
      return message;
    },

    // MARK: getChatHistory
    async getChatHistory(params: FindMessagesParams): Promise<Message[]> {
      return messageRepo.findChatHistory({
        chatId: params.chatId,
        userId: params.userId,
        limit: Math.min(params.limit ?? 50, 100),
        beforeTimestamp: params.beforeTimestamp,
      });
    },

    // MARK: getLastVisibleMessage
    async getLastVisibleMessage(chatId: bigint, userId: string): Promise<Message | undefined> {
      return messageRepo.findLastVisibleMessage(chatId, userId);
    },

    // MARK: editMessage
    // async editMessage(messageId: bigint, userId: string, newText: string): Promise<Message> {
    //   const trimmedText = newText.trim();
    //   if (!trimmedText) {
    //     throw new BadRequestError('Edited message text cannot be empty');
    //   }

    //   const message = await messageRepo.findById(messageId);
    //   if (!message) {
    //     throw new NotFoundError('Message', messageId.toString());
    //   }

    //   // Редактировать сообщение может только его автор
    //   if (message.senderId !== userId) {
    //     throw new ForbiddenError('You can only edit your own messages');
    //   }

    //   const updated = await messageRepo.updateText(messageId, trimmedText);
    //   if (!updated) {
    //     throw new NotFoundError('Message', messageId.toString());
    //   }

    //   return updated;
    // },

    // MARK: updateMessageStatus
    async updateMessageStatus(messageId: bigint, status: MessageStatus): Promise<Message> {
      const updated = await messageRepo.updateStatus(messageId, status);
      if (!updated) {
        throw new NotFoundError('Message', messageId.toString());
      }
      return updated;
    },

    // MARK: markMessagesAsRead
    async markMessagesAsRead(chatId: bigint, userId: string, upToMessageId: bigint): Promise<void> {
      await messageRepo.markManyAsRead(chatId, userId, upToMessageId);
    },

    // MARK: deleteMessageForSelf
    async deleteMessageForSelf(messageId: bigint, userId: string): Promise<void> {
      const deleted = await messageRepo.softDeleteForUser(messageId, userId);
      if (!deleted) {
        throw new NotFoundError('Message', messageId.toString());
      }
    },

    // MARK: deleteMessageForAll
    async deleteMessageForAll(messageId: bigint, userId: string): Promise<Message> {
      const message = await messageRepo.findById(messageId);
      if (!message) {
        throw new NotFoundError('Message', messageId.toString());
      }

      // Удалить для всех может только отправитель
      if (message.senderId !== userId) {
        throw new ConflictError('You can only delete your own messages for everyone');
      }

      const deleted = await messageRepo.deleteById(messageId);
      if (!deleted) {
        throw new NotFoundError('Message', messageId.toString());
      }

      return message; // Возвращаем удаленное сообщение, чтобы use-case мог проверить, не было ли оно lastMessage в чате
    },
  };
};
