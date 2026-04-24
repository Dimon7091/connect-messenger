package ru.gorbunov.connect.web.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.gorbunov.connect.core.models.UserPrincipal;
import ru.gorbunov.connect.web.util.JwtUtil;

import java.util.Map;

@Slf4j
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtDecoder decoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.info("🔐 ===== HANDSHAKE START =====");

        if (request instanceof ServletServerHttpRequest servletServerRequest) {
            String token = servletServerRequest.getServletRequest().getParameter("token");
            log.info("JWT token: {}", token);
            if (token != null) {
                try {
                    var jwt = decoder.decode(token);
                    // Извлекаем sub и приводим к Long
                    Object sub = jwt.getClaim("sub");
                    Long userId = (sub instanceof Number n) ? n.longValue() : Long.parseLong(sub.toString());
                    log.info("JWT token: {}, userId: {}", token, userId);
                    if (userId != null) {
                        // Кладем в атрибуты, чтобы HandshakeHandler увидел этот ID
                        attributes.put("userId", userId);
                        return true;
                    }
                } catch (Exception e) {
                    log.error("❌ JWT validation failed: {}", e.getMessage());
                }
            }
        }
        return false; // Отклоняем соединение, если токена нет или он невалиден
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            log.info("✅ WebSocket handshake completed successfully");
        }
    }
}

