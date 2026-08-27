package ru.connect.messenger.features.messaging.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.domain.MessageStatus;
import ru.connect.messenger.features.messaging.message.dto.MessageDeletedState;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") long id, @Param("status") MessageStatus status);

    @Modifying
    @Query(value = "UPDATE messages "
            + "SET read_by = read_by || CAST(:readerId AS jsonb) "
            + "WHERE id = :id AND NOT (read_by @> CAST(:readerId AS jsonb))",
            nativeQuery = true)
    void addReaderByUser(@Param("id") long id, @Param("readerId") long readerId);

    @Query("SELECT m FROM Message m "
            + "WHERE m.chatId = :chatId "
            + "AND m.createdAt < :beforeTimestamp "
            + "AND (m.senderId = :currentUserId OR m.receiverId = :currentUserId) "
            + "ORDER BY m.createdAt DESC")
    List<Message> findChatMessages(@Param("chatId") long chatId,
                                   @Param("limit") int limit,
                                   @Param("currentUserId") long currentUserId,
                                   @Param("beforeTimestamp") OffsetDateTime beforeTimestamp);

    @Query(value = "SELECT * FROM messages "
            + "WHERE chat_id = :chatId "
            + "ORDER BY created_at DESC "
            + "LIMIT 1",
            nativeQuery = true)
    Optional<Message> findLastChatMessage(@Param("chatId") long chatId);

    @Query(value = "SELECT COUNT(*) FROM messages "
            + "WHERE chat_id = :chatId "
            + "AND sender_id != :userId "
            + "AND NOT (read_by @> CAST(CAST(:userId AS text) AS jsonb))",
            nativeQuery = true)
    Integer countUnreadMessagesByUser(@Param("chatId") long chatId, @Param("userId") long userId);

    @Query("""
        SELECT new ru.connect.messenger.features.messaging.message.dto.MessageDeletedState
            (m.id, m.deletedBy, m.senderId, m.receiverId)
        FROM Message m
        WHERE m.chatId = :chatId
    """)
    List<MessageDeletedState> findDeletedStatesByChatId(@Param("chatId") Long chatId);

    @Modifying
    @Query("UPDATE Message m SET m.deletedBy = :deletedBy WHERE m.id = :id")
    void updateDeletedBy(@Param("id") Long id, @Param("deletedBy") List<Long> deletedBy);

    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.chatId = :chatId AND m.receiverId = :receiverId")
    void markAllAsReadByReceiver(@Param("chatId") Long chatId,
                       @Param("status") MessageStatus status,
                       @Param("receiverId") Long receiverId);

}
