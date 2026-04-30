package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.mapper.ChatMapper;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.ChatParticipantId;
import ru.gorbunov.connect.core.repository.ChatParticipantRepository;
import ru.gorbunov.connect.core.repository.ChatRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private RequestToViewNameTranslator requestToViewNameTranslator;

    @Autowired
    private ChatMapper mapper;

    @Transactional
    public Chat createOrGetDirectChat(Long userId1, Long userId2) {
        // 1. Генерируем детерминированный ключ (всегда "меньшийID:большийID")
        String directKey = Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);

        // 2. Сначала ищем по ключу
        return chatRepository.findByDirectKey(directKey)
                .orElseGet(() -> {
                    try {
                        // 3. Если не нашли, создаем новый
                        Chat chat = new Chat();
                        chat.setDirectKey(directKey);
                        chat.setType("DIRECT");
                        chat.setCreatedAt(OffsetDateTime.now());
                        Chat savedChat = chatRepository.save(chat); // Здесь может вылететь Exception

                        // Создание первого участника чата
                        var chatParticipantId1 = new ChatParticipantId(savedChat.getId(), userId1);
                        var chatParticipant1 = new ChatParticipant();
                        chatParticipant1.setChat(savedChat);
                        chatParticipant1.setId(chatParticipantId1);
                        chatParticipant1.setIsDeleted(false);
                        chatParticipant1.setIsMuted(false);

                        // Создание второго участника чата
                        var chatParticipantId2 = new ChatParticipantId(savedChat.getId(), userId2);
                        var chatParticipant2 = new ChatParticipant();
                        chatParticipant2.setChat(savedChat);
                        chatParticipant2.setId(chatParticipantId2);
                        chatParticipant2.setIsDeleted(false);
                        chatParticipant2.setIsMuted(false);

                        savedChat.addParticipant(chatParticipant1);
                        savedChat.addParticipant(chatParticipant2);
                        return savedChat;
                    } catch (DataIntegrityViolationException e) {
                        // 4. Если за это время кто-то уже успел создать такой чат,
                        // просто находим его снова
                        return chatRepository.findByDirectKey(directKey)
                                .orElseThrow(() -> new RuntimeException("Ошибка конкуренции"));
                    }
                });
    }

    public Chat findChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .orElse(null);
    }

    public Chat findChatByParticipants(long userId1, long userId2) {
        return chatRepository.findChatByParticipants(userId1, userId2)
                .orElseThrow(() -> new ResourceNotFoundException("чат не найден"));
    }

    public List<Chat> findAllDirectChatsByUser(Long userId) {
        return chatRepository.findChatsByUserIdNative(userId);
    }

    public void updateLastMessage(long chatId, String message, OffsetDateTime timestamp) {
        chatRepository.updateLastMessageOnly(chatId, message, timestamp);
    }

    public void updateUpdatedAt() {

    }

    public void deleteChatForUser(Long chatId, Long userId) {
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("чат не найден"));
        var currentParticipant = chat.getParticipants().stream()
                .filter(p -> Objects.equals(p.getId().getUserId(), userId))
                .findFirst();
        // Если пользователь есть в чате помечаем как удаленный
        if (currentParticipant.isPresent()) {
            currentParticipant.get().setIsDeleted(true);
            currentParticipant.get().setDeletedAt(LocalDateTime.now());
        } else {
            throw new ResourceNotFoundException("пользователь не состоит в чате");
        }

        // Если чат помечен как удаленный у обоих участников то удаляем чат
        var isChatDeleteByAllUsers = chat.getParticipants().stream()
                .allMatch(p -> p.getIsDeleted() == true);
        if (isChatDeleteByAllUsers) {
            chatRepository.deleteById(chatId);
        }
    }
}
