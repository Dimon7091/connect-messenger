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
import java.util.List;
import java.util.NoSuchElementException;

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

    public void markAsDelivered(Long messageId) {
        messageRepository.updateStatus(messageId, MessageStatus.DELIVERED.name());
    }

    public void markAsRead(Long messageId, Long readerId) {
        messageRepository.addReaderByUser(messageId, readerId);
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


}
