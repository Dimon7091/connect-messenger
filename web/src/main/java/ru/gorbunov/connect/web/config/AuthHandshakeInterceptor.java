package ru.gorbunov.connect.web.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.gorbunov.connect.core.service.BanService;
import ru.gorbunov.connect.core.service.orchestrators.UserDeletionService;

import java.util.Map;

@Slf4j
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private BanService banService;

    @Autowired
    private UserDeletionService userDeletionService;

    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes
    ) {
        log.info("🔐 ===== HANDSHAKE START =====");

        if (!(request instanceof ServletServerHttpRequest servletServerRequest)) {
            log.warn("❌ Handshake rejected: Not a servlet request");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        String token = servletServerRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            log.warn("❌ Handshake rejected: Missing token parameter");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            var jwt = decoder.decode(token);
            Object sub = jwt.getClaim("sub");

            if (sub == null) {
                log.warn("❌ JWT claim 'sub' is missing");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = (sub instanceof Number n) ? n.longValue() : Long.parseLong(sub.toString());
            log.info("JWT validated successfully for userId: {}", userId);

            // Проверка бизнес-статусов (Рекомендуется использовать кэш внутри сервисов)
            if (banService.isUserBanned(userId)) {
                log.warn("❌ Connection rejected: User {} is banned", userId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            if (userDeletionService.isUserDeleted(userId)) {
                log.warn("❌ Connection rejected: User {} is deleted", userId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // Авторизация успешна
            attributes.put("userId", userId);
            return true;

        } catch (Exception e) {
            log.error("❌ Auth failed during handshake: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception == null) {
            log.info("✅ WebSocket handshake completed successfully");
        } else {
            log.error("❌ WebSocket handshake failed post-processing", exception);
        }
    }
}
