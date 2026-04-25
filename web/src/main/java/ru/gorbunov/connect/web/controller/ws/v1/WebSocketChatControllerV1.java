package ru.gorbunov.connect.web.controller.ws.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.gorbunov.connect.core.dto.ws.ChatHistoryRequest;
import ru.gorbunov.connect.core.dto.ws.ChatHistoryResponse;
import ru.gorbunov.connect.core.dto.ws.ErrorResponse;
import ru.gorbunov.connect.core.dto.ws.MessageDeliveredPayload;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.dto.ws.MessageReadPayload;
import ru.gorbunov.connect.core.dto.ws.MessageSentResponse;
import ru.gorbunov.connect.core.models.MessageStatus;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.dto.ws.TypingPayload;
import ru.gorbunov.connect.core.dto.ws.WSEvent;
import ru.gorbunov.connect.core.mapper.MessageMapper;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.MessageService;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatControllerV1 {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatService chatService;
    private final MessageMapper messageMapper;

    /**
     * 1. Отправка сообщения (send_message)
     * Клиент → Сервер: SendMessageRequest
     * Сервер → Отправитель: MessageSentResponse
     * Сервер → Получатель: MessageNewResponse
     */
    @MessageMapping("/message_sent")
    public void handleSendMessage(
            @Payload SendMessageRequest request,
            Principal principal
    ) {
        Long senderId = Long.valueOf(principal.getName());

        log.info("📨 Send message from user {} to chat {}", senderId, request.getChatId());

        try {
            // 1. Сохраняем сообщение в БД
            Message savedMessage = messageService.createMessage(request);

            // 2. Отправляем подтверждение отправки отправителю (MessageSentResponse)
            MessageSentResponse sentResponse = MessageSentResponse.builder()
                    .messageId(request.getMessageId())      // временный ID от клиента
                    .serverMessageId(savedMessage.getId().toString())  // реальный ID из БД
                    .status(MessageStatus.SENT)
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC).toString())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_SENT, sentResponse)
            );

            // 3. Отправляем новое сообщение всем участникам чата (MessageNewResponse)
            MessageNewResponse newResponse = messageMapper.toDto(savedMessage);
            // Отправляем в общий топик чата
            messagingTemplate.convertAndSendToUser(
                    request.getReceiverId(),  // ← отправляем конкретному получателю
                    "/queue/private",            // ← в его личную очередь
                    new WSEvent<>(WSEvent.EventType.MESSAGE_NEW, newResponse)
            );

            log.info("✅ Message sent successfully: {}", savedMessage.getId());

        } catch (Exception e) {
            log.error("❌ Error sending message", e);

            // Отправляем ошибку отправителю
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to send message")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ERROR, error)
            );
        }
    }

    /**
     * 2. Подтверждение доставки (message_delivered)
     * Получатель → Сервер: MessageDeliveredPayload
     * Сервер → Отправитель: MessageDeliveredPayload
     */
    @MessageMapping("/message_delivered")
    public void handleMessageDelivered(
            @Payload WSEvent<MessageDeliveredPayload> event,
            Principal principal
    ) {
        MessageDeliveredPayload payload = event.getPayload();
        Long receiverId = Long.valueOf(principal.getName());

        log.info("✅ Message delivered: {} to user {}", payload.getMessageId(), receiverId);

        try {
            // Обновляем статус в БД
            messageService.markAsDelivered(Long.valueOf(payload.getMessageId()));

            // Получаем сообщение, чтобы узнать отправителя
            Message message = messageService.findById(Long.valueOf(payload.getMessageId()));

            // Пересылаем уведомление отправителю
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getSenderId()),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_DELIVERED, payload)
            );

        } catch (Exception e) {
            log.error("❌ Error processing delivery confirmation", e);

            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to process delivery confirmation")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ERROR, error)
            );
        }
    }

    /**
     * 3. Подтверждение прочтения (message_read)
     * Получатель → Сервер: MessageReadPayload
     * Сервер → Отправитель: MessageReadPayload
     */
    @MessageMapping("/message_read")
    public void handleMessageRead(
            @Payload WSEvent<MessageReadPayload> event,
            Principal principal
    ) {
        MessageReadPayload payload = event.getPayload();
        Long readerId = Long.valueOf(principal.getName());

        log.info("👁️ Message read: {} by user {}", payload.getMessageId(), readerId);

        try {
            // Обновляем статус в БД
            messageService.markAsRead(Long.valueOf(payload.getMessageId()), readerId);

            // Получаем сообщение, чтобы узнать отправителя
            Message message = messageService.findById(Long.valueOf(payload.getMessageId()));

            // Пересылаем уведомление отправителю
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getSenderId()),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_READ, payload)
            );

        } catch (Exception e) {
            log.error("❌ Error processing read confirmation", e);

            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to process read confirmation")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(readerId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ERROR, error)
            );
        }
    }

    /**
     * 4. Начало печатания (typing_start)
     * Печатающий → Сервер: TypingPayload
     * Сервер → Остальные участники: TypingPayload
     */
    @MessageMapping("/typing_start")
    public void handleTypingStart(
            @Payload WSEvent<TypingPayload> event,
            Principal principal
    ) {
        TypingPayload payload = event.getPayload();
        Long typingUserId = Long.valueOf(principal.getName());

        log.debug("✏️ User {} started typing in chat {}", typingUserId, payload.getChatId());

        try {
            // Получаем чат
            Chat chat = chatService.findChatById(Long.valueOf(payload.getChatId()));

            // Отправляем уведомление всем участникам, кроме печатающего
            for (ChatParticipant participant : chat.getParticipants()) {
                var participantId = participant.getId().getUserId();
                if (!participantId.equals(typingUserId)) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(participantId),
                            "/queue/private",
                            new WSEvent<>(WSEvent.EventType.TYPING_START, payload)
                    );
                }
            }

        } catch (Exception e) {
            log.error("❌ Error processing typing start", e);
        }
    }

    /**
     * 5. Остановка печатания (typing_stop)
     * Печатающий → Сервер: TypingPayload
     * Сервер → Остальные участники: TypingPayload
     */
    @MessageMapping("/typing_stop")
    public void handleTypingStop(
            @Payload WSEvent<TypingPayload> event,
            Principal principal
    ) {
        TypingPayload payload = event.getPayload();
        Long typingUserId = Long.valueOf(principal.getName());

        log.debug("✏️ User {} stopped typing in chat {}", typingUserId, payload.getChatId());

        try {
            // Получаем чат
            Chat chat = chatService.findChatById(Long.valueOf(payload.getChatId()));

            // Отправляем уведомление всем участникам, кроме печатающего
            for (ChatParticipant participant : chat.getParticipants()) {
                var participantId = participant.getId().getUserId();
                if (!participantId.equals(typingUserId)) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(participantId),
                            "/queue/private",
                            new WSEvent<>(WSEvent.EventType.TYPING_STOP, payload)
                    );
                }
            }

        } catch (Exception e) {
            log.error("❌ Error processing typing stop", e);
        }
    }

    /**
     * 6. Загрузка истории чата (chat_history)
     * Клиент → Сервер: ChatHistoryRequest
     * Сервер → Клиент: ChatHistoryResponse
     */
    @MessageMapping("/chat_history")
    public void handleChatHistory(
            @Payload WSEvent<ChatHistoryRequest> event,
            Principal principal
    ) {
        ChatHistoryRequest request = event.getPayload();
        Long userId = Long.valueOf(principal.getName());

        log.info("📚 User {} requesting history for chat {}", userId, request.getChatId());

        try {
            // Загружаем историю сообщений
            List<Message> messages = messageService.findChatMessages(
                    Long.valueOf(request.getChatId()),
                    request.getLimit() != null ? request.getLimit() : 50,
                    OffsetDateTime.parse(request.getBeforeTimestamp())
            );

            // Конвертируем в DTO
            var messageDTOs = messages.stream()
                    .map(messageMapper::toDto)
                    .toList();

            // Считаем непрочитанные
            int unreadCount = messageService.getUnreadCountInChat(Long.valueOf(request.getChatId()), userId);

            // Отправляем ответ
            ChatHistoryResponse response = ChatHistoryResponse.builder()
                    .chatId(request.getChatId())
                    .messages(messageDTOs)
                    .hasMore(messages.size() == (request.getLimit() != null ? request.getLimit() : 50))
                    .unreadCount(unreadCount)
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.CHAT_STORE, response)
            );

        } catch (Exception e) {
            log.error("❌ Error loading chat history", e);

            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to load chat history")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.CHAT_STORE, error)
            );
        }
    }
}
