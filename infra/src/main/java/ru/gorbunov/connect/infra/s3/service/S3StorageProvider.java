package ru.gorbunov.connect.infra.s3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.models.FileStorageProvider;
import ru.gorbunov.connect.core.models.StorageType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

@Service
public class S3StorageProvider implements FileStorageProvider {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String avatarBucket;
    private final String chatFilesBucket;

    public S3StorageProvider(
            S3Presigner s3Presigner,
            S3Client s3Client,
            @Value("${yandex.storage.buckets.avatars}") String avatarBucket,
            @Value("${yandex.storage.buckets.chat-files}") String chatFilesBucket) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.avatarBucket = avatarBucket;
        this.chatFilesBucket = chatFilesBucket;
    }

    @Override
    public void upload(String key, byte[] content, String contentType, StorageType type) {
        String bucket = resolveBucket(type);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    @Override
    public InputStream download(String key, StorageType type) {
        String bucket = resolveBucket(type);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    @Override
    public String generatePresignedUrl(String key, StorageType type, long durationInMinutes) {
        String bucket = resolveBucket(type);

        // Формируем стандартный запрос на получение объекта
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        // Оборачиваем его в запрос на подпись с указанием времени жизни
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationInMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        // Генерируем подписанный запрос
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        // Возвращаем готовую временную строку-ссылку с токеном авторизации от Яндекса
        return presignedRequest.url().toString();
    }

    @Override
    public void delete(String key, StorageType type) {
        String bucket = resolveBucket(type);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    // Вспомогательный метод для выбора бакета
    private String resolveBucket(StorageType type) {
        return switch (type) {
            case AVATAR -> avatarBucket;
            case CHAT_FILE -> chatFilesBucket;
        };
    }
}
