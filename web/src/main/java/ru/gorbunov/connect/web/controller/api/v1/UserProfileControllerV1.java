package ru.gorbunov.connect.web.controller.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.gorbunov.connect.core.service.UserProfileService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileControllerV1 {

    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("/{id}/profile/avatar-upload")
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
}
