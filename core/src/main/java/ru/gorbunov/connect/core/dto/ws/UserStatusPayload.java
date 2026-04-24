package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusPayload {
    private Long userId;
    private String status;  // "online" | "offline" | "away"
    private OffsetDateTime lastSeen;
}
