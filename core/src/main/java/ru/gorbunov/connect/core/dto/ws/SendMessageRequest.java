package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    private String chatId;
    private Boolean isFistMessage;
    private String messageId;  // временный ID от клиента
    private String senderId;
    private String receiverId;
    private String text;
    private String replyToId;
    private String timestamp;
}
