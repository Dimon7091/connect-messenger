package ru.connect.messenger.features.messaging.chat.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.chat.domain.ChatParticipant;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.shared.mapper.JsonNullableMapper;

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
    protected List<String> toParticipantsId(List<ChatParticipant> rawParticipants) {
        return rawParticipants.stream()
                .map(p -> p.getId().getUserId().toString())
                .toList();
    }
}
