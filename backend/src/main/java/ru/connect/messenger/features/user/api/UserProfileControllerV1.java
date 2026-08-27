package ru.connect.messenger.features.user.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.dto.UserProfileUpdateRequest;
import ru.connect.messenger.features.user.service.UserProfileService;
import ru.connect.messenger.features.user.service.UserProviderService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileControllerV1 {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProviderService userProviderService;

    @PostMapping("/profile/upload-avatar")
    public ResponseEntity<String> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        var currentUserId = Long.parseLong(jwt.getClaim("sub"));
        // Валидация: проверяем, что файл не пустой
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не выбран");
        }

        // Передаем ID пользователя, массив байт, тип файла (image/png) и оригинальное имя файла в Core-слой
        userProfileService.changeAvatar(
                currentUserId,
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename()
        );

        return ResponseEntity.ok("Аватар успешно обновлен!");
    }

    @PatchMapping("/profile/update")
    public ResponseEntity<UserPrivateResponse> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest requestData,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var currentUserId = Long.parseLong(jwt.getClaim("sub"));
        var response = userProviderService.updateUserProfile(currentUserId, requestData);
        return ResponseEntity.ok()
                .body(response);
    }
}
