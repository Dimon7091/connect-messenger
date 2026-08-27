package ru.connect.messenger.features.messaging.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.connect.messenger.features.messaging.message.dto.MessageNewResponse;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private String chatId;
    private List<MessageNewResponse> messages;
    private int unreadCount;
    private boolean hasMore;         // есть ли еще сообщения для загрузки
    private int totalCount;          // общее количество сообщений в чате
    private String nextCursor;       // timestamp для следующей загрузки (пагинация)
}
