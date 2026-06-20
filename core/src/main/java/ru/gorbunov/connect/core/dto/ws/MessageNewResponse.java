package ru.gorbunov.connect.core.dto.ws;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import ru.gorbunov.connect.core.dto.chat.ChatResponse;
import ru.gorbunov.connect.core.models.Attachment;
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
    private ChatResponse chat;
    private String senderId;
    private Long receiverId;
    private String text;
    private OffsetDateTime timestamp;
    private OffsetDateTime createdAt;
    private MessageStatus status;
    private List<Attachment> attachments;
    private List<String> readBy;
    private String replyToId;
    private ReplyContext replyContext;
}
