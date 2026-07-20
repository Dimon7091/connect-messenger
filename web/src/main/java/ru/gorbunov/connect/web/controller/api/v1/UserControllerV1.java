package ru.gorbunov.connect.web.controller.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.exception.UserDeletedException;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserBlockService;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.core.service.UserStatusSubscriptionService;
import ru.gorbunov.connect.core.service.orchestrators.UserProviderService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {

    private final UserService userService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserProviderService userProviderService;

    @Autowired
    private UserStatusSubscriptionService userStatusSubscriptionService;

    @Autowired
    private UserBlockService userBlockService;


    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserResponse> show(@PathVariable("id") Long id) {
        var user = userProviderService.getUserDetails(id);
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var user = userProviderService.getUserDetails(Long.parseLong(jwt.getClaim("sub")));
        if (user.getIsDeleted()) {
            throw new UserDeletedException("Пользователь удален");
        }
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> search(@RequestParam("username") String userName) {
        var users = userProviderService.findAllUserDetailsByUserName(userName);
        return ResponseEntity.ok()
                .body(users);
    }

    @PatchMapping("/{id}/update-username")
    public ResponseEntity<UserResponse> updateUserName(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserNameRequest requestData
    ) {
        var updatedUser = userProviderService.updateUserName(id, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @PostMapping("/{id}/blacklist")
    public ResponseEntity<Long> addToBlackList(
            @PathVariable("id") Long blockedId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var currentUserId = Long.parseLong(jwt.getClaim("sub"));
        var response = userBlockService.blockUserByUser(currentUserId, blockedId);
        return ResponseEntity.ok()
                .body(response);
    }

    @DeleteMapping("/{id}/blacklist")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromBlackList(
            @PathVariable("id") Long blockedId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var currentUserId = Long.parseLong(jwt.getClaim("sub"));
        userBlockService.removeBlockUserByUser(currentUserId, blockedId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        userService.delete(id);
        userStatusSubscriptionService.cleanupUserFully(id);
        statusService.deleteStatusFromDatabase(id);
    }
}
