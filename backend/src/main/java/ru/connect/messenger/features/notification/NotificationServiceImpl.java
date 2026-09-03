package ru.connect.messenger.features.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final RestClient restClient;
    private final String appId;

    // Внедряем все параметры через конструктор, чтобы baseUrl не был null
    public NotificationServiceImpl(
            @Value("${onesignal.api-key}") String apiKey,
            @Value("${onesignal.base-url}") String baseUrl,
            @Value("${onesignal.app-id}") String appId) {

        this.appId = appId;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl) // Теперь здесь гарантированно будет строка из конфига
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                .defaultHeader("Authorization", apiKey)
                .build();
    }

    public void sendPushToUsers(
            List<String> userIds,
            String title, String message,
            Map<String, String> additionalData,
            String targetChannel
    ) {
        OneSignalRequest payload = new OneSignalRequest(
                appId,
                Map.of("external_id", userIds),
                targetChannel,
                Map.of("ru", title, "en", title),
                Map.of("ru", message, "en", message),
                additionalData
        );

        try {
            String response = restClient.post()
                    .uri("/notifications?c=push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.debug("OneSignal response: {}, users: {}", response, userIds);
        } catch (Exception e) {
            log.debug("Notification error: {}", e.getMessage());
        }
    }
}
