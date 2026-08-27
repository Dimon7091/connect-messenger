package ru.connect.messenger.features.messaging.message.dto;

import java.util.List;

public record MessageDeletedState(
        Long id,
        List<Long> deletedBy,
        Long senderId,
        Long receiverId
) { }
