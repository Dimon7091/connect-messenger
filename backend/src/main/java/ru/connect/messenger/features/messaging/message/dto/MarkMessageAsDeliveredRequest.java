package ru.connect.messenger.features.messaging.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkMessageAsDeliveredRequest {
    private String messageId;
    private String chatId;
}
