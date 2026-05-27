package ru.gorbunov.connect.core.dto.user;

import ru.gorbunov.connect.core.models.Profile;

import java.util.Set;

public record UserResponse(
        Long id,
        String userName,
        String email,
        Set<String> roles,
        String createdAt,
        String lastSeen,
        ProfileResponse profile
) {}
