import type { Chat, ChatParticipant, ChatWithParticipants } from '@messenger/database';
import type { ChatRepository } from './chat.repository.js';
import { ConflictError, ForbiddenError, NotFoundError } from '../../lib/errors.js';

// Абстракция транзакции для слоя сервисов (без привязки к Drizzle)
export interface ChatTransactionManager {
  runInTransaction<T>(work: (txChatRepo: ChatRepository) => Promise<T>): Promise<T>;
}

export interface ChatService {
  getOrCreateDirectChat(currentUserId: string, targetUserId: string): Promise<Chat>;
  getChatById(chatId: bigint, userId: string): Promise<ChatWithParticipants>;
  getUserChats(userId: string): Promise<ChatWithParticipants[]>;
  markChatAsRead(
    chatId: bigint,
    userId: string,
    lastReadMessageId: bigint
  ): Promise<ChatParticipant>;
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

export const createChatService = (
  chatRepo: ChatRepository,
  txManager: ChatTransactionManager
): ChatService => {
  return {
    // MARK: getOrCreateDirectChat
    async getOrCreateDirectChat(currentUserId: string, targetUserId: string): Promise<Chat> {
      if (currentUserId === targetUserId) {
        throw new ConflictError('Cannot create a direct chat with yourself');
      }

      const directKey = buildDirectKey(currentUserId, targetUserId);

      // Быстрая проверка вне транзакции (в 95% случаев чат уже существует)
      const existing = await chatRepo.findByDirectKey(directKey);
      if (existing) {
        return existing;
      }

      // Атомарное создание чата и добавление обоих участников через абстракцию транзакции
      return txManager.runInTransaction(async txChatRepo => {
        const created = await txChatRepo.createDirectIfNotExists({
          type: 'DIRECT',
          directKey,
          createdBy: currentUserId,
        });

        // Если параллельный запрос успел создать чат на миллисекунду раньше — возвращаем его
        if (!created) {
          const raceChat = await txChatRepo.findByDirectKey(directKey);
          if (!raceChat) {
            throw new Error('Failed to create or fetch direct chat');
          }
          return raceChat;
        }

        await txChatRepo.addParticipants([
          { chatId: created.id, userId: currentUserId, isChatEmpty: true },
          { chatId: created.id, userId: targetUserId, isChatEmpty: true },
        ]);

        return created;
      });
    },

    // MARK: getChatById
    async getChatById(chatId: bigint, userId: string): Promise<ChatWithParticipants> {
      const chat = await chatRepo.findByIdWithParticipants(chatId);
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
      return chatRepo.findUserChatsWithParticipants(userId);
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

    // MARK: updateChatSettings
    async updateChatSettings(
      chatId: bigint,
      userId: string,
      settings: { isMuted?: boolean; isPinned?: boolean }
    ): Promise<ChatParticipant> {
      // Исправлено: используем chatRepo.updateParticipantSettings
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
      // Исправлено: используем chatRepo.softDeleteForUser
      const deleted = await chatRepo.softDelete(chatId, userId);
      if (!deleted) {
        throw new NotFoundError('ChatParticipant', `${chatId}:${userId}`);
      }
    },
  };
};
