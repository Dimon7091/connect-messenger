package ru.gorbunov.connect.core.service.orchestrators;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.user.ProfileResponse;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserAdminResponse;
import ru.gorbunov.connect.core.dto.user.UserPrivateResponse;
import ru.gorbunov.connect.core.dto.user.UserProfileUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserPublicResponse;
import ru.gorbunov.connect.core.dto.user.UserStatResponse;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.AvatarType;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserBlockService;
import ru.gorbunov.connect.core.service.UserProfileService;
import ru.gorbunov.connect.core.service.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class UserProviderService {
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserBlockService userBlockService;
    private final StatusService statusService;
    private final UserMapper mapper;

    private final UserRepository userRepository;

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
                statusService.getAllOnlineUsersCount()
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
