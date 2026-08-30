package ru.connect.messenger.features.storage;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3FilenameValidator implements FilenameStorageValidator {

    private static final String SAFE_CHAR_REGEX = "[^a-zA-Z0-9.\\-_]";
    private static final int S3_MAX_KEY_LENGTH_BYTES = 1024;
    private static final String DEFAULT_EXTENSION = ".bin";
    private static final String TRUNCATED_SUFFIX = "_truncated";

    @Override
    public String generateSafeS3Key(String originalFilename) {
        if (originalFilename == null || originalFilename.strip().isEmpty()) {
            return generateFallbackName();
        }

        String cleanedName = originalFilename.strip();
        cleanedName = getSimpleFilename(cleanedName);

        String extension = getFileExtension(cleanedName);
        String nameWithoutExtension = getFileNameWithoutExtension(cleanedName, extension);

        String safeBaseName = nameWithoutExtension.replaceAll(SAFE_CHAR_REGEX, "_");

        if (safeBaseName.replace("_", "").isEmpty()) {
            safeBaseName = "file";
        }

        if (safeBaseName.equals(".") || safeBaseName.equals("..") || safeBaseName.contains("..")) {
            safeBaseName = "safe";
        }

        // Корректно собираем имя с суффиксом UUID и расширением
        String uniqueSuffix = "_" + UUID.randomUUID().toString().substring(0, 8);
        String safeFileName = safeBaseName + uniqueSuffix + extension;

        // Проверяем длину итогового файла
        if (getUtf8ByteLength(safeFileName) > S3_MAX_KEY_LENGTH_BYTES) {
            safeFileName = truncateFilename(safeBaseName, uniqueSuffix + extension);
        }

        return safeFileName;
    }

    private static String getSimpleFilename(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot).toLowerCase();
        }
        return "";
    }

    private static String getFileNameWithoutExtension(String filename, String extension) {
        if (extension.isEmpty()) {
            return filename;
        }
        return filename.substring(0, filename.length() - extension.length());
    }

    private static int getUtf8ByteLength(String str) {
        return str.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String truncateFilename(String baseName, String suffixAndExtension) {
        // Вычисляем точный лимит для базовой части имени
        int reservedBytes = getUtf8ByteLength(TRUNCATED_SUFFIX + suffixAndExtension);
        int availableBytes = S3_MAX_KEY_LENGTH_BYTES - reservedBytes;

        StringBuilder sb = new StringBuilder();
        int currentBytes = 0;

        // Посимвольный подсчет без пересоздания строк на каждой итерации
        for (char c : baseName.toCharArray()) {
            int charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
            if (currentBytes + charBytes > availableBytes) {
                break;
            }
            sb.append(c);
            currentBytes += charBytes;
        }

        return sb.toString() + TRUNCATED_SUFFIX + suffixAndExtension;
    }

    private static String generateFallbackName() {
        return "upload_" + UUID.randomUUID().toString() + DEFAULT_EXTENSION;
    }
}
