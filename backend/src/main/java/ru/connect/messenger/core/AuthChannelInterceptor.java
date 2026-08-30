package ru.connect.messenger.core;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import ru.connect.messenger.core.client.UserBanChecker;
import ru.connect.messenger.core.client.UserDeletedChecker;

import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {
    private final UserBanChecker userBanChecker;
    private final UserDeletedChecker userDeletedChecker;

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
            if (userBanChecker.isUserBanned(userId)) {
                log.warn("❌ Blocked messaging from banned user: {}", userId);

                // Выбрасываем исключение. Spring STOMP автоматически превратит
                // его в фрейм ERROR и отправит клиенту, закрыв соединение.
                throw new DisabledException("Аккаунт заблокирован");
            }

            if (userDeletedChecker.isUserDeleted(userId)) {
                log.warn("❌ Blocked messaging from deleted user: {}", userId);

                // Выбрасываем исключение. Spring STOMP автоматически превратит
                // его в фрейм ERROR и отправит клиенту, закрыв соединение.
                throw new DisabledException("Аккаунт заблокирован");

            }
        }

        return message;
    }
}
