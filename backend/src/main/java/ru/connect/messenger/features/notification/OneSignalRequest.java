package ru.connect.messenger.features.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record OneSignalRequest(
        @JsonProperty("app_id") String appId,
        @JsonProperty("include_aliases") Map<String, List<String>> includeAliases,
        @JsonProperty("target_channel") String targetChannel,
        @JsonProperty("headings") Map<String, String> headings,
        @JsonProperty("contents") Map<String, String> contents,
        @JsonProperty("data") Map<String, String> data
) {}
