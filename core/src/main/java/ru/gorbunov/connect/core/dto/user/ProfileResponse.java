package ru.gorbunov.connect.core.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProfileResponse {
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String avatarThumbUrl;
}
