package ru.gorbunov.connect.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String fileKey;
    private String mimeType;
    private String fileName;
    private String size;
}
