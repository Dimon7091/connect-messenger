package ru.connect.messenger.features.user.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.connect.messenger.features.user.domain.BlockId;
import ru.connect.messenger.features.user.domain.UserBlock;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, BlockId> {
    // Проверка: заблокировал ли пользователь А пользователя Б
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT blockerId FROM UserBlock ub WHERE "
            + "(ub.blockerId = :userA AND ub.blockedId = :userB) OR "
            + "(ub.blockerId = :userB AND ub.blockedId = :userA)")
    long findBlocker(@Param("userA") Long userA, @Param("userB") Long userB);

    // Проверка в обе стороны (блокировал ли А -> Б ИЛИ Б -> А)
    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub WHERE "
            + "(ub.blockerId = :userA AND ub.blockedId = :userB) OR "
            + "(ub.blockerId = :userB AND ub.blockedId = :userA)")
    boolean isEitherBlocked(@Param("userA") Long userA, @Param("userB") Long userB);

    // Получить все ID, которые заблокировал данный пользователь
    @Query("SELECT ub.blockedId FROM UserBlock ub WHERE ub.blockerId = :blockerId")
    List<Long> findBlockedIdsByBlockerId(Long blockerId);

    @Override
    void deleteById(@NotNull BlockId blockId);
}
