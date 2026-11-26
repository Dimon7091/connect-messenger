import { string } from 'zod';
import type { ChatService } from '../../../modules/chats/chat.service.js';
import type { MessageService } from '../../../modules/messages/message.service.js';

export function createAllChatMessagesReadUseCase(deps: {
  messageService: MessageService;
  chatService: ChatService;
}) {
  return {
    async execute(chatId: bigint, receiverId: string): Promise<string | undefined> {
      const result = await deps.messageService.markAllChatMessagesAsRead(chatId, receiverId);

      if (!result) {
        return;
      }

      await deps.chatService.resetUnreadForParticipant(chatId, receiverId);
      const participants = await deps.chatService.findChatParticipants(chatId);
      const sender = participants.find(p => p.userId !== receiverId);
      return sender?.userId;
    },
  };
}
