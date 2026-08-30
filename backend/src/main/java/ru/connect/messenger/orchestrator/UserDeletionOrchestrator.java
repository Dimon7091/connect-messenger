package ru.connect.messenger.orchestrator;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.connect.messenger.core.client.UserDeletedChecker;
import ru.connect.messenger.core.exception.IllegalActionException;
import ru.connect.messenger.features.storage.FileStorageProvider;
import ru.connect.messenger.features.user.api.UserService;
import ru.connect.messenger.features.userstatus.ws.UserStatusService;
import ru.connect.messenger.shared.domain.StorageType;
import ru.connect.messenger.features.user.dto.UserAdminResponse;
import ru.connect.messenger.features.user.mapper.UserMapper;
import ru.connect.messenger.features.userstatus.UserStatusSubscriptionService;


@Service
@AllArgsConstructor
@Transactional
public class UserDeletionOrchestrator implements UserDeletedChecker {
    private final UserService userService;
    private final UserStatusSubscriptionService userStatusSubscriptionService;
    private final UserStatusService userStatusService;
    private final FileStorageProvider storageProvider;
    private final Cache<Long, Boolean> usersDeletedCache;
    private final UserMapper mapper;

    public UserAdminResponse softDelete(Long userId) {
        var isAdmin = userService.isAdmin(userId);

        if (isAdmin) {
            throw new IllegalActionException("Вы не можете удалить аккаунт администратора.");
        }

        var user = userService.getUserById(userId);
        if (user.getIsDeleted()) {
            throw new IllegalActionException("Аккаунт уже удален.");
        }
        // Анонимизация данных
        user.setUserName("deleted_" + user.getId());
        user.setPasswordHash(null);
        user.getProfile().setFirstName("Аккаунт");
        user.getProfile().setLastName("удален");

        var avatarKey = user.getProfile().getAvatarKey();
        user.getProfile().setAvatarKey(null);
        user.setIsDeleted(true);
        var userAsDeleted = userService.save(user);

        if (avatarKey != null) {
            storageProvider.delete(avatarKey, StorageType.AVATAR);
        }
        userStatusSubscriptionService.cleanupUserFully(userId);
        userStatusService.deleteStatusFromDatabase(userId);

        usersDeletedCache.put(userId, true);
        return mapper.toUserAdminDto(userAsDeleted);
    }

    @Override
    public boolean isUserDeleted(Long userId) {
        Boolean cache = usersDeletedCache.getIfPresent(userId);
        if (cache != null) {
            return cache;
        }

        boolean deleted = userService.isDeleted(userId);
        usersDeletedCache.put(userId, deleted);
        return deleted;
    }
}
