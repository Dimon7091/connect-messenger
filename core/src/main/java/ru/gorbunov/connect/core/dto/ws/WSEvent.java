package ru.gorbunov.connect.core.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Полезно для создания событий
public class WSEvent<T> {

    private EventType type; // Используем Enum вместо String
    private T payload;

    // Внутренний Enum
    @RequiredArgsConstructor
    public enum EventType {
        CONNECTION("connection"),
        MESSAGE_NEW("message_new"),
        MESSAGE_SENT("message_sent"),
        MESSAGE_DELIVERED("message_delivered"),
        MESSAGE_READ("message_read"),
        USER_STATUS("user_status"),
        TYPING_START("typing_start"),
        TYPING_STOP("typing_stop"),
        CHAT_STORE("chat_store"),
        ERROR("error"),
        PONG("pong");

        private final String value;

        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() {
            return value;
        }

        // Позволяет Jackson правильно сопоставить строку из JSON с элементом Enum
        @com.fasterxml.jackson.annotation.JsonCreator
        public static EventType fromValue(String value) {
            for (EventType type : EventType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown event type: " + value);
        }
    }
}
