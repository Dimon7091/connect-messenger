package ru.connect.messenger.features.auth;

import ru.connect.messenger.features.user.dto.UserPrivateResponse;

public record AuthResponse(
        UserPrivateResponse user,
        String token
) { }
