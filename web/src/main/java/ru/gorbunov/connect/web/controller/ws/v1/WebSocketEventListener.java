package ru.gorbunov.connect.web.controller.ws.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.gorbunov.connect.core.dto.ws.UserStatusPayload;
import ru.gorbunov.connect.core.dto.ws.WSEvent;
import ru.gorbunov.connect.core.service.StatusService;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final StatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        Long userId = getUserId(event.getMessage());
        if (userId != null) {
            log.info("🟢 Пользователь {} подключился", userId);
            updateAndBroadcastStatus(userId, "ONLINE");
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Long userId = getUserId(event.getMessage());
        if (userId != null) {
            log.info("🔴 Пользователь {} отключился", userId);
            updateAndBroadcastStatus(userId, "OFFLINE");
        }
    }

    private void updateAndBroadcastStatus(Long userId, String status) {
        // 1. Обновляем в кэше (StatusService сам синхронизирует с БД раз в минуту)
        statusService.updateInCache(userId, status);

        // 2. Рассылаем всем статус
        UserStatusPayload payload = new UserStatusPayload(userId, status, OffsetDateTime.now());
        messagingTemplate.convertAndSend(
                "/topic/public_status",
                new WSEvent<>(WSEvent.EventType.USER_STATUS, payload)
        );
    }

    private Long getUserId(org.springframework.messaging.Message<byte[]> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        // Достаем Principal, который создал MyHandshakeHandler.determineUser
        if (accessor.getUser() != null) {
            return Long.valueOf(accessor.getUser().getName());
        }
        return null;
    }
}
