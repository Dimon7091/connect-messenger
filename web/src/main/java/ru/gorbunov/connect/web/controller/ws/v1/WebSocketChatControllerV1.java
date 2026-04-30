package ru.gorbunov.connect.web.controller.ws.v1;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.gorbunov.connect.core.dto.chat.ChatResponse;
import ru.gorbunov.connect.core.dto.ws.ErrorResponse;
import ru.gorbunov.connect.core.dto.ws.MessageDeliveredPayload;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.dto.ws.MessageReadPayload;
import ru.gorbunov.connect.core.dto.ws.MessageSentResponse;
import ru.gorbunov.connect.core.mapper.ChatMapper;
import ru.gorbunov.connect.core.models.MessageStatus;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.dto.ws.TypingPayload;
import ru.gorbunov.connect.core.dto.ws.WSEvent;
import ru.gorbunov.connect.core.mapper.MessageMapper;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.service.ChatParticipantService;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.MessageService;
import org.springframework.util.StopWatch;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatControllerV1 {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatService chatService;
    private final MessageMapper messageMapper;
    private final ChatParticipantService chatParticipantService;
    private final ChatMapper chatMapper;

    /**
     * 1. Отправка сообщения (send_message)
     * Клиент → Сервер: SendMessageRequest
     * Сервер → Отправитель: MessageSentResponse
     * Сервер → Получатель: MessageNewResponse
     */
    @MessageMapping("/message_sent")
    @Transactional
    public void handleSendMessage(
            @Payload WSEvent<SendMessageRequest> request,
            Principal principal
    ) {
        StopWatch sw = new StopWatch();
        sw.start("Message sent");
        SendMessageRequest payload = request.getPayload();
        long senderId = Long.parseLong(principal.getName());
        long receiverId = Long.parseLong(payload.getReceiverId());

        log.info("📨 Send message from user {} to chat {}", senderId, payload.getChatId());

        try {
            // 1. Сохраняем сообщение в БД
            Message savedMessage = messageService.createMessage(payload);

            // 2. Отправляем подтверждение отправки отправителю (MessageSentResponse)
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

            // 3. Отправляем новое сообщение всем участникам чата (MessageNewResponse)
            MessageNewResponse newResponse = messageMapper.toDto(savedMessage);
            // Добовляем непрочитанные сообщения, время обновления получателю и в чат
            var lastMessage = (payload.getText().length() > 40) ?
                    payload.getText().substring(0, 40) + "..." : payload.getText();
            chatService.updateLastMessage(
                    Long.parseLong(payload.getChatId()),
                    lastMessage,
                    OffsetDateTime.parse(payload.getTimestamp()));
            chatParticipantService.addUnreadCount(Long.parseLong(payload.getChatId()), receiverId);
            Chat chat = chatService.findChatById(Long.valueOf(payload.getChatId()));
            ChatResponse chatResponse = chatMapper.toDto(chat);
            chatResponse.setUnreadCount(chatParticipantService.getUnreadCount(chat.getId(), receiverId));
            chatResponse.setLastMessage(lastMessage);
            chatResponse.setUpdatedAt(chat.getUpdatedAt());
            newResponse.setChat(chatResponse);

            // Отправляем получателю чата
            messagingTemplate.convertAndSendToUser(
                    payload.getReceiverId(),  // ← отправляем конкретному получателю
                    "/queue/private",            // ← в его личную очередь
                    new WSEvent<>(WSEvent.EventType.MESSAGE_NEW, newResponse)
            );
            sw.stop();
            log.info("✅ Дата: {} ", newResponse.getChat().getUpdatedAt());
            log.info("✅ Время записи и отправки сообщения: {} мс", sw.getTotalTimeMillis());
            log.info("Sending MESSAGE_NEW to user {} via /queue/private, payload: {}", payload.getReceiverId(), newResponse);

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
            chatParticipantService.deleteUnreadCount(Long.parseLong(payload.getChatId()), readerId);
            Message message = messageService.findById(Long.valueOf(payload.getMessageId()));

            // Пересылаем уведомление отправителю
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getSenderId()),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_READ, payload)
            );

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(message.getReceiverId()),
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
    @Transactional
    public void handleTypingStart(
            @Payload WSEvent<TypingPayload> event,
            Principal principal
    ) {

        Long typingUserId = Long.valueOf(principal.getName());
        TypingPayload payload = event.getPayload();
        Chat chat = chatService.findChatById(Long.valueOf(payload.getChatId()));

        log.debug("✏️ User {} started typing in chat {}", typingUserId, payload.getChatId());

        try {
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
    @Transactional
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
}
