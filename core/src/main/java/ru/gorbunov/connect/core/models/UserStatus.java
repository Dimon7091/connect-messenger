package ru.gorbunov.connect.core.models;

import jakarta.persistence.Entity;
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
    private Long userId;
    private String status;
    private OffsetDateTime lastSeen;
}