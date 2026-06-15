package ru.gorbunov.connect.core.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.user.UserProfileUpdateRequest;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.AvatarType;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class UserProfileService {

    private final FileStorageProvider storageProvider;
    private final UserRepository userRepository;
    private final UserMapper mapper;

    public UserProfileService(FileStorageProvider storageProvider,
                              UserRepository userRepository,
                              UserMapper mapper
    ) {
        this.storageProvider = storageProvider;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public User updateUserProfile(Long userId, UserProfileUpdateRequest requestData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        mapper.updateProfile(requestData, user);
        return userRepository.save(user);
    }

    public void changeAvatar(Long userId, byte[] imageBytes, String contentType, String originalFilename) {
        //  Вырезаем расширение файла (например, ".jpg" или ".png")
        if (originalFilename == null) {
            originalFilename = "image";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        byte[] resizedImageBytes;
        try {
            resizedImageBytes = resizeTo56(imageBytes, extension);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать миниатюру изображения", e);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String oldAvatarKey = user.getProfile().getAvatarKey();
        String newAvatarKey = userId + "-" + UUID.randomUUID() + extension;
        // Отправляем байты в хранилище через интерфейс
        storageProvider.upload(
                AvatarType.ORIGINAL.getValue() + newAvatarKey,
                imageBytes,
                contentType,
                StorageType.AVATAR
        );
        storageProvider.upload(
                AvatarType.THUMBNAIL.getValue() + newAvatarKey,
                resizedImageBytes,
                contentType,
                StorageType.AVATAR
        );

        // Сохраняем ТОЛЬКО СГЕНЕРИРОВАННЫЙ КЛЮЧ в базу данных этого пользователя
        user.getProfile().setAvatarKey(newAvatarKey);
        userRepository.save(user);

        if (oldAvatarKey != null) {
            try {
                storageProvider.delete(AvatarType.ORIGINAL.getValue() + oldAvatarKey, StorageType.AVATAR);
                storageProvider.delete(AvatarType.THUMBNAIL.getValue() + oldAvatarKey, StorageType.AVATAR);
            } catch (Exception e) {
                // Логируем ошибку, но не прерываем выполнение.
                // Даже если старый файл не удалился, новый уже успешно применился в БД.
                log.error("Не удалось удалить старый аватар из S3: {}", oldAvatarKey, e);
            }
        }
    }

    public String generateAvatarUrl(String avatarKey) {
        if (avatarKey == null) {
            return null;
        }

        return storageProvider.generatePresignedUrl(
                avatarKey,
                StorageType.AVATAR,
                60);
    }

    // Вспомогательный метод для миниатюры аватара
    public static byte[] resizeTo56(byte[] imageBytes, String formatName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Массив байтов изображения пуст");
        }

        // Очищаем формат от точек и переводим в нижний регистр ("PNG" -> "png")
        String cleanFormat = formatName.replace(".", "").toLowerCase();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 1. Thumbnailator сжимает изображение прямо из входного потока
            BufferedImage resizedImage = Thumbnails.of(bais)
                    .size(56, 56)
                    .keepAspectRatio(true) // Сохраняем пропорции (картинка не исказится)
                    .asBufferedImage();

            // 2. Записываем полученный BufferedImage обратно в поток байтов
            boolean success = ImageIO.write(resizedImage, cleanFormat, baos);

            if (!success) {
                throw new IOException("Не удалось записать изображение в формате: " + cleanFormat);
            }

            // 3. Возвращаем готовый массив байтов
            return baos.toByteArray();
        }
    }
}
