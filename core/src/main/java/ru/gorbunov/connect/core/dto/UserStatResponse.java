package ru.gorbunov.connect.core.dto;

public record UserStatResponse(
        Long totalUsers,
        Long onlineUsers
) { }
