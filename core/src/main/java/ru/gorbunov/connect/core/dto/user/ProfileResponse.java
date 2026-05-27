package ru.gorbunov.connect.core.dto.user;

public record ProfileResponse(
        String firstName,
        String lastName,
        String avatarUrl
) { }
