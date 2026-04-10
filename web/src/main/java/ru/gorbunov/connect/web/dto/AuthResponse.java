package ru.gorbunov.connect.web.dto;

import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.models.User;

public record AuthResponse(
        UserResponse user,
        String token
) { }
