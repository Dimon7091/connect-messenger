package ru.gorbunov.connect.core.models;

import lombok.Getter;

@Getter
public enum AvatarType {
    THUMBNAIL("avatars-thumbnail/"),
    ORIGINAL("avatars-original/");

    private final String value;

    AvatarType(String value) {
        this.value = value;
    }
}
