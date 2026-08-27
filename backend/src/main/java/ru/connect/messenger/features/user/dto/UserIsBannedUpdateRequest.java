package ru.connect.messenger.features.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserIsBannedUpdateRequest(
        @NotNull(message = "Флаг бана не может быть null")
        Boolean isBanned
) { }
