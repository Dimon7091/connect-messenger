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
import ru.gorbunov.connect.core.dto.user.ProfileResponse;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.models.Profile;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;

import java.util.Set;
import java.util.stream.Collectors;

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
    @Mapping(source = "firstName", target = "profile.firstName")
    @Mapping(source = "lastName", target = "profile.lastName")
    public abstract User toEntity(UserCreateRequest dto);

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesEnumToString")
    @Mapping(source = "profile", target = "profile", qualifiedByName = "addProfileResponse")
    public abstract UserResponse toDto(User entity);

    public abstract void putUpdate(UserPutUpdateRequest dto, @MappingTarget User entity);

    public abstract void patchUpdate(UserPatchUpdateRequest dto, @MappingTarget User entity);

    @Named("hashPassword")
    protected String hashPassword(String rawPassword) {
        return rawPassword == null ? null : passwordEncoder.encode(rawPassword);
    }

    @Named("rolesEnumToString")
    protected Set<String> rolesEnumToString(Set<Role> rawRoles) {
        return rawRoles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Named("addProfileResponse")
    protected ProfileResponse addProfileResponse(Profile profile) {
        return new ProfileResponse(
                profile.getFirstName(),
                profile.getLastName(),
                profile.getAvatarKey()
        );
    }
}
