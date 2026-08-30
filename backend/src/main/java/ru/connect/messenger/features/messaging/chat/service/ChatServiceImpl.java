package ru.connect.messenger.features.messaging.chat.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.connect.messenger.core.exception.ResourceNotFoundException;
import ru.connect.messenger.features.messaging.api.ChatService;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipantId;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.features.messaging.chat.mapper.ChatMapper;
import ru.connect.messenger.features.messaging.chat.repository.ChatParticipantRepository;
import ru.connect.messenger.features.messaging.chat.repository.ChatRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatParticipantService chatParticipantService;
    private final ChatMapper mapper;

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
                        chatParticipant1.setIsChatEmpty(true);
                        chatParticipant1.setUnreadCount(0);

                        // Создание второго участника чата
                        var chatParticipantId2 = new ChatParticipantId(savedChat.getId(), userId2);
                        var chatParticipant2 = new ChatParticipant();
                        chatParticipant2.setChat(savedChat);
                        chatParticipant2.setId(chatParticipantId2);
                        chatParticipant2.setIsDeleted(false);
                        chatParticipant2.setIsMuted(false);
                        chatParticipant2.setIsChatEmpty(true);
                        chatParticipant2.setUnreadCount(0);

                        savedChat.addParticipant(chatParticipant1);
                        savedChat.addParticipant(chatParticipant2);
                        return chatRepository.save(savedChat);
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

    public List<ChatResponse> findAllDirectChatsByUser(Long userId) {
        var chats = chatRepository.findChatsByUserIdNative(userId);
        if (chats == null) {
            throw new ResourceNotFoundException("Чаты не найдены");
        }
        var filterDeletedChats = chats.stream()
                .filter(chat -> chat.getParticipants().stream()
                        .anyMatch(p -> Objects.equals(p.getId().getUserId(), userId)
                                && !Boolean.TRUE.equals(p.getIsDeleted())))
                .toList();

        return filterDeletedChats.stream()
                .map(chat -> {
                    var chatResponse = mapper.toDto(chat);
                    var unreadCount = chatParticipantService.getUnreadCount(chat.getId(), userId);
                    var lastMessage = chat.getLastMessage();
                    if (chatParticipantService.getIsChatCleared(chat.getId(), userId)) {
                        lastMessage = "Нет новых сообщений";
                    }
                    chatResponse.setUnreadCount(unreadCount);
                    chatResponse.setLastMessage(lastMessage);
                    chatResponse.setUpdatedAt(chat.getUpdatedAt());
                    return chatResponse;
                })
                .toList();
    }

    public List<ChatParticipant> getChatParticipantsByChatId(Long chatId) {
        return chatRepository.findParticipantsByChatId(chatId);
    }

    public void updateLastMessage(long chatId, String message, OffsetDateTime timestamp) {
        chatRepository.updateLastMessageOnly(chatId, message, timestamp);
    }

    public void deleteChatForUser(Long chatId, Long userId) {
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("чат не найден"));
        var optionalChatParticipant = chat.getParticipants().stream()
                .filter(p -> Objects.equals(p.getId().getUserId(), userId))
                .findFirst();
        // Если пользователь есть в чате помечаем как удаленный
        if (optionalChatParticipant.isPresent()) {
            var participant = optionalChatParticipant.get();
            participant.setIsDeleted(true);
            participant.setDeletedAt(OffsetDateTime.now());
            chatParticipantRepository.save(participant);
        } else {
            throw new ResourceNotFoundException("пользователь не состоит в чате");
        }

        // Если чат помечен как удаленный у обоих участников то удаляем чат
        var isChatDeleteByAllUsers = chat.getParticipants().stream()
                .allMatch(ChatParticipant::getIsDeleted);
        if (isChatDeleteByAllUsers) {
            chatRepository.deleteById(chatId);
        }
    }
}
