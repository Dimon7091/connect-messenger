package ru.gorbunov.connect.core.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateUtils {

    // Приватный конструктор запрещает создавать экземпляры класса
    private DateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Конвертирует строку Unix Timestamp (секунды.микросекунды) в OffsetDateTime
     */
    public static OffsetDateTime parseTimestamp(String ts) {
        if (ts == null || ts.isBlank()) {
            return null;
        }

        try {
            double timestamp = Double.parseDouble(ts);
            long seconds = (long) timestamp;
            // (timestamp % 1) дает дробную часть, умножаем на 10^9 для наносекунд
            long nanos = (long) ((timestamp - seconds) * 1_000_000_000);

            return Instant.ofEpochSecond(seconds, nanos)
                    .atOffset(ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            // Можно пробросить свое исключение или вернуть null
            throw new IllegalArgumentException("Некорректный формат timestamp: " + ts);
        }
    }
}

