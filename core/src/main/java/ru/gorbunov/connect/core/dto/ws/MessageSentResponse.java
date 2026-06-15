package ru.gorbunov.connect.core.dto.ws;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import ru.gorbunov.connect.core.models.MessageStatus;

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
