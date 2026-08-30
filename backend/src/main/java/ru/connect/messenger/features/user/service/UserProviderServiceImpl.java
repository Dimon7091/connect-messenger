package ru.connect.messenger.features.user.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.user.api.UserProviderService;
import ru.connect.messenger.features.user.domain.AvatarType;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.ProfileResponse;
import ru.connect.messenger.features.user.dto.UpdateUserNameRequest;
import ru.connect.messenger.features.user.dto.UserAdminResponse;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.dto.UserProfileUpdateRequest;
import ru.connect.messenger.features.user.dto.UserPublicResponse;
import ru.connect.messenger.features.user.dto.UserStatResponse;
import ru.connect.messenger.features.user.mapper.UserMapper;
import ru.connect.messenger.features.userstatus.UserStatusServiceImpl;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class UserProviderServiceImpl implements UserProviderService {
    private final UserServiceImpl userService;
    private final UserProfileService userProfileService;
    private final UserBlockService userBlockService;
    private final UserStatusServiceImpl userStatusServiceImpl;
    private final UserMapper mapper;

    public UserPrivateResponse getUserDetailsForAuth(Long userId) {
        var user = userService.getUserById(userId);
        UserPrivateResponse response = mapper.toPrivateDto(user);
        addAvatarUrls(response.getProfile(), user.getProfile().getAvatarKey());
        var blacklistIdsAsString = userBlockService.findBlockedUserIds(userId).stream()
                .map(Object::toString)
                .toList();
        response.setBlackListIds(blacklistIdsAsString);
        return response;
    }

    public UserPublicResponse findUserPublicDetails(Long userId) {
        var user = userService.getUserById(userId);
        UserPublicResponse response = mapper.toPublicDto(user);
        addAvatarUrls(response.getProfile(), user.getProfile().getAvatarKey());
        return response;
    }

    public UserAdminResponse getUserDetailsForAdmin(Long userId) {
        var user = userService.getUserById(userId);
        UserAdminResponse response = mapper.toUserAdminDto(user);
        addAvatarUrls(response.getProfile(), user.getProfile().getAvatarKey());
        return response;
    }

    public Page<UserAdminResponse> findAllUsersDetailsWithPaginationForAdmin(
            Integer page,
            Integer size,
            String userName,
            String sortBy,
            String sortDir
    ) {
        Page<User> users = userService.findAllUsersWithPagination(page, size, userName, sortBy, sortDir);
        return users.map(user -> {
            var dto = mapper.toUserAdminDto(user);
            addAvatarUrls(dto.getProfile(), user.getProfile().getAvatarKey());
            return dto;
        });
    }

    public List<UserPublicResponse> findAllUserPublicDetailsByUserName(String userName) {
        var users = userService.findByUserNameStartingWith(userName);
        return users.stream()
                .map(user -> {
                    var dto = mapper.toPublicDto(user);
                    addAvatarUrls(dto.getProfile(), user.getProfile().getAvatarKey());
                    return dto;
                })
                .toList();
    }

    public List<UserPublicResponse> findAllBlockedUsersByUser(Long userId) {
        List<Long> blockedIds = userBlockService.findBlockedUserIds(userId);
        List<User> blockedUsers;
        if (blockedIds.isEmpty()) {
            return Collections.emptyList();
        }
        blockedUsers = userService.findUsersInBatches(blockedIds);
        return blockedUsers.stream()
                .map(user -> {
                    var dto = mapper.toPublicDto(user);
                    addAvatarUrls(dto.getProfile(), user.getProfile().getAvatarKey());
                    return dto;
                })
                .toList();
    }

    public UserPrivateResponse updateUserName(Long userId, UpdateUserNameRequest requestData) {
        var user = userService.updateUserName(userId, requestData);
        UserPrivateResponse response = mapper.toPrivateDto(user);
        addAvatarUrls(response.getProfile(), user.getProfile().getAvatarKey());
        return response;
    }

    public UserPrivateResponse updateUserProfile(Long userId, UserProfileUpdateRequest requestData) {
        var user = userProfileService.updateUserProfile(userId, requestData);
        UserPrivateResponse response = mapper.toPrivateDto(user);
        addAvatarUrls(response.getProfile(), user.getProfile().getAvatarKey());
        return response;
    }

    public UserStatResponse getUsersStat() {
        return new UserStatResponse(
                userService.getTotalUsers(),
                userStatusServiceImpl.getAllOnlineUsersCount()
        );
    }

    // Вспомогательный метод для наполнения ответа профиля
    public void addAvatarUrls(ProfileResponse profileResponse, String avatarKey) {
        if (avatarKey == null) {
            return;
        }

        profileResponse.setAvatarUrl(
                userProfileService.generateAvatarUrl(AvatarType.ORIGINAL.getValue() + avatarKey)
        );
        profileResponse.setAvatarThumbUrl(
                userProfileService.generateAvatarUrl(AvatarType.THUMBNAIL.getValue() + avatarKey)
        );
    }
}
