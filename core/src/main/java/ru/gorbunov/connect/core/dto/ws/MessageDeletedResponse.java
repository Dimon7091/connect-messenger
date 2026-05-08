package ru.gorbunov.connect.core.dto.ws;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageDeletedResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long chatId,
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        List<Long> deletedMessagesIds,
        OffsetDateTime chatUpdatedAt,
        Integer unreadCount,
        String lastMessage
) { }
