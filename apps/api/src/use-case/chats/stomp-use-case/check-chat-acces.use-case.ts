import type { ChatService } from '../../../modules/chats/chat.service.js';

export function createCheckChatAccessUseCase(deps: { chatService: ChatService }) {
  return {
    async execute(chatId: bigint, senderId: string, receiverId: string): Promise<void> {
      await deps.chatService.ensureParticipant(chatId, senderId);
      await deps.chatService.ensureParticipant(chatId, receiverId);
    },
  };
}
