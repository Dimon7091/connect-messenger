package ru.gorbunov.connect.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "chat_participants")
public class ChatParticipant {

    @EmbeddedId
    private ChatParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")  // указывает, что поле chatId в составе ключа
    @JoinColumn(name = "chat_id", insertable = false, updatable = false)
    private Chat chat;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "unreadCount")
    private int unreadCount;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;

    @Column(name = "is_chat_empty")
    private Boolean isChatEmpty;

    @Column(name = "is_muted")
    private Boolean isMuted = false;

    @Column(name = "pinned_at")
    private OffsetDateTime pinnedAt;

    @Column(name = "joined_at")
    @CreatedDate
    private OffsetDateTime joinedAt;
}
