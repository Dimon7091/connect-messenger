package ru.connect.messenger.features.userstatus.ws;
import ru.connect.messenger.features.userstatus.UserStatus;

public interface UserStatusService {
    void updateInCache(Long userId, UserStatus.Status status);
    UserStatus getStatus(Long userId);
    long getAllOnlineUsersCount();
    void deleteStatusFromDatabase(Long userId);
    void syncWithDb();
}