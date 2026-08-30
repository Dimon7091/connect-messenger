package ru.connect.messenger.features.user.api;


import org.springframework.data.domain.Page;
import ru.connect.messenger.features.user.dto.UpdateUserNameRequest;
import ru.connect.messenger.features.user.dto.UserAdminResponse;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.dto.UserProfileUpdateRequest;
import ru.connect.messenger.features.user.dto.UserPublicResponse;
import ru.connect.messenger.features.user.dto.UserStatResponse;

import java.util.List;

public interface UserProviderService {
    UserPrivateResponse getUserDetailsForAuth(Long userId);
    UserPublicResponse findUserPublicDetails(Long userId);
    UserAdminResponse getUserDetailsForAdmin(Long userId);
    Page<UserAdminResponse> findAllUsersDetailsWithPaginationForAdmin(Integer page, Integer size, String userName, String sortBy, String sortDir);
    List<UserPublicResponse> findAllUserPublicDetailsByUserName(String userName);
    List<UserPublicResponse> findAllBlockedUsersByUser(Long userId);
    UserPrivateResponse updateUserName(Long userId, UpdateUserNameRequest requestData);
    UserPrivateResponse updateUserProfile(Long userId, UserProfileUpdateRequest requestData);
    UserStatResponse getUsersStat();
}