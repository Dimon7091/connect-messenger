package ru.connect.messenger.features.messaging.chat.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.connect.messenger.features.messaging.chat.dto.ChatCreateOrGetRequest;
import ru.connect.messenger.features.messaging.chat.dto.ChatResponse;
import ru.connect.messenger.features.messaging.chat.mapper.ChatMapper;
import ru.connect.messenger.features.messaging.chat.service.ChatCleanupService;
import ru.connect.messenger.features.messaging.chat.service.ChatParticipantService;
import ru.connect.messenger.features.messaging.chat.service.ChatServiceImpl;


import java.util.List;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
public class ChatControllerV1 {
    private final ChatServiceImpl chatService;
    private final ChatParticipantService chatParticipantService;
    private final ChatCleanupService chatCleanupService;
    private final ChatMapper mapper;

    @PostMapping("")
    private ChatResponse createOrGetDirectChat(
            @RequestBody ChatCreateOrGetRequest requestData,
            @AuthenticationPrincipal Jwt token
    ) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        var chat = chatService.createOrGetDirectChat(
                currentUserId, requestData.companionId()
        );
        chatParticipantService.setIsDeleted(chat.getId(), currentUserId, false);
        return mapper.toDto(chat);
    }

    @GetMapping()
    private List<ChatResponse> getAllUserChats(@AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        return chatService.findAllDirectChatsByUser(currentUserId);
    }

    @GetMapping("/{id}")
    private ChatResponse getChat(@PathVariable("id") Long chatId) {
        var chat = chatService.findChatById(chatId);
        if (chat == null) {
            return null;
        }
        return mapper.toDto(chat);
    }

    @GetMapping("/participants")
    private ChatResponse getChatByParticipants(@RequestParam("id1") long userId1,
                                               @RequestParam("id2") long userId2) {
        return mapper.toDto(chatService.findChatByParticipants(userId1, userId2));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChatForUser(
            @PathVariable("id") Long chatId,
            @AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        chatCleanupService.clearChatForUser(chatId, currentUserId);
    }

    @DeleteMapping("/{id}/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearChatHistory(
            @PathVariable("id") Long chatId,
            @AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        chatCleanupService.clearChatHistoryForUser(chatId, currentUserId);
    }
}
