import type { MessageStatus } from '@messenger/shared-schema';
import type { MessageService } from '../../../modules/messages/message.service.js';
import type { Message } from '@messenger/database';
import type { ChatService } from '../../../modules/chats/chat.service.js';

export function createUpdateMessageStatusUseCase(deps: {
  messageService: MessageService;
  chatService: ChatService;
}) {
  return {
    async execute(
      messageId: bigint,
      receiverId: string,
      status: MessageStatus
    ): Promise<Message | undefined> {
      if (status === 'READ') {
        const message = await deps.messageService.updateMessageStatus(
          messageId,
          receiverId,
          status
        );

        if (!message) {
          return;
        }

        await deps.chatService.decrementUnreadForParticipant(message.chatId, receiverId);
        return message;
      }

      return await deps.messageService.updateMessageStatus(messageId, receiverId, status);
    },
  };
}
