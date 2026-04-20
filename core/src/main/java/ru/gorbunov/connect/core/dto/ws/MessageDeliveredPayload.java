package ru.gorbunov.connect.core.dto.ws;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeliveredPayload {
    private String messageId;
    private String chatId;
    private String userId;
}
