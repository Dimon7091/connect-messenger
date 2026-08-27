package ru.connect.messenger.features.messaging.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByDirectKey(String directKey);
    @Query(value = "SELECT c.* FROM chats c "
            + "INNER JOIN chat_participants cp ON c.id = cp.chat_id "
            + "WHERE cp.user_id = :userId AND cp.is_deleted = false",
            nativeQuery = true)
    List<Chat> findChatsByUserIdNative(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c "
            + "JOIN c.participants p "
            + "WHERE p.id.userId = :id1 OR p.id.userId = :id2 "
            + "GROUP BY c.id "
            + "HAVING COUNT(DISTINCT p.id.userId) = 2")
    Optional<Chat> findChatByParticipants(@Param("id1") Long id1, @Param("id2") Long id2);

    @Query("SELECT c.participants FROM Chat c WHERE c.id = :chatId")
    List<ChatParticipant> findParticipantsByChatId(@Param("chatId") Long chatId);

    @Transactional
    @Modifying
    @Query("UPDATE Chat c SET c.lastMessage = :message, c.updatedAt = :timestamp WHERE c.id = :id")
    void updateLastMessageOnly(@Param("id") Long chatId,
                               @Param("message") String message,
                               @Param("timestamp") OffsetDateTime timestamp);
}
