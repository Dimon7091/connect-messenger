package ru.connect.messenger.features.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.user.repository.UserRepository;


@AllArgsConstructor
@Service
public class UserBanService {
    private UserRepository userRepository;
    private Cache<Long, Boolean> banCache;

    public void toggleUserBlockStatus(Long userId, Boolean flag) {
        userRepository.updateIsBanned(userId, flag);
        invalidateCache(userId);
    }

    public boolean isUserBanned(Long userId) {
        Boolean cache = banCache.getIfPresent(userId);
        if (cache != null) {
            return cache;
        }

        Boolean banned = userRepository.isBanned(userId);
        if (banned == null) {
            banned = false;
        }
        banCache.put(userId, banned);
        return banned;
    }

    public void invalidateCache(Long userId) {
        banCache.invalidate(userId);
    }
}
