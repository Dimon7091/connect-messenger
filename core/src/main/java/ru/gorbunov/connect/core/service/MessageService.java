package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.payload.MessageDeletedState;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.mapper.MessageMapper;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.models.MessageStatus;
import ru.gorbunov.connect.core.repository.MessageRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Transactional
@AllArgsConstructor
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper mapper;

    public Message createMessage(SendMessageRequest requestData) {
        Message newMessage = mapper.toEntity(requestData);

        newMessage.setCreatedAt(OffsetDateTime.now());
        if (requestData.getReplyToId() != null) {
            newMessage.setReplyToId(Long.valueOf(requestData.getReplyToId()));
        }

        if (requestData.getAttachments() != null) {
            newMessage.setAttachments(requestData.getAttachments());
        }

        return messageRepository.save(newMessage);
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(NoSuchElementException::new);
    }

    public List<Message> findChatMessages(
            Long chatId,
            Integer limit,
            OffsetDateTime beforeTimestamp,
            Long userId
    ) {
        var messages = messageRepository.findChatMessages(chatId, limit, beforeTimestamp);
        return messages.stream()
                .filter(m -> m.getDeletedBy() == null || !m.getDeletedBy().contains(userId))
                .toList();
    }

    public Message getLastChatMessage(Long chatId) {
        return messageRepository.findLastChatMessage(chatId)
                .orElse(null);
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
        }
        if (!readBy.contains(readerId)) {
            readBy.add(readerId);
            message.setReadBy(readBy);
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        }
    }

    public void markAllAsReadByReceiver(Long chatId, Long receiverId) {
        messageRepository.markAllAsReadByReceiver(chatId, MessageStatus.READ, receiverId);
    }

    public void deleteMessages(List<Long> messagesIds) {
        messageRepository.deleteAllByIdInBatch(messagesIds);
    }

    public void deleteChatMessagesForUser(Long chatId, Long userId) {
        // 1. Легкая выгрузка только нужных полей
        List<MessageDeletedState> states = messageRepository.findDeletedStatesByChatId(chatId);

        List<Long> messagesToDelete = new ArrayList<>();

        for (MessageDeletedState state : states) {
            List<Long> deletedBy = new ArrayList<>();
            if (state.deletedBy() != null) {
                deletedBy.addAll(state.deletedBy());
            }

            if (!deletedBy.contains(userId)) {
                deletedBy.add(userId);

                // 2. Проверяем, нужно ли совсем удалить сообщение
                if (deletedBy.contains(state.senderId()) && deletedBy.contains(state.receiverId())) {
                    messagesToDelete.add(state.id());
                } else {
                    // 3. Если нет, обновляем список (уйдет в Batch)
                    messageRepository.updateDeletedBy(state.id(), deletedBy);
                }
            }
        }
        // 4. Массовое удаление (тоже уйдет батчем)
        if (!messagesToDelete.isEmpty()) {
            messageRepository.deleteAllByIdInBatch(messagesToDelete);
        }
    }
}
