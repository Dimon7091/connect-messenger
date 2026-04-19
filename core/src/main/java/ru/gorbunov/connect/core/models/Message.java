package ru.gorbunov.connect.core.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_chat_timestamp", columnList = "chat_id, timestamp")
})
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String text;

    // Тот самый источник правды для UI (UTC)
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private MessageStatus status; // SENT, DELIVERED, READ, FAILED

    @Column(name = "reply_to_id")
    private UUID replyToId;

    // Используем JSONB для гибкости вложений и списков
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Attachment> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deleted_by", columnDefinition = "jsonb")
    private List<Long> deletedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by", columnDefinition = "jsonb")
    private List<Long> readBy;

    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ,
        FAILED
    }

    // Вложенный класс для вложений
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String id;        // ID файла в хранилище (S3/Minio)
        private String url;       // Ссылка на файл
        private String name;      // Оригинальное имя файла
        private String type;      // image, video, file, audio
        private Long size;        // Размер в байтах
        private String previewUrl; // Для превью картинок
    }
}

