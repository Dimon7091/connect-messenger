package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserProfileUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.service.UserProfileService;
import ru.gorbunov.connect.core.service.orchestrators.UserProviderService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileControllerV1 {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProviderService userProviderService;

    @PostMapping("/{id}/profile/upload-avatar")
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

    @PostMapping("/{id}/profile/update")
    public ResponseEntity<UserResponse> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest requestData,
            @AuthenticationPrincipal Jwt token
    ) {
        var userId = Long.parseLong(token.getClaim("sub"));
        var response = userProviderService.updateUserProfile(userId, requestData);
        return ResponseEntity.ok()
                .body(response);
    }
}
