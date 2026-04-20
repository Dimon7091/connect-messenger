package ru.gorbunov.connect.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Для отправки сообщений клиентам
        registry.enableSimpleBroker("/topic", "/queue");

        // Префикс для сообщений от клиента
        registry.setApplicationDestinationPrefixes("/app");

        // Префикс для личных сообщений
        registry.setUserDestinationPrefix("/user");
    }
}