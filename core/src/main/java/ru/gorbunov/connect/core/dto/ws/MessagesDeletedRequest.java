package ru.gorbunov.connect.core.dto.ws;

import java.util.List;

public record MessagesDeletedRequest(
        Long chatId,
        List<Long> messagesIds
) { }
