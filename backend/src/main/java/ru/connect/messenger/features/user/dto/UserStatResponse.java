package ru.connect.messenger.features.user.dto;

public record UserStatResponse(
        Long totalUsers,
        Long onlineUsers
) { }
