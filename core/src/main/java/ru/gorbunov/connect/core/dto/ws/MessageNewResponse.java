package ru.gorbunov.connect.core.dto.ws;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import ru.gorbunov.connect.core.models.MessageStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNewResponse {
    private String id;
    private String chatId;
    private String senderId;
    private String text;
    private OffsetDateTime timestamp;
    private OffsetDateTime createdAt;
    private MessageStatus status;
    private String replyToId;
    private List<AttachmentDto> attachments;
    private List<String> readBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private String id;
        private String url;
        private String name;
        private String type;
        private Long size;
        private String previewUrl;
    }
}
