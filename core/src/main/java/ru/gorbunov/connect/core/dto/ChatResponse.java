package ru.gorbunov.connect.core.dto;

import ru.gorbunov.connect.core.models.User;

import java.time.LocalDateTime;
import java.util.List;

public record ChatResponse(
        Long id,
        String type,
        List<User> participants,
        String lastMessage,
        Integer unreadCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdBy
) { }
