package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.ws.MessageNewResponse;
import ru.gorbunov.connect.core.mapper.MessageMapper;
import ru.gorbunov.connect.core.service.MessageService;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageControllerV1 {
    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper mapper;

    @GetMapping("/chats/{id}")
    public List<MessageNewResponse> getChatMessage(
            @PathVariable("id") long chatId,
            @RequestParam("limit") int limit,
            @RequestParam(value = "beforeTimestamp", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime beforeTimestamp) {

        // Если параметр не пришел, используем текущее время
        OffsetDateTime timestamp = (beforeTimestamp != null) ? beforeTimestamp : OffsetDateTime.now();
        var messages = messageService.findChatMessages(chatId, limit, timestamp);
        return messages.stream()
                .map(m -> mapper.toDto(m))
                .toList();
    }
}
