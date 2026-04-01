package ru.gorbunov.connect.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.dto.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.models.User;

@Mapper(
        uses = {JsonNullableMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(source = "password", target = "passwordHash", qualifiedByName = "hashPassword")
    public abstract User toEntity(UserCreateRequest dto);

    public abstract UserResponse toDto(User entity);

    public abstract void putUpdate(UserPutUpdateRequest dto, @MappingTarget User entity);

    public abstract void patchUpdate(UserPatchUpdateRequest dto, @MappingTarget User entity);

    @Named("hashPassword")
    protected String hashPassword(String rawPassword) {
        return rawPassword == null ? null : passwordEncoder.encode(rawPassword);
    }
}
