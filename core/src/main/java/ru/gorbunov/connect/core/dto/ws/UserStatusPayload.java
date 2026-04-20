package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusPayload {
    private String userId;
    private String status;  // "online" | "offline" | "away"
}
