package ru.connect.messenger.core;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.Map;

public class MyHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected java.security.Principal determineUser(ServerHttpRequest request,
                                                    WebSocketHandler wsHandler,
                                                    Map<String, Object> attributes) {
        // Берем userId, который мы положили в интерцепторе
        Long userId = (Long) attributes.get("userId");
        if (userId != null) {
            // Создаем Principal, который будет виден в EventListener через accessor.getUser()
            return () -> String.valueOf(userId);
        }
        return null;
    }
}
