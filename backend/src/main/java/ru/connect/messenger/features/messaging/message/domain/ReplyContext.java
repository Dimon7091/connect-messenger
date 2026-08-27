package ru.connect.messenger.features.messaging.message.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyContext {
    private Long originalMessageId;
    private String senderName;
    private String textSnippet;
}
