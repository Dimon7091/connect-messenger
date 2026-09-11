package ru.connect.messenger.shared.dto;

import java.util.List;
import java.util.Map;


public record NotificationTask(
        List<String> external_id,
        String title,
        String message,
        String chromeWebImage,
        String url,
        Map<String, String> data
) {
}
