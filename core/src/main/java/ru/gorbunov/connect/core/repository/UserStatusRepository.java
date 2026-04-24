package ru.gorbunov.connect.core.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.gorbunov.connect.core.models.UserStatus;

import java.time.OffsetDateTime;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_statuses (user_id, status, last_seen) " +
            "VALUES (:id, :status, :lastSeen) " +
            "ON CONFLICT (user_id) DO UPDATE SET status = :status, last_seen = :lastSeen",
            nativeQuery = true)
    void upsertStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("lastSeen") OffsetDateTime lastSeen);
}

