package ru.gorbunov.connect.web.dto;

import ru.gorbunov.connect.core.dto.user.UserPrivateResponse;

public record AuthResponse(
        UserPrivateResponse user,
        String token
) { }
