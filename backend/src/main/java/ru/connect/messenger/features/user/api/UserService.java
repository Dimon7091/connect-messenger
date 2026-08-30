package ru.connect.messenger.features.user.api;

import org.springframework.data.domain.Page;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UpdateUserNameRequest;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;

import java.util.List;

public interface UserService {
    User create(UserCreateRequest requestData, Role role);
    User save(User user);
    User getUserById(Long id);
    List<User> findUsersInBatches(List<Long> ids);
    Page<User> findAllUsersWithPagination(Integer page, Integer size, String userName, String sortBy, String sortDir);
    List<User> findByUserNameStartingWith(String userName);
    String getUserFullName(Long userId);
    long getTotalUsers();
    UserPrivateResponse findByUserName(String userName);
    List<User> findUsersByRole(Role role);
    Long findUserIdByUserName(String userName);
    boolean isAdmin(Long userId);
    boolean isAdmin(String userName);
    boolean isDeleted(Long userId);
    User updateUserName(Long id, UpdateUserNameRequest requestData);
    void delete(Long id);
}