package ru.gorbunov.connect.core.dto.ws;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentResponse {
    private String messageId;        // временный ID от клиента
    private String serverMessageId;  // UUID из БД
    private String status;           // "SENT" | "FAILED"
    private String timestamp;
}