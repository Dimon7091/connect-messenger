package ru.gorbunov.connect.core.dto;

public record UserResponse(
        Long id,
        String userName,
        String email,
        String firstName,
        String lastName,
        String createdAt,
        String lastSeen
) {}
