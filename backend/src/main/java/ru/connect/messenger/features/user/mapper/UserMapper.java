package ru.connect.messenger.features.user.mapper;

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
import ru.connect.messenger.features.user.domain.Profile;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.ProfileResponse;
import ru.connect.messenger.features.user.dto.UpdateUserNameRequest;
import ru.connect.messenger.features.user.dto.UserAdminResponse;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.dto.UserProfileUpdateRequest;
import ru.connect.messenger.features.user.dto.UserPublicResponse;
import ru.connect.messenger.features.userstatus.UserStatus;
import ru.connect.messenger.shared.mapper.JsonNullableMapper;

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

    @Mapping(source = "userName", target = "userName", qualifiedByName = "lowercase")
    @Mapping(source = "password", target = "passwordHash", qualifiedByName = "hashPassword")
    @Mapping(source = "firstName", target = "profile.firstName", qualifiedByName = "capitalize")
    @Mapping(source = "lastName", target = "profile.lastName", qualifiedByName = "capitalize")
    @Mapping(target = "profile.avatarKey", expression = "java(null)")
    public abstract User toEntity(UserCreateRequest dto);

    @Mapping(source = "username", target = "userName")
    @Mapping(target = "roles", ignore = true)
    @Mapping(source = "profile", target = "profile", qualifiedByName = "addProfileResponse")
    public abstract UserPrivateResponse toPrivateDto(User entity);

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "profile", target = "profile", qualifiedByName = "addProfileResponse")
    public abstract UserPublicResponse toPublicDto(User entity);


    @Mapping(source = "username", target = "userName")
    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesEnumToString")
    @Mapping(source = "profile", target = "profile", qualifiedByName = "addProfileResponse")
    @Mapping(source = "userStatus", target = "status", qualifiedByName = "setStatus")
    @Mapping(source = "userStatus", target = "lastSeen", qualifiedByName = "setLastSeen")
    public abstract UserAdminResponse toUserAdminDto(User entity);

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

    @Named("capitalize")
    protected String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    @Named("lowercase")
    protected String lowercase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.toLowerCase();
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

    @Named("setStatus")
    protected String setStatus(UserStatus userStatus) {
        if (userStatus == null) {
            return UserStatus.Status.OFFLINE.toString();
        }
        return userStatus.getStatus().toString();
    }

    @Named("setLastSeen")
    protected String setLastSeen(UserStatus userStatus) {
        if (userStatus == null) {
            return "";
        }
        return userStatus.getLastSeen().toString();
    }
}
