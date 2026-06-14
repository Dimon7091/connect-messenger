package ru.gorbunov.connect.core.mapper;

import org.mapstruct.CollectionMappingStrategy;
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
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserProfileUpdateRequest;
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
        collectionMappingStrategy = CollectionMappingStrategy.ACCESSOR_ONLY,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(source = "password", target = "passwordHash", qualifiedByName = "hashPassword")
    @Mapping(source = "firstName", target = "profile.firstName")
    @Mapping(source = "lastName", target = "profile.lastName")
    @Mapping(target = "profile.avatarKey", expression = "java(null)")
    public abstract User toEntity(UserCreateRequest dto);

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesEnumToString")
    @Mapping(source = "profile", target = "profile", qualifiedByName = "addProfileResponse")
    @Mapping(target = "authorities", ignore = true)
    public abstract UserResponse toDto(User entity);

    // Update
    public abstract void updateUserName(UpdateUserNameRequest dto, @MappingTarget User entity);
    @Mapping(target = "profile", source = "dto")
    public abstract void updateProfile(UserProfileUpdateRequest dto, @MappingTarget User entity);
    public abstract void updateProfileFields(UserProfileUpdateRequest dto, @MappingTarget Profile profile);

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
                null,
                null
        );
    }
}
