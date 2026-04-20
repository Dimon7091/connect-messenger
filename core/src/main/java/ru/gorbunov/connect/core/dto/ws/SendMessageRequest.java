package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String chatId;
    private String messageId;  // временный ID от клиента
    private String text;
    private String replyToId;
    private String timestamp;
}
