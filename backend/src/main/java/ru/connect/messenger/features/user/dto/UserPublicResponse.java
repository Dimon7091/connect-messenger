package ru.connect.messenger.features.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPublicResponse {
    private String id;
    private String userName;
    private String createdAt;
    private ProfileResponse profile;
    private Boolean isDeleted;
    private Boolean isBanned;
}
