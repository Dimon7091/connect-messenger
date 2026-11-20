import type { ChatResponse } from '@messenger/shared-schema';
import type { ChatService } from '../../modules/chats/chat.service.js';
import { report } from 'node:process';

export function createGetUserChatsUseCase(deps: { chatService: ChatService }) {
  return {
    async getChats(userId: string): Promise<ChatResponse[]> {
      const chats = await deps.chatService.getUserChats(userId);

      const response = [];

      for (const chat of chats) {
        const participantsIds = chat.participants.map(p => p.userId);

        response.push({
          id: chat.id.toString(),
          type: chat.type,
          participants: participantsIds,
          unreadCount: 0,
          createdAt: chat.createdAt,
          updatedAt: chat.updatedAt,
          createdBy: chat.createdBy,
          lastMessage: chat.lastMessage,
        });
      }
      return response;
    },
  };
}
