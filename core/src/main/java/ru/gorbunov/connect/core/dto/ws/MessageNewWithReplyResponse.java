package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.gorbunov.connect.core.dto.chat.ChatResponse;
import ru.gorbunov.connect.core.models.MessageStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNewWithReplyResponse {
    private String id;
    private String chatId;
    private ChatResponse chat;
    private String senderId;
    private Long receiverId;
    private String text;
    private OffsetDateTime timestamp;
    private OffsetDateTime createdAt;
    private MessageStatus status;
    private String replyToId;
    private List<MessageNewResponse.AttachmentDto> attachments;
    private List<String> readBy;
    private ReplyContext replyContext;
}
