package ru.connect.messenger.features.messaging.message.dto;

import java.time.OffsetDateTime;

public record MessagesDeletedPayload(
        Long receiverId,
        OffsetDateTime chatUpdatedAt,
        Integer unreadCount,
        String lastMessage
) { }
