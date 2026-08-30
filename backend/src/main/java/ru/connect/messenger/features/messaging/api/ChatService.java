package ru.connect.messenger.features.messaging.api;

import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatService {
    Chat createOrGetDirectChat(Long userId1, Long userId2);
    Chat findChatById(Long chatId);
    Chat findChatByParticipants(long userId1, long userId2);
    List<ChatResponse> findAllDirectChatsByUser(Long userId);
    List<ChatParticipant> getChatParticipantsByChatId(Long chatId);
    void updateLastMessage(long chatId, String message, OffsetDateTime timestamp);
    void deleteChatForUser(Long chatId, Long userId);
}