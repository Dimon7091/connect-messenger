package ru.connect.messenger.features.messaging.message.api;

import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.dto.SendMessageRequest;
import java.time.OffsetDateTime;
import java.util.List;

public interface MessageService {
    Message createMessage(SendMessageRequest requestData);
    Message getMessageById(Long messageId);
    List<Message> findChatMessages(Long chatId, Integer limit, OffsetDateTime beforeTimestamp, Long currentUserId);
    Message getLastChatMessage(Long chatId);
    int getUnreadCountInChat(Long chatId, Long userId);
    void markAsDelivered(Long messageId);
    void markAsRead(Long messageId, Long readerId);
    void markAllAsReadByReceiver(Long chatId, Long receiverId);
    void deleteMessages(List<Long> messagesIds);
    void deleteChatMessagesForUser(Long chatId, Long userId);
}