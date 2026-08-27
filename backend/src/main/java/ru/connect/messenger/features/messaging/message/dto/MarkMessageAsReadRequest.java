package ru.connect.messenger.features.messaging.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkMessageAsReadRequest {
    private String messageId;
    private String chatId;
}
