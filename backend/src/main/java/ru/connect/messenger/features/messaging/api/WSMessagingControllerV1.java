package ru.connect.messenger.features.messaging.api;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;
import ru.connect.messenger.features.messaging.chat.mapper.ChatMapper;
import ru.connect.messenger.features.messaging.chat.service.ChatParticipantService;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.dto.AllMessagesReadPayload;
import ru.connect.messenger.features.messaging.message.dto.MessageDeliveredPayload;
import ru.connect.messenger.features.messaging.message.dto.MessageReadPayload;
import ru.connect.messenger.features.messaging.message.dto.SendMessageRequest;
import ru.connect.messenger.features.messaging.message.dto.TypingPayload;
import ru.connect.messenger.features.messaging.message.mapper.MessageMapper;
import ru.connect.messenger.features.messaging.message.service.MessageReplyService;
import ru.connect.messenger.features.messaging.message.service.MessageServiceImpl;
import ru.connect.messenger.features.notification.NotificationService;
import ru.connect.messenger.features.user.api.UserBlockChecker;
import ru.connect.messenger.features.messaging.MessagingOrchestrator;
import ru.connect.messenger.shared.dto.ErrorResponse;
import ru.connect.messenger.shared.dto.WSEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WSMessagingControllerV1 {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageServiceImpl messageService;
    private final ChatService chatService;
    private final MessageMapper messageMapper;
    private final ChatParticipantService chatParticipantService;
    private final ChatMapper chatMapper;
    private final MessageReplyService messageReplyService;
    private final UserBlockChecker userBlockChecker;
    private final NotificationService notificationService;
    private final MessagingOrchestrator messagingOrchestrator;

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
        var sentMessage = messagingOrchestrator.sendMessage(request, principal);

        var additionalData = Map.of(
                "type", "TEXT_MESSAGE",
                "action", "OPEN_PUSH_WINDOW"
        );
        notificationService.sendPushToUsers(
                List.of(String.valueOf(sentMessage.getReceiverId())),
                "Новое сообщение",
                sentMessage.getText(),
                additionalData,
                "push"
        );
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
            Message message = messageService.getMessageById(Long.valueOf(payload.getMessageId()));

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
            chatParticipantService.decrementUnreadCount(Long.parseLong(payload.getChatId()), readerId);
            Message message = messageService.getMessageById(Long.valueOf(payload.getMessageId()));

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

    // Подтверждение прочтения всех сообщений чата получателем
    @MessageMapping("/all_messages_read")
    @Transactional
    public void handleAllMessageRead(
            @Payload WSEvent<AllMessagesReadPayload> event,
            Principal principal
    ) {
        AllMessagesReadPayload payload = event.getPayload();
        Long receiverId = Long.valueOf(principal.getName());

        log.info("👁️ AllChatMessages read: {} by user {}", payload.getChatId(), receiverId);

        try {
            // Обновляем статус в БД
            messageService.markAllAsReadByReceiver(Long.valueOf(payload.getChatId()), receiverId);
            chatParticipantService.cleanUnreadCount(Long.valueOf(payload.getChatId()), receiverId);

            var chat = chatService.findChatById(Long.valueOf(payload.getChatId()));
            ChatParticipant sender = Objects.requireNonNull(chat.getParticipants().stream()
                    .filter(p -> !Objects.equals(p.getId().getUserId(), receiverId))
                    .findFirst()
                    .orElse(null));
            var senderId = sender.getId().getUserId();

            // Пересылаем уведомление отправителю
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ALL_MESSAGE_READ, payload)
            );

        } catch (Exception e) {
            log.error("❌ Error processing read confirmation", e);

            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to process read confirmation")
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
            // Отправляем уведомление участнику, кроме печатающего
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

            // Отправляем уведомление участнику, кроме печатающего
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
