package ru.connect.messenger.features.messaging.message.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String fileKey;
    private String mimeType;
    private String fileName;
    private String size;
}
