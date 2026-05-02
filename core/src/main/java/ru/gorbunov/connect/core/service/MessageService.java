package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.models.MessageStatus;
import ru.gorbunov.connect.core.repository.MessageRepository;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
@Transactional
@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public Message createMessage(SendMessageRequest requestData) {
        Message message = Message.builder()
                .chatId(Long.valueOf(requestData.getChatId()))
                .senderId(Long.valueOf(requestData.getSenderId()))
                .receiverId(Long.valueOf(requestData.getReceiverId()))
                .text(requestData.getText())
                .status(MessageStatus.SENT)
                .replyToId(requestData.getReplyToId() != null ? Long.valueOf(requestData.getReplyToId()) : null)
                .timestamp(requestData.getTimestamp() != null ? OffsetDateTime.parse(requestData.getTimestamp()) : null)
                .createdAt(OffsetDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    public Message findById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(NoSuchElementException::new);
    }

    public List<Message> findChatMessages(
            long chatId,
            int limit,
            OffsetDateTime beforeTimestamp
    ) {
        return messageRepository.findChatMessages(chatId, limit, beforeTimestamp);
    }

    public int getUnreadCountInChat(Long chatId, Long userId) {
        return messageRepository.countUnreadMessagesByUser(chatId, userId);
    }

    public void markAsDelivered(Long messageId) {
        messageRepository.updateStatus(messageId, MessageStatus.DELIVERED);
    }

    public void markAsRead(Long messageId, Long readerId) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        List<Long> readBy = message.getReadBy();
        if (readBy == null) {
            readBy = new ArrayList<>();
        };
        if (!readBy.contains(readerId)) {
            readBy.add(readerId);
            message.setReadBy(readBy);
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        }
    }

    public void markAsDeletedAllChatMessagesForUser(long chatId, long userId) {

    }
}
