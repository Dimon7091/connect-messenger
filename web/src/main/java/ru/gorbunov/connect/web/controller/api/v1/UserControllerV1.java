package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.validation.Valid;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.gorbunov.connect.core.dto.user.UpdateUserNameRequest;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.dto.user.UserStatResponse;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.service.StatusService;
import ru.gorbunov.connect.core.service.UserProfileService;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.core.service.UserStatusSubscriptionService;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {

    private static final Log log = LogFactory.getLog(UserControllerV1.class);
    private final UserService userService;
    private final UserProfileService userProfileService;

    @Autowired
    public StatusService statusService;

    @Autowired
    public UserStatusSubscriptionService userStatusSubscriptionService;


    public UserControllerV1(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest requestData) {
        var savedUser = userService.create(requestData, Role.ROLE_USER);
        return ResponseEntity.created(URI.create("api/v1/users/" + savedUser.id()))
                .body(savedUser);
    }

    @GetMapping("")
    public ResponseEntity<Page<UserResponse>> index(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var users = userService.findAll(pageable);
        return ResponseEntity.ok()
                .body(users);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserResponse> show(@PathVariable("id") Long id) {
        var user = userService.findById(id);
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var user = userService.findById(Long.parseLong(jwt.getClaim("sub")));
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> search(@RequestParam("username") String userName) {
        var users = userService.findByUserNameStartingWith(userName);
        return ResponseEntity.ok()
                .body(users);
    }

    @GetMapping("/stat")
    public UserStatResponse stat() {
       return userService.getUsersStat();
    }

    @PatchMapping("/{id}/update-username")
    public ResponseEntity<UserResponse> updateUserName(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserNameRequest requestData
    ) {
        var updatedUser = userService.updateUserName(id, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> putUpdate(
            @PathVariable("id") Long id,
            @RequestBody UserPutUpdateRequest requestData,
            @AuthenticationPrincipal Jwt token
    ) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        var updatedUser = userService.putUpdate(currentUserId, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patchUpdate(
            @PathVariable("id") Long id,
            @RequestBody UserPatchUpdateRequest requestData
    ) {
        var updatedUser = userService.patchUpdate(id, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @PostMapping("/{id}/avatar/upload")
    public ResponseEntity<String> uploadAvatar(
            @PathVariable("id") Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        // Валидация: проверяем, что файл не пустой
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не выбран");
        }

        // Передаем ID пользователя, массив байт, тип файла (image/png) и оригинальное имя файла в Core-слой
        userProfileService.changeAvatar(
                userId,
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename()
        );

        return ResponseEntity.ok("Аватар успешно обновлен!");
    }

    @PostMapping("/{id}/avatar/download")
    public ResponseEntity<String> downloadAvatar(
            @PathVariable("id") Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        // Валидация: проверяем, что файл не пустой
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не выбран");
        }

        // Передаем ID пользователя, массив байт, тип файла (image/png) и оригинальное имя файла в Core-слой
        userProfileService.changeAvatar(
                userId,
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename()
        );

        return ResponseEntity.ok("Аватар успешно обновлен!");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        userService.delete(id);
        userStatusSubscriptionService.cleanupUserFully(id);
        statusService.deleteStatusFromDatabase(id);
    }
}
