package ru.connect.messenger.features.notification;

import ru.connect.messenger.shared.dto.NotificationTask;

public interface NotificationService {
    void sendPushToUsers(NotificationTask task);
}
