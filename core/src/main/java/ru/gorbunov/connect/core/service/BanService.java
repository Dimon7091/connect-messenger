package ru.gorbunov.connect.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.repository.UserRepository;

@AllArgsConstructor
@Service
public class BanService {
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

        boolean banned = userRepository.isBanned(userId);
        banCache.put(userId, banned);
        return banned;
    }

    public void invalidateCache(Long userId) {
        banCache.invalidate(userId);
    }
}
