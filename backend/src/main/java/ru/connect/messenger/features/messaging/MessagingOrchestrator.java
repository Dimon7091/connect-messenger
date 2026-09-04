package ru.connect.messenger.features.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.connect.messenger.core.exception.UserBlockedException;
import ru.connect.messenger.features.messaging.api.ChatService;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.features.messaging.chat.mapper.ChatMapper;
import ru.connect.messenger.features.messaging.chat.service.ChatParticipantService;
import ru.connect.messenger.features.messaging.message.domain.Attachment;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.domain.MessageStatus;
import ru.connect.messenger.features.messaging.message.domain.ReplyContext;
import ru.connect.messenger.features.messaging.message.dto.MessageNewResponse;
import ru.connect.messenger.features.messaging.message.dto.MessageSentResponse;
import ru.connect.messenger.features.messaging.message.dto.SendMessageRequest;
import ru.connect.messenger.features.messaging.message.mapper.MessageMapper;
import ru.connect.messenger.features.messaging.message.service.MessageReplyService;
import ru.connect.messenger.features.messaging.message.service.MessageServiceImpl;
import ru.connect.messenger.features.user.api.UserBlockChecker;
import ru.connect.messenger.shared.dto.ErrorResponse;
import ru.connect.messenger.shared.dto.WSEvent;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Service
public class MessagingOrchestrator {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageServiceImpl messageService;
    private final ChatService chatService;
    private final MessageMapper messageMapper;
    private final ChatParticipantService chatParticipantService;
    private final ChatMapper chatMapper;
    private final MessageReplyService messageReplyService;
    private final UserBlockChecker userBlockChecker;

    public MessageNewResponse sendMessage(WSEvent<SendMessageRequest> request, Principal principal) {
        SendMessageRequest payload = request.getPayload();
        long senderId = Long.parseLong(principal.getName());
        long receiverId = Long.parseLong(payload.getReceiverId());
        long chatId = Long.parseLong(payload.getChatId());

        MessageNewResponse newMessageResponse = new MessageNewResponse();

        try {
            log.debug("\uD83D\uDCE8 Message sending has started, chatId: {}", payload.getChatId());
            // Проверка на блокировку между пользователями
            if (userBlockChecker.isEitherBlocked(senderId, receiverId)) {
                log.debug("❌ User blocking detected, message sending was stopped  chatId: {}", payload.getChatId());
                throw new UserBlockedException("blocked by user");
            }

            // Сохраняем сообщение в БД
            Message savedMessage = messageService.createMessage(payload);
            // Обновление метаданных чата

            String lastMessage = buildLastMessage(payload.getAttachments(), payload.getText());

            chatService.updateLastMessage(
                    Long.parseLong(payload.getChatId()),
                    lastMessage,
                    OffsetDateTime.parse(payload.getTimestamp()));
            chatParticipantService.incrementUnreadCount(chatId, receiverId);
            chatParticipantService.setIsChatEmpty(chatId, senderId, false);
            chatParticipantService.setIsChatEmpty(chatId, receiverId, false);

            // Отправляем подтверждение отправки отправителю (MessageSentResponse)
            MessageSentResponse sentResponse = MessageSentResponse.builder()
                    .messageId(payload.getMessageId())      // временный ID от клиента
                    .serverMessageId(savedMessage.getId().toString())  // реальный ID из БД
                    .status(MessageStatus.SENT)
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC).toString())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_SENT, sentResponse)
            );

            // Формируем сообщение ответ для получателя
            newMessageResponse = messageMapper.toDto(savedMessage);
            // Если сообщение это ответ на другое сообщение добовляем ReplyContext
            if (payload.getReplyToId() != null) {
                ReplyContext replyContext = messageReplyService.getReplyContext(
                        Long.valueOf(payload.getReplyToId())
                );
                newMessageResponse.setReplyContext(replyContext);
            }

            Chat chat = chatService.findChatById(Long.valueOf(payload.getChatId()));
            ChatResponse chatResponse = chatMapper.toDto(chat);
            chatResponse.setUnreadCount(chatParticipantService.getUnreadCount(chat.getId(), receiverId));
            chatResponse.setLastMessage(lastMessage);
            chatResponse.setUpdatedAt(chat.getUpdatedAt());
            newMessageResponse.setChat(chatResponse);
            newMessageResponse.setStatus(MessageStatus.SENT);
            newMessageResponse.setAttachments(payload.getAttachments());
            // Устанавливаем статус чата если он удален у пользователя
            chatParticipantService.setIsDeleted(chat.getId(), receiverId, false);

            // Отправляем сообщение получателю
            messagingTemplate.convertAndSendToUser(
                    payload.getReceiverId(),  // ← отправляем конкретному получателю
                    "/queue/private",            // ← в его личную очередь
                    new WSEvent<>(WSEvent.EventType.MESSAGE_NEW, newMessageResponse)
            );

            log.debug("✅ New message sent to user {}, payload: {}",
                    payload.getReceiverId(), newMessageResponse);

        } catch (Exception e) {
            log.error("❌ Error sending messaging", e);

            // Отправляем ошибку отправителю
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to send messaging")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ERROR, error)
            );
        }
        return newMessageResponse;
    }

    private String buildLastMessage(List<Attachment> attachments, String messageText) {
        StringBuilder lastMessage = new StringBuilder();
        if (!attachments.isEmpty()) {
            Map<String, Integer> typeCount = new LinkedHashMap<>();

            for (var attachment : attachments) {
                String mimeType = attachment.getMimeType();
                if (mimeType.contains("image")) {
                    typeCount.merge("Фото", 1, Integer::sum);
                } else if (mimeType.contains("video")) {
                    typeCount.merge("Видео", 1, Integer::sum);
                } else {
                    typeCount.merge("Фаил", 1, Integer::sum);
                }
            }

            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                var iconType = switch (entry.getKey()) {
                    case "Фото" -> "\uD83C\uDF04";
                    case "Видео" -> "\uD83C\uDF9E\uFE0F";
                    default -> "\uD83D\uDCC4";
                };

                lastMessage.append(iconType).append(" ").append(entry.getValue())
                        .append(" ").append(entry.getKey()).append(", ");
            }
        } else {
            String text = messageText != null ? messageText : "";
            lastMessage = new StringBuilder(((text.length() > 40) ? text.substring(0, 40) + "..." : text));
        }
        return lastMessage.toString();
    }
}
