package ru.connect.messenger.features.messaging.chat.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.core.exception.ResourceNotFoundException;
import ru.connect.messenger.features.messaging.message.dto.MessagesDeletedPayload;
import ru.connect.messenger.features.messaging.message.service.MessageService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class ChatCleanupService {
    private MessageService messageService;
    private ChatService chatService;
    private ChatParticipantService chatParticipantService;

    public void clearChatForUser(Long chatId, Long userId) {
        chatService.deleteChatForUser(chatId, userId);
        messageService.deleteChatMessagesForUser(chatId, userId);
        chatParticipantService.cleanUnreadCount(chatId, userId);
    }

    public void clearChatHistoryForUser(Long chatId, Long userId) {
        // Находим участников чата, проверяем участие пользователя в чате
        var participantsId = chatService.getChatParticipantsByChatId(chatId).stream()
                .map(p -> p.getId().getUserId())
                .toList();
        if (participantsId.contains(userId)) {
            messageService.deleteChatMessagesForUser(chatId, userId);
            chatParticipantService.cleanUnreadCount(chatId, userId);
            chatParticipantService.setIsChatEmpty(chatId, userId, true);
        } else {
            throw new AuthorizationDeniedException("Не достаточно прав для доступа");
        }
    }

    public MessagesDeletedPayload deleteMessages(List<Long> messageIds, Long chatId, Long currentUserId) {
        // Находим id собеседника
        var participantsId = chatService.getChatParticipantsByChatId(chatId).stream()
                .map(p -> p.getId().getUserId())
                .toList();

        if (!participantsId.contains(currentUserId)) {
            throw new AccessDeniedException("Нет доступа для удаления сообщения!");
        }

        Long receiverId = participantsId.stream()
                .filter(p -> !Objects.equals(p, currentUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Участник не найден"));

        messageService.deleteMessages(messageIds);

        // Обновляем последнее сообщение чата, время обновления, колличество не прочитанных сообщений
        var lastMessage = messageService.getLastChatMessage(chatId);
        var updatedAt = OffsetDateTime.now();
        var messageText = "Нет сообщений";
        if (lastMessage != null) {
            updatedAt = lastMessage.getCreatedAt();
            messageText = lastMessage.getText();

        }
        chatService.updateLastMessage(chatId, messageText, updatedAt);
        chatParticipantService.decreaseUnreadCount(chatId, receiverId, messageIds.size());
        var unreadCount = chatParticipantService.getUnreadCount(chatId, receiverId);
        log.info("Unread count: {} ", unreadCount);
        return new MessagesDeletedPayload(
                receiverId,
                updatedAt,
                unreadCount,
                messageText);
    }
}
