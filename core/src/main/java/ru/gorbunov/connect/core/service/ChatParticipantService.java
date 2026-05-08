package ru.gorbunov.connect.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.ChatParticipantId;
import ru.gorbunov.connect.core.repository.ChatParticipantRepository;

@Service
public class ChatParticipantService {
    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    public void incrementUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.incrementUnreadCount(participantId);
    }

    public int getUnreadCount(Long chatId, Long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        return chatParticipantRepository.getUnreadCount(participantId);
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
}
