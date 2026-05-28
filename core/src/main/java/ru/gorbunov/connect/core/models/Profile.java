package ru.gorbunov.connect.core.models;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Profile {
    private String firstName;
    private String lastName;
    private String avatarKey;
}
