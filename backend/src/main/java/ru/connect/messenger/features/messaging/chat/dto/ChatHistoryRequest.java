package ru.connect.messenger.features.messaging.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryRequest {
    private String chatId;
    private Integer limit;           // сколько сообщений загрузить
    private String beforeTimestamp;  // пагинация: загрузить до этой даты
    private String afterTimestamp;   // опционально: загрузить после даты
}
