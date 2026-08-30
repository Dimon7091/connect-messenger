package ru.connect.messenger.features.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.user.domain.BlockId;
import ru.connect.messenger.features.user.domain.UserBlock;
import ru.connect.messenger.features.user.dto.UserBlockResponse;
import ru.connect.messenger.features.user.repository.UserBlockRepository;
import ru.connect.messenger.features.user.api.UserBlockChecker;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class UserBlockService implements UserBlockChecker {
    private final UserBlockRepository userBlockRepository;
    private final Cache<BlockId, Boolean>  userBlockCache;

    public UserBlockResponse blockUserByUser(Long blockerId, Long blockedId) {
        UserBlock newBlock = new UserBlock(blockerId, blockedId);
        userBlockRepository.save(newBlock);
        userBlockCache.invalidate(new BlockId(blockerId, blockedId));
        return new UserBlockResponse(blockedId.toString());
    }

    public void removeBlockUserByUser(Long blockerId, Long blockedId) {
        BlockId blockId = new BlockId(blockerId, blockedId);
        userBlockRepository.deleteById(blockId);
        userBlockCache.invalidate(blockId);
        log.info("********** Удаление блокировки: {}, {}", blockId.getBlockerId(), blockId.getBlockedId());
    }

    @Override
    public boolean isEitherBlocked(Long userA, Long userB) {
        BlockId directKey = new BlockId(userA, userB);
        BlockId reverseKey = new BlockId(userB, userA);

        Boolean directCache = userBlockCache.getIfPresent(directKey);
        Boolean reverseCache = userBlockCache.getIfPresent(reverseKey);

        if ((directCache != null && directCache) || (reverseCache != null && reverseCache)) {
            return true;
        }

        if (directCache != null && reverseCache != null) {
            return false;
        }

        boolean isBlocked = userBlockRepository.isEitherBlocked(userA, userB);
        if (isBlocked) {
            if (userBlockRepository.existsByBlockerIdAndBlockedId(userA, userB)) {
                userBlockCache.put(directKey, true);
            }
            if (userBlockRepository.existsByBlockerIdAndBlockedId(userB, userA)) {
                userBlockCache.put(reverseKey, true);
            }
        } else {
            userBlockCache.put(directKey, false);
            userBlockCache.put(reverseKey, false);
        }
        return isBlocked;
    }


    public long whoIsBlocker(Long userA, Long userB) {
        return userBlockRepository.findBlocker(userA, userB);
    }

    public List<Long> findBlockedUserIds(Long blockerId) {
        return userBlockRepository.findBlockedIdsByBlockerId(blockerId);
    }
}
