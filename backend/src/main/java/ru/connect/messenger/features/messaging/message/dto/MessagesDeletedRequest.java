package ru.connect.messenger.features.messaging.message.dto;

import java.util.List;

public record MessagesDeletedRequest(
        Long chatId,
        List<Long> messagesIds
) { }
