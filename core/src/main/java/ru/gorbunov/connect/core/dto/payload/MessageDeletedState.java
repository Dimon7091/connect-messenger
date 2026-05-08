package ru.gorbunov.connect.core.dto.payload;

import java.util.List;

public record MessageDeletedState(
        Long id,
        List<Long> deletedBy,
        Long senderId,
        Long receiverId
) {}

