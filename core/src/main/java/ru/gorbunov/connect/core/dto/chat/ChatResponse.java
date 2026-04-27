package ru.gorbunov.connect.core.dto.chat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record ChatResponse(
        Long id,
        String type,
        List<Long> participants,
        Integer unreadCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long createdBy
) { }
