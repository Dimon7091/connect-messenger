package ru.gorbunov.connect.core.service;

import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;

import java.util.UUID;

@Service
public class UserProfileService {

    private final FileStorageProvider storageProvider;
    private final UserRepository userRepository;

    public UserProfileService(FileStorageProvider storageProvider, UserRepository userRepository) {
        this.storageProvider = storageProvider;
        this.userRepository = userRepository;
    }

    public void changeAvatar(Long userId, byte[] imageBytes, String contentType, String originalFilename) {
        // 1. Вырезаем расширение файла (например, ".jpg" или ".png")
        if (originalFilename == null) {
            originalFilename = "image";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 2. Генерируем уникальный КЛЮЧ (путь внутри бакета).
        String fileKey = "avatars/" + userId + "-" + UUID.randomUUID() + extension;

        // 3. Отправляем байты в хранилище через интерфейс
        storageProvider.upload(fileKey, imageBytes, contentType, StorageType.AVATAR);

        // 4. Сохраняем ТОЛЬКО СГЕНЕРИРОВАННЫЙ КЛЮЧ в базу данных этого пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.getProfile().setAvatarUrl(fileKey); // Записываем строку "avatars/45-a1b2..." в колонку avatar_key
        userRepository.save(user);
    }
}
