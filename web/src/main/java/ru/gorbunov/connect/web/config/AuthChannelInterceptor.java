package ru.gorbunov.connect.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import ru.gorbunov.connect.core.service.BanService;
import ru.gorbunov.connect.core.service.orchestrators.UserDeletionService;

import java.util.Map;

@Slf4j
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {
    private final BanService banService;
    private final UserDeletionService userDeletionService;

    public AuthChannelInterceptor(UserDeletionService userDeletionService, BanService banService) {
        this.userDeletionService = userDeletionService;
        this.banService = banService;
    }

    @Override
    public Message<?> preSend(@org.jetbrains.annotations.NotNull Message<?> message,
                              @org.jetbrains.annotations.NotNull MessageChannel channel
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Проверяем только сообщения отправки (не CONNECT, не SUBSCRIBE)
        if (SimpMessageType.MESSAGE.equals(accessor.getMessageType())) {

            // Достаем userId, который мы сохранили в сессию во время Handshake
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null || !sessionAttributes.containsKey("userId")) {
                log.warn("❌ Rejected: No userId found in session attributes");
                throw new DisabledException("Пользователь не авторизован");
            }

            Long userId = (Long) sessionAttributes.get("userId");

            // Запрос в БД/Кэш «на лету»
            if (banService.isUserBanned(userId)) {
                log.warn("❌ Blocked message from banned user: {}", userId);

                // Выбрасываем исключение. Spring STOMP автоматически превратит
                // его в фрейм ERROR и отправит клиенту, закрыв соединение.
                throw new DisabledException("Аккаунт заблокирован");
            }

            if (userDeletionService.isUserDeleted(userId)) {
                log.warn("❌ Blocked message from deleted user: {}", userId);

                // Выбрасываем исключение. Spring STOMP автоматически превратит
                // его в фрейм ERROR и отправит клиенту, закрыв соединение.
                throw new DisabledException("Аккаунт заблокирован");

            }
        }

        return message;
    }
}
