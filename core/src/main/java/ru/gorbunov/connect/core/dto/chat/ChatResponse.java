package ru.gorbunov.connect.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private Long id;
    private String type;
    private List<Long> participants;
    private Integer unreadCount;
    private String lastMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long createdBy;
}
