package ru.connect.messenger.features.notification;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    void sendPushToUsers(
            List<String> userIds,
            String title, String message,
            Map<String, String> additionalData,
            String targetChannel
    );
}
