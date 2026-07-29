package ru.gorbunov.connect.core.service.orchestrators;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.dto.user.UserAdminResponse;
import ru.gorbunov.connect.core.exception.IllegalActionException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.core.service.UserStatusSubscriptionService;

@Service
@AllArgsConstructor
@Transactional
public class UserDeletionService {
    private final UserService userService;
    private final UserStatusSubscriptionService userStatusSubscriptionService;
    private final StatusService statusService;
    private final FileStorageProvider storageProvider;
    private Cache<Long, Boolean> usersDeletedCache;
    private UserMapper mapper;

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
        statusService.deleteStatusFromDatabase(userId);

        usersDeletedCache.put(userId, true);
        return mapper.toUserAdminDto(userAsDeleted);
    }

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
