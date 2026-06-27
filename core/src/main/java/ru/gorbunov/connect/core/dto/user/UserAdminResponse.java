package ru.gorbunov.connect.core.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class UserAdminResponse {
    private Long id;
    @JsonProperty("userName")
    private String userName;
    private String email;
    private Set<String> roles;
    private String createdAt;
    private String lastSeen;
    private String status;
    private ProfileResponse profile;
}
