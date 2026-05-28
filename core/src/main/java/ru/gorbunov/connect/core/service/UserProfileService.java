package ru.gorbunov.connect.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;

import java.util.UUID;

@Slf4j
@Service
public class UserProfileService {

    private final FileStorageProvider storageProvider;
    private final UserRepository userRepository;

    public UserProfileService(FileStorageProvider storageProvider, UserRepository userRepository) {
        this.storageProvider = storageProvider;
        this.userRepository = userRepository;
    }

    public void changeAvatar(Long userId, byte[] imageBytes, String contentType, String originalFilename) {
        //  Вырезаем расширение файла (например, ".jpg" или ".png")
        if (originalFilename == null) {
            originalFilename = "image";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        String oldAvatarKey = user.getProfile().getAvatarKey();
        String newAvatarKey = "avatars/" + userId + "-" + UUID.randomUUID() + extension;

        // Отправляем байты в хранилище через интерфейс
        storageProvider.upload(newAvatarKey, imageBytes, contentType, StorageType.AVATAR);

        // Сохраняем ТОЛЬКО СГЕНЕРИРОВАННЫЙ КЛЮЧ в базу данных этого пользователя
        user.getProfile().setAvatarKey(newAvatarKey); // Записываем строку "avatars/45-a1b2..." в колонку avatar_key
        userRepository.save(user);

        if (oldAvatarKey != null) {
            try {
                storageProvider.delete(oldAvatarKey, StorageType.AVATAR);
            } catch (Exception e) {
                // Логируем ошибку, но не прерываем выполнение.
                // Даже если старый файл не удалился, новый уже успешно применился в БД.
                log.error("Не удалось удалить старый аватар из S3: {}", oldAvatarKey, e);
            }
        }
    }

    public String generateAvatarUrl(String avatarKey) {
        if (avatarKey == null) return null;

        return storageProvider.generatePresignedUrl(
                avatarKey,
                StorageType.AVATAR,
                60);
    }
}
