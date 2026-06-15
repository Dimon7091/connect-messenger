package ru.gorbunov.connect.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.gorbunov.connect.core.dto.chat.ChatResponse;
import ru.gorbunov.connect.core.models.Chat;
import ru.gorbunov.connect.core.models.ChatParticipant;

import java.util.List;

@Mapper(
        uses = {JsonNullableMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class ChatMapper {
    @Mapping(source = "participants", target = "participants", qualifiedByName = "toParticipantsId")
    @Mapping(target = "unreadCount", ignore = true)
    public abstract ChatResponse toDto(Chat entity);

    @Named("toParticipantsId")
    protected List<Long> toParticipantsId(List<ChatParticipant> rawParticipants) {
        return rawParticipants.stream()
                .map(p -> p.getId().getUserId())
                .toList();
    }
}
