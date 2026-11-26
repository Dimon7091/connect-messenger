import type { Chat, ChatParticipant, ChatWithParticipants } from '@messenger/database';
import type { ChatRepository } from './chat.repository.js';
import { ConflictError, ForbiddenError, NotFoundError } from '../../lib/errors.js';

export interface ChatService {
  getOrCreateDirectChat(currentUserId: string, targetUserId: string): Promise<ChatWithParticipants>;
  getChatById(chatId: bigint, userId: string): Promise<ChatWithParticipants>;
  getUserChats(userId: string): Promise<ChatWithParticipants[]>;
  ensureParticipant(chatId: bigint, userId: string): Promise<void>;
  findChatParticipants(chatId: bigint): Promise<ChatParticipant[]>;
  onNewMessage(chatId: bigint, senderId: string, text: string): Promise<ChatWithParticipants>;
  markChatAsRead(
    chatId: bigint,
    userId: string,
    lastReadMessageId: bigint
  ): Promise<ChatParticipant>;
  incrementUnreadForParticipant(chatId: bigint, userId: string): Promise<void>;
  decrementUnreadForParticipant(chatId: bigint, userId: string): Promise<void>;
  resetUnreadForParticipant(chatId: bigint, userId: string): Promise<void>;
  
  updateChatSettings(
    chatId: bigint,
    userId: string,
    settings: { isMuted?: boolean; isPinned?: boolean }
  ): Promise<ChatParticipant>;
  deleteChatForUser(chatId: bigint, userId: string): Promise<void>;
}

/**
 * Детерминированный ключ для личного диалога 1-на-1
 */
function buildDirectKey(userA: string, userB: string): string {
  const [first, second] = [userA.toLowerCase(), userB.toLowerCase()].sort();
  return `${first}:${second}`;
}

export const createChatService = (chatRepo: ChatRepository): ChatService => {
  return {
    // MARK: getOrCreateDirectChat
    async getOrCreateDirectChat(currentUserId: string, targetUserId: string) {
      if (currentUserId === targetUserId) {
        throw new ConflictError('Cannot create a direct chat with yourself');
      }

      const directKey = buildDirectKey(currentUserId, targetUserId);

      // Сразу получаем чат с участниками за 1 запрос
      const existing = await chatRepo.findByDirectKey(directKey);
      if (existing) {
        return existing;
      }

      // Пытаемся создать новый чат
      const created = await chatRepo.createDirectIfNotExists({
        type: 'DIRECT',
        directKey,
        createdBy: currentUserId,
      });

      // Если параллельный запрос успел создать чат раньше — забираем его (уже с участниками!)
      if (!created) {
        const raceChat = await chatRepo.findByDirectKey(directKey);
        if (!raceChat) {
          throw new Error('Failed to create or fetch direct chat');
        }
        return raceChat;
      }

      // Добавляем участников и возвращаем собранный объект
      const participants = await chatRepo.addParticipants([
        { chatId: created.id, userId: currentUserId, isChatEmpty: false },
        { chatId: created.id, userId: targetUserId, isChatEmpty: false },
      ]);

      return {
        ...created,
        participants,
      };
    },

    // MARK: getChatById
    async getChatById(chatId: bigint, userId: string): Promise<ChatWithParticipants> {
      const chat = await chatRepo.findById(chatId);
      if (!chat) {
        throw new NotFoundError('Chat', chatId.toString());
      }

      const isMember = chat.participants.some(p => p.userId === userId && !p.isDeleted);
      if (!isMember) {
        throw new ForbiddenError('You are not a participant of this chat');
      }

      return chat;
    },

    // MARK: getUserChats
    async getUserChats(userId: string): Promise<ChatWithParticipants[]> {
      return chatRepo.findUserChats(userId);
    },

    // MARK: ensureParticipant
    async ensureParticipant(chatId: bigint, userId: string): Promise<void> {
      const isMember = await chatRepo.exists(chatId, userId);
      if (!isMember) {
        throw new ForbiddenError('You are not a participant of this chat');
      }
    },

    // MARK: findChatParticipants
    async findChatParticipants(chatId) {
      return chatRepo.findAllByChatId(chatId);
    },

    // MARK: onNewMessage
    async onNewMessage(
      chatId: bigint,
      receiverId: string,
      text: string
    ): Promise<ChatWithParticipants> {
      await chatRepo.updateLastMessage(chatId, text);
      await chatRepo.incrementUnreadForParticipant(chatId, receiverId);

      const updatedChat = await chatRepo.findById(chatId);
      if (!updatedChat) {
        throw new NotFoundError('Chat', chatId.toString());
      }

      return updatedChat;
    },

    // MARK: markChatAsRead
    async markChatAsRead(
      chatId: bigint,
      userId: string,
      lastReadMessageId: bigint
    ): Promise<ChatParticipant> {
      const updated = await chatRepo.markAsRead(chatId, userId, lastReadMessageId);
      if (!updated) {
        throw new NotFoundError('ChatParticipant', `${chatId}:${userId}`);
      }

      return updated;
    },

    // MARK: unreadCount
    async incrementUnreadForParticipant(chatId, userId) {
      return chatRepo.incrementUnreadForParticipant(chatId, userId);
    },

    async decrementUnreadForParticipant(chatId, userId) {
      return chatRepo.decrementUnreadForParticipant(chatId, userId);
    },

    async resetUnreadForParticipant(chatId, userId) {
      return chatRepo.resetUnreadForParticipant(chatId, userId)
    },

    // MARK: updateChatSettings
    async updateChatSettings(
      chatId: bigint,
      userId: string,
      settings: { isMuted?: boolean; isPinned?: boolean }
    ): Promise<ChatParticipant> {
      const updated = await chatRepo.updateSettings(chatId, userId, {
        ...(settings.isMuted !== undefined && { isMuted: settings.isMuted }),
        ...(settings.isPinned !== undefined && {
          pinnedAt: settings.isPinned ? new Date() : null,
        }),
      });

      if (!updated) {
        throw new NotFoundError('ChatParticipant', `${chatId}:${userId}`);
      }

      return updated;
    },

    // MARK: deleteChatForUser
    async deleteChatForUser(chatId: bigint, userId: string): Promise<void> {
      const deleted = await chatRepo.softDelete(chatId, userId);
      if (!deleted) {
        throw new NotFoundError('ChatParticipant', `${chatId}:${userId}`);
      }
    },
  };
};
