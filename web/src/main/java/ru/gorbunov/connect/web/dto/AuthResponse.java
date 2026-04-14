package ru.gorbunov.connect.web.dto;

import ru.gorbunov.connect.core.dto.user.UserResponse;

public record AuthResponse(
        UserResponse user,
        String token
) { }
