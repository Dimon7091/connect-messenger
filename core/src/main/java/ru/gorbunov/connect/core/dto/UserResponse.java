package ru.gorbunov.connect.core.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String userName,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        String createdAt,
        String lastSeen
) {}
