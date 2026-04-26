package ru.gorbunov.connect.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.models.Message;

import java.util.List;


@Mapper(
        uses = {JsonNullableMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class MessageMapper {
    @Mapping(source = "attachments", target = "attachments", qualifiedByName = "toAttachmentDto")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "chat", ignore = true)
    public abstract MessageNewResponse toDto(Message entity);

    @Named("toAttachmentDto")
    protected List<MessageNewResponse.AttachmentDto> toAttachmentDto(List<Message.Attachment> rawAttachment) {
        if (rawAttachment == null) {
            return null;
        }

        return rawAttachment.stream()
                .map(a -> (
                        MessageNewResponse.AttachmentDto.builder()
                                .id(a.getId())
                                .url(a.getUrl())
                                .name(a.getName())
                                .type(a.getType())
                                .size(a.getSize())
                                .previewUrl(a.getPreviewUrl())
                                .build()
                        ))
                .toList();
    }
}
