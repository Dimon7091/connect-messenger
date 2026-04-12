package ru.gorbunov.connect.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participants")

@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipant {

    @EmbeddedId
    private ChatParticipantId id;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "is_muted")
    private Boolean isMuted = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}
