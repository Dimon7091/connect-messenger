package ru.connect.messenger.features.messaging.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.features.messaging.message.domain.Attachment;
import ru.connect.messenger.features.messaging.message.domain.MessageStatus;
import ru.connect.messenger.features.messaging.message.domain.ReplyContext;

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
