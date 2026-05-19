package ru.gorbunov.connect.web.controller.ws.v1;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.dto.ws.UserStatusPayload;
import ru.gorbunov.connect.core.dto.ws.WSEvent;
import ru.gorbunov.connect.core.models.UserStatus;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserStatusSubscriptionService;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WSUserStatusController {

    private final UserStatusSubscriptionService subscriptionService;
    private final StatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;

    // Подписка на статус определённого пользователя
    @MessageMapping("/user_status_subscribe")
    public void subscribeToUserStatus(@Payload WSEvent<SubscribeRequest> request, Principal principal) {
        Long subscriberId = Long.valueOf(principal.getName());
        Long targetUserId = request.getPayload().targetUserId;
        subscriptionService.subscribe(subscriberId, targetUserId);

        // Можно сразу отправить текущий статус пользователя, чтобы клиент отобразил его без ожидания события
        UserStatus currentStatus = statusService.getStatus(targetUserId);
        if (currentStatus == null) currentStatus = new UserStatus(targetUserId, UserStatus.Status.OFFLINE, OffsetDateTime.now());
        var payload = new UserStatusPayload(targetUserId, currentStatus.getStatus().toString(), currentStatus.getLastSeen());

        messagingTemplate.convertAndSendToUser(
                subscriberId.toString(),
                "/queue/status",
                new WSEvent<>(WSEvent.EventType.USER_STATUS, payload)
        );
    }

    // Отписка от статуса
    @MessageMapping("/user_status_unsubscribe")
    public void unsubscribeFromUserStatus(@Payload UnsubscribeRequest request, Principal principal) {
        Long subscriberId = Long.valueOf(principal.getName());
        subscriptionService.unsubscribe(subscriberId, request.getTargetUserId());
    }

    // Метод для рассылки статуса подписчикам (будет вызываться из WebSocketEventListener)
    public void broadcastStatusToSubscribers(Long targetUserId, UserStatus.Status status, OffsetDateTime timestamp) {
        Set<Long> subscribers = subscriptionService.getSubscribers(targetUserId);
        UserStatusPayload payload = new UserStatusPayload(targetUserId, status.toString(), timestamp);
        WSEvent<UserStatusPayload> event = new WSEvent<>(WSEvent.EventType.USER_STATUS, payload);

        for (Long subscriberId : subscribers) {
            messagingTemplate.convertAndSendToUser(
                    subscriberId.toString(),
                    "/queue/status",
                    event
            );
        }
        log.debug("✅ Рассылка подписчикам {}", event.getPayload().getStatus());
    }

    // DTO (можно вынести в отдельные файлы)
    @Data
    public static class SubscribeRequest {
        private Long targetUserId;
    }

    @Data
    public static class UnsubscribeRequest {
        private Long targetUserId;
    }
}