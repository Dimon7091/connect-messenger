package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gorbunov.connect.core.models.Message;

import java.time.OffsetDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // В MessageRepository
    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query(value = "UPDATE messages " +
            "SET read_by = read_by || CAST(:readerId AS jsonb) " +
            "WHERE id = :id AND NOT (read_by @> CAST(:readerId AS jsonb))",
            nativeQuery = true)
    void addReaderByUser(@Param("id") Long id, @Param("readerId") Long readerId);

    @Query(value = "SELECT * FROM messages " +
            "WHERE chat_id = :chatId " +
            "AND created_at < :beforeTimestamp " + // Предположим, поле называется created_at
            "ORDER BY created_at DESC " +        // Сначала новые
            "LIMIT :limit",
            nativeQuery = true)
    List<Message> findChatMessage(@Param("chatId") Long chatId,
                                  @Param("limit") Integer limit,
                                  @Param("beforeTimestamp") OffsetDateTime beforeTimestamp);

    @Query(value = "SELECT COUNT(*) FROM messages " +
            "WHERE chat_id = :chatId " +
            "AND sender_id != :userId " +
            "AND NOT (read_by @> CAST(CAST(:userId AS text) AS jsonb))",
            nativeQuery = true)
    int countUnreadMessagesByUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

}
