package ru.gorbunov.connect.web.controller.ws.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.gorbunov.connect.core.models.UserStatus;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserStatusSubscriptionService;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSEventListener {

    private final StatusService statusService;
    private final WSUserStatusController userStatusController;
    private final UserStatusSubscriptionService subscriptionService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        Long userId = getUserId(event.getMessage());
        if (userId != null) {
            log.info("🟢 Пользователь {} подключился", userId);
            updateAndBroadcastStatus(userId, UserStatus.Status.ONLINE);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Long userId = getUserId(event.getMessage());
        if (userId != null) {
            log.info("🔴 Пользователь {} отключился", userId);
            updateAndBroadcastStatus(userId, UserStatus.Status.OFFLINE);
            subscriptionService.cleanupSubscribers(userId);
        }
    }

    private void updateAndBroadcastStatus(Long userId, UserStatus.Status status) {
        // 1. Обновляем в кэше (StatusService сам синхронизирует с БД раз в минуту)
        statusService.updateInCache(userId, status);

        // 2. Рассылаем всем статус
        userStatusController.broadcastStatusToSubscribers(userId, status, OffsetDateTime.now());
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
