import type { ChatResponse } from '@messenger/shared-schema';
import type { ChatService } from '../../modules/chats/chat.service.js';

export function createGetOrCreateUseCase(deps: { chatService: ChatService }) {
  return {
    async getChat(currentUserId: string, companionId: string): Promise<ChatResponse> {
      // ===============================================================
      // DOING: Сделать возможность отдавать собеседников вместе с чатами
      // ===============================================================
      const chat = await deps.chatService.getOrCreateDirectChat(currentUserId, companionId);
      const participantsIds = chat.participants.map(p => p.userId);

      const chatResponse: ChatResponse = {
        id: chat.id.toString(),
        type: chat.type,
        participants: participantsIds,
        unreadCount: 0,
        createdAt: chat.createdAt,
        updatedAt: chat.updatedAt,
        createdBy: chat.createdBy,
        lastMessage: chat.lastMessage,
      };
      return chatResponse;
    },
  };
}
