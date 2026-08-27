package ru.connect.messenger.features.messaging.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.connect.messenger.features.messaging.message.domain.MessageStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentResponse {
    private String messageId;        // временный ID от клиента
    private String serverMessageId;  // UUID из БД
    private MessageStatus status;           // "SENT" | "FAILED"
    private String timestamp;
}
