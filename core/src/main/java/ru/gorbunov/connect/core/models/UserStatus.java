package ru.gorbunov.connect.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_statuses")
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Status status;
    private OffsetDateTime lastSeen;

    public enum Status {
        ONLINE,
        OFFLINE
    }
}
