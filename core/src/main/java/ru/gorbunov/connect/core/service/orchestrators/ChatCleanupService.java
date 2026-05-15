package ru.gorbunov.connect.core.service.orchestrators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.payload.MessagesDeletedPayload;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.repository.ChatRepository;
import ru.gorbunov.connect.core.service.ChatParticipantService;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.MessageService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ChatCleanupService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatParticipantService chatParticipantService;

    @Autowired
    private ChatRepository chatRepository;

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

    public MessagesDeletedPayload deleteMessages(List<Long> messageIds, Long chatId, Long userId) {
        // Находим id собеседника
        var participantsId = chatService.getChatParticipantsByChatId(chatId).stream()
                .map(p -> p.getId().getUserId())
                .toList();
        Long receiverId = participantsId.stream()
                .filter(p -> !Objects.equals(p, userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Участник не найден"));

        if (participantsId.contains(userId)) {
            messageService.deleteMessages(messageIds);
        }
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
