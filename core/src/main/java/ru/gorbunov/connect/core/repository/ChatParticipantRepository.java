package ru.gorbunov.connect.core.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;
import ru.gorbunov.connect.core.models.ChatParticipantId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
    List<ChatParticipant> findByIdChatId(Long chatId);
    List<ChatParticipant> findByIdChatIdAndIsDeletedFalse(Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp SET cp.unreadCount = cp.unreadCount + 1 WHERE cp.id = :id")
    void incrementUnreadCount(@Param("id") ChatParticipantId id);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp SET cp.unreadCount = CASE " +
            "WHEN cp.unreadCount > 0 THEN cp.unreadCount - 1 ELSE 0 END WHERE cp.id = :id")
    void decrementUnreadCount(@Param("id") ChatParticipantId id);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp SET cp.unreadCount = 0 WHERE cp.id = :id")
    void cleanUnreadCount(@Param("id") ChatParticipantId id);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp SET cp.unreadCount = CASE " +
            "WHEN cp.unreadCount > :count THEN cp.unreadCount - :count " +
            "ELSE 0 END " +
            "WHERE cp.id = :id")
    void decreaseUnreadCount(@Param("id") ChatParticipantId id, @Param("count") int count);


    @Query("SELECT cp.unreadCount FROM ChatParticipant cp WHERE cp.id = :id")
    int getUnreadCount(@Param("id") ChatParticipantId id);

    @Modifying
    @Transactional
    @Query("UPDATE ChatParticipant cp SET cp.isDeleted = :status WHERE cp.id = :id")
    void updateIsDeleted(@Param("id") ChatParticipantId id, @Param("status") boolean status);
}
