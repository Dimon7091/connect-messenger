package ru.gorbunov.connect.web.controller.api.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.ws.ErrorResponse;
import ru.gorbunov.connect.core.dto.ws.MessageDeletedResponse;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.dto.ws.MessagesDeletedRequest;
import ru.gorbunov.connect.core.dto.ws.WSEvent;
import ru.gorbunov.connect.core.mapper.MessageMapper;
import ru.gorbunov.connect.core.service.MessageService;
import ru.gorbunov.connect.core.service.orchestrators.ChatCleanupService;
import ru.gorbunov.connect.core.service.orchestrators.MessageReplyService;
import ru.gorbunov.connect.core.util.DateUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
public class MessageControllerV1 {
    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper mapper;

    @Autowired
    private ChatCleanupService chatCleanupService;

    @Autowired
    private MessageReplyService messageReplyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/chats/{id}")
    public List<MessageNewResponse> getChatMessages(
            @PathVariable("id") Long chatId,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "beforeTimestamp", required = false) String beforeTimestamp,
            @AuthenticationPrincipal Jwt token
    ) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        OffsetDateTime timestamp = (beforeTimestamp != null)
                ? DateUtils.parseTimestamp(beforeTimestamp) : OffsetDateTime.now();
        var messages = messageService.findChatMessages(
                chatId,
                limit,
                timestamp,
                currentUserId
        );
        return messages.stream()
                .map(m -> mapper.toDto(m))
                .peek(m -> Optional.ofNullable(m.getReplyToId())
                        .map(Long::valueOf)
                        .ifPresent(id -> m.setReplyContext(messageReplyService.getReplyContext(id))))
                .toList();
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessages(
            @RequestBody MessagesDeletedRequest requestData,
            @AuthenticationPrincipal Jwt token
    ) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        var deletionResult = chatCleanupService.deleteMessages(
                requestData.messagesIds(),
                requestData.chatId(),
                currentUserId);

        var messagesDeletedResponse = new MessageDeletedResponse(
                requestData.chatId().toString(),
                requestData.messagesIds(),
                deletionResult.chatUpdatedAt(),
                deletionResult.unreadCount(),
                deletionResult.lastMessage());
        // Отправляем участникам чата
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(deletionResult.receiverId()),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.MESSAGE_DELETED, messagesDeletedResponse)
            );
        } catch (Exception e) {
            log.error("❌ Error deleting message(s)", e);

            // Отправляем ошибку удаляющему сообщение(я)
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to delete message(s)")
                    .body(e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(currentUserId),
                    "/queue/private",
                    new WSEvent<>(WSEvent.EventType.ERROR, error)
            );
        }
    }
}
