package ru.gorbunov.connect.core.dto.user;

public record UserStatResponse(
        Long totalUsers,
        Long onlineUsers
) { }
