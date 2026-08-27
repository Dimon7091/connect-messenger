package ru.connect.messenger.features.messaging.chat.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipantId;
import ru.connect.messenger.features.messaging.chat.repository.ChatParticipantRepository;

@Service
@AllArgsConstructor
public class ChatParticipantService {
    private final ChatParticipantRepository chatParticipantRepository;

    public void incrementUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.incrementUnreadCount(participantId);
    }

    public int getUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        return chatParticipantRepository.getUnreadCount(participantId);
    }

    public boolean getIsChatCleared(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        return chatParticipantRepository.getIsChatEmpty(participantId);
    }

    public void setIsDeleted(Long chatId, Long userId, boolean status) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.updateIsDeleted(participantId, status);
    }

    public void cleanUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.cleanUnreadCount(participantId);
    }

    public void decreaseUnreadCount(Long chatId, Long userId, Integer count) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.decreaseUnreadCount(participantId, count);
    }

    public void decrementUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.decrementUnreadCount(participantId);
    }

    public void setIsChatEmpty(Long chatId, Long userId, boolean status) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.updateIsChatEmpty(participantId, status);
    }
}
