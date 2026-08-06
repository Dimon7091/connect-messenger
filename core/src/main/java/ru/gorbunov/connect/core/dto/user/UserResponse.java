package ru.gorbunov.connect.core.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class UserResponse {
    private String id;
    private String userName;
    private Set<String> roles;
    private String createdAt;
    private ProfileResponse profile;
    private Boolean isDeleted;
    private Boolean isBanned;
    private List<Long> blackListIds;
}
