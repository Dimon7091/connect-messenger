package ru.connect.messenger.features.messaging.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantId implements Serializable {
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_id")
    private Long userId;
}
