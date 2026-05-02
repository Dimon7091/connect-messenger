package ru.gorbunov.connect.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gorbunov.connect.core.models.Message;
import ru.gorbunov.connect.core.models.MessageStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // В MessageRepository
    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") long id, @Param("status") MessageStatus status);

    @Modifying
    @Query(value = "UPDATE messages " +
            "SET read_by = read_by || CAST(:readerId AS jsonb) " +
            "WHERE id = :id AND NOT (read_by @> CAST(:readerId AS jsonb))",
            nativeQuery = true)
    void addReaderByUser(@Param("id") long id, @Param("readerId") long readerId);

    @Query(value = "SELECT * FROM messages " +
            "WHERE chat_id = :chatId " +
            "AND created_at < :beforeTimestamp " + // Предположим, поле называется created_at
            "ORDER BY created_at DESC " +        // Сначала новые
            "LIMIT :limit",
            nativeQuery = true)
    List<Message> findChatMessages(@Param("chatId") long chatId,
                                  @Param("limit") int limit,
                                  @Param("beforeTimestamp") OffsetDateTime beforeTimestamp);

    @Query(value = "SELECT COUNT(*) FROM messages " +
            "WHERE chat_id = :chatId " +
            "AND sender_id != :userId " +
            "AND NOT (read_by @> CAST(CAST(:userId AS text) AS jsonb))",
            nativeQuery = true)
    int countUnreadMessagesByUser(@Param("chatId") long chatId, @Param("userId") long userId);

    @Modifying
    @Query(value = "UPDATE messages " +
            "SET read_by = read_by || CAST(:readerId AS jsonb) " +
            "WHERE id = :id AND NOT (read_by @> CAST(:readerId AS jsonb))",
            nativeQuery = true)
    void addDeletedByUser(@Param("chatId") long id, @Param("userId") long readerId);
}
