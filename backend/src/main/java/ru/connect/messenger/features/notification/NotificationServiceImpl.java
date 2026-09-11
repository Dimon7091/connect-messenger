package ru.connect.messenger.features.notification;

import io.hypersistence.utils.spring.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.connect.messenger.shared.dto.NotificationTask;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final RestClient restClient;
    private final String appId;
    private final boolean isEnabled;

    public NotificationServiceImpl(
            @Value("${onesignal.api-key:}") String apiKey,
            @Value("${onesignal.base-url:}") String baseUrl,
            @Value("${onesignal.app-id:}") String appId,
            @Value("${onesignal.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${onesignal.read-timeout-ms:15000}") long readTimeoutMs) {

        this.isEnabled = StringUtils.hasText(apiKey)
                && StringUtils.hasText(baseUrl) && StringUtils.hasText(appId);
        this.appId = appId;

        if (!isEnabled) {
            log.warn("OneSignal configuration is incomplete. Push notification service is DISABLED.");
            this.restClient = null;
            return;
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                .defaultHeader("Authorization", apiKey)
                .build();

        log.debug("NotificationService initialized: baseUrl={}, connectTimeout={}ms, readTimeout={}ms, httpVersion=HTTP_2",
                baseUrl, connectTimeoutMs, readTimeoutMs);
    }

    @Async
    @Override
    public void sendPushToUsers(NotificationTask task) {
        List<String> externalId = task.external_id();
        log.debug("Start to send push notification, external_id={}", externalId);

        OneSignalRequest payload = new OneSignalRequest(
                appId,
                Map.of("external_id", task.external_id()),
                "push",
                Map.of("en", task.title(), "ru", task.title()),
                Map.of("en", task.message(), "ru", task.message()),
                task.chromeWebImage(),
                task.url(),
                List.of(
                        new OneSignalRequest.WebButton("reply", "Ответить", task.url()),
                        new OneSignalRequest.WebButton("read", "Отметить как прочитанное", "do_not_open")
                ),
                Map.of("chat_id", "123")
        );

        long start = System.nanoTime();
        try {
            String response = restClient.post()
                    .uri("/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.debug("OneSignal response ({} ms), external_id={}, body={}",
                    tookMs, externalId, response);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.error("OneSignal HTTP {} ({} ms), external_id={}, body={}",
                    e.getStatusCode(), tookMs, externalId, e.getResponseBodyAsString(), e);

        } catch (Throwable e) {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.error("Notification error ({} ms), external_id={}",
                    tookMs, externalId, e);
        }
    }
}