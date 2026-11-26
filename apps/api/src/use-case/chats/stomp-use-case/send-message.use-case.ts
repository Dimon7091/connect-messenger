import type {
  MessageNewResponse,
  SendMessageDto,
  SendMessageRequest,
} from '@messenger/shared-schema';
import type { MessageService } from '../../../modules/messages/message.service.js';
import type { ChatService } from '../../../modules/chats/chat.service.js';

export function createSendMessageUseCase(deps: {
  messageService: MessageService;
  chatService: ChatService;
}) {
  return {
    async send(input: SendMessageDto): Promise<MessageNewResponse> {
      // Проверяем являются ли пользователи участниками чата
      await deps.chatService.ensureParticipant(input.chatId, input.senderId);
      await deps.chatService.ensureParticipant(input.chatId, input.receiverId);

      const newMessage = await deps.messageService.createMessage(input);
      const chat = await deps.chatService.onNewMessage(
        input.chatId,
        input.receiverId,
        input.text ?? ''
      );

      const receiverParticipant = chat.participants.find(p => p.userId !== input.senderId);
      const participantsIds = chat.participants.map(p => p.userId);

      // Вычисляем replyContext, если есть replyToId
      let replyContext: MessageNewResponse['message']['replyContext'] = undefined;
      if (input.replyToId) {
        try {
          const repliedMessage = await deps.messageService.getMessageById(input.replyToId);
          if (repliedMessage && repliedMessage.chatId.toString() === newMessage.chatId.toString()) {
            replyContext = {
              originalMessageId: repliedMessage.id.toString(),
              senderName: repliedMessage.senderId,
              textSnippet: (repliedMessage.text ?? '').substring(0, 100),
            };
          }
        } catch {
          // Если цитируемое сообщение не найдено — просто игнорируем replyContext
        }
      }

      const response = {
        chat: {
          id: chat.id.toString(),
          type: chat.type,
          participants: participantsIds,
          unreadCount: receiverParticipant?.unreadCount ?? 0,
          createdAt: chat.createdAt.toISOString(),
          updatedAt: chat.updatedAt.toISOString(),
          createdBy: chat.createdBy,
          lastMessage: newMessage.text ?? '',
        },
        message: {
          id: newMessage.id.toString(),
          chatId: newMessage.chatId.toString(),
          createdAt: newMessage.createdAt.toISOString(),
          senderId: newMessage.senderId,
          receiverId: newMessage.receiverId,
          timestamp: newMessage.timestamp.toISOString(),
          status: newMessage.status,
          deletedBy: newMessage.deletedBy ?? [],
          text: newMessage.text ?? null,
          replyToId: input.replyToId?.toString() ?? undefined,
          attachments: newMessage.attachments,
          readBy: newMessage.readBy ?? [input.senderId],
          replyContext: replyContext ?? null,
        },
      };

      return response;
    },
  };
}
