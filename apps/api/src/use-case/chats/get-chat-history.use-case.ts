import type {
  ChatHistoryQueryRequest,
  ChatResponse,
  MessageResponse,
} from '@messenger/shared-schema';
import type { ChatService } from '../../modules/chats/chat.service.js';
import type { MessageService } from '../../modules/messages/message.service.js';

export function createGetChatHistoryUseCase(deps: { messageService: MessageService }) {
  return {
    async getChatHistory(request: ChatHistoryQueryRequest): Promise<MessageResponse[]> {
      const messages = await deps.messageService.getChatHistory(request);
      const response = [];

      for (const message of messages) {
        const messageResponse: MessageResponse = {
          id: message.id.toString(),
          chatId: message.chatId.toString(),
          createdAt: message.createdAt,
          senderId: message.senderId,
          receiverId: message.receiverId,
          text: message.text,
          timestamp: message.timestamp,
          status: message.status,
          deletedBy: message.deletedBy,
          readBy: message.readBy,
          replyToId: message.replyToId?.toString() ?? undefined,
          attachments: message.attachments,
        };
        response.push(messageResponse);
      }
      return response;
    },
  };
}
