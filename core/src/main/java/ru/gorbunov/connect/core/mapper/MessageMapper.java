package ru.gorbunov.connect.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.dto.ws.SendMessageRequest;
import ru.gorbunov.connect.core.models.Attachment;
import ru.gorbunov.connect.core.models.Message;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(
        uses = {JsonNullableMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class MessageMapper {

    @Mapping(source = "attachments", target = "attachments", qualifiedByName = "toAttachmentEntity")
    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "stringToTimestamp")
    @Mapping(target = "replyToId", source = "replyToId", qualifiedByName = "stringToLong")
    public abstract Message toEntity(SendMessageRequest dto);

    @Mapping(source = "attachments", target = "attachments", qualifiedByName = "toAttachmentDto")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "chat", ignore = true)
    @Mapping(target = "replyToId", source = "replyToId", qualifiedByName = "longToString")
    public abstract MessageNewResponse toDto(Message entity);

    @Named("stringToLong")
    protected Long stringToLong(String stringId) {
        if (stringId == null || stringId.trim().isEmpty()) {
            return null;
        }

        return Long.valueOf(stringId);
    }

    @Named("longToString")
    protected String longToString(Long longId) {
        if (longId == null) {
            return null;
        }

        return String.valueOf(longId);
    }

    @Named("toAttachmentDto")
    protected List<Attachment> toAttachmentDto(List<Attachment> rawAttachment) {
        if (rawAttachment == null) {
            return null;
        }

        return rawAttachment;
    }

    @Named("toAttachmentEntity")
    protected List<Attachment> toAttachmentEntity(List<Attachment> rawAttachment) {
        if (rawAttachment == null) {
            return null;
        }

        return rawAttachment;
    }

    @Named("stringToTimestamp")
    protected OffsetDateTime stringToTimestamp(String stringTimestamp) {
        return OffsetDateTime.parse(stringTimestamp);
    }
}
