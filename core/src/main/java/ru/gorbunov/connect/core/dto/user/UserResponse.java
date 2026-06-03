package ru.gorbunov.connect.core.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.gorbunov.connect.core.models.Profile;

import java.util.Set;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String userName;
    private String email;
    private Set<String> roles;
    private String createdAt;
    private String lastSeen;
    private ProfileResponse profile;
}
