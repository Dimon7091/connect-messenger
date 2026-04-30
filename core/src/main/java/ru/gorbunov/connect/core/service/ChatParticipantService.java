package ru.gorbunov.connect.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.ChatParticipantId;
import ru.gorbunov.connect.core.repository.ChatParticipantRepository;

@Service
public class ChatParticipantService {
    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    public void addUnreadCount(long chatId, long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.incrementUnreadCount(participantId);
    }

    public void deleteUnreadCount(long chatId, long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        chatParticipantRepository.decrementUnreadCount(participantId);
    }

    public int getUnreadCount(long chatId, long userId) {
        var participantId = new ChatParticipantId(chatId, userId);
        return chatParticipantRepository.getUnreadCount(participantId);
    }
}
