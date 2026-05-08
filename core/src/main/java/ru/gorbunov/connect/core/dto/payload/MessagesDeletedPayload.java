package ru.gorbunov.connect.core.dto.payload;

import java.time.OffsetDateTime;
import java.util.List;

public record MessagesDeletedPayload(
        Long receiverId,
        OffsetDateTime chatUpdatedAt,
        Integer unreadCount,
        String lastMessage
) { }
