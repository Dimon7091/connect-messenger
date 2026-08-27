package ru.connect.messenger.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private AuthHandshakeInterceptor authHandshakeInterceptor;

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("🔧 Registering STOMP endpoint with interceptor: {}",
                authHandshakeInterceptor != null ? "present" : "NULL");
        registry.addEndpoint("/chat-ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor)
                .setHandshakeHandler(new MyHandshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1); // Экономим память: 1 поток для хартбитов идеален для 1 ядра
        te.setThreadNamePrefix("ws-heartbeat-");
        te.initialize();

        // Для отправки сообщений клиентам
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{20000, 20000}) // 20 секунд
                .setTaskScheduler(te);

        // Префикс для сообщений от клиента
        registry.setApplicationDestinationPrefixes("/app");

        // Префикс для личных сообщений
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Подключаем интерцептор для проверки КАЖДОГО входящего сообщения
        registration.interceptors(authChannelInterceptor);
    }
}
