package ru.gorbunov.connect.core.models;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_chat_timestamp", columnList = "chat_id, timestamp, read_by")
})
@EntityListeners(AuditingEntityListener.class)
public class Message {
    @Id
    @Tsid
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(columnDefinition = "TEXT")
    private String text;

    // Тот самый источник правды для UI (UTC)
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private MessageStatus status; // SENT, DELIVERED, READ, FAILED

    @Column(name = "reply_to_id")
    private Long replyToId;

    // Используем JSONB для гибкости вложений и списков
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Attachment> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deleted_by", columnDefinition = "jsonb")
    private List<Long> deletedBy = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by", columnDefinition = "jsonb")
    private List<Long> readBy;

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

