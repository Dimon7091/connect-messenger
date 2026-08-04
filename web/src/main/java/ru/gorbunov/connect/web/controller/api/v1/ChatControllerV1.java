package ru.gorbunov.connect.web.controller.api.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import ru.gorbunov.connect.core.dto.chat.ChatCreateOrGetRequest;
import ru.gorbunov.connect.core.dto.chat.ChatResponse;
import ru.gorbunov.connect.core.mapper.ChatMapper;
import ru.gorbunov.connect.core.service.ChatParticipantService;
import ru.gorbunov.connect.core.service.ChatService;
import ru.gorbunov.connect.core.service.MessageService;
import ru.gorbunov.connect.core.service.orchestrators.ChatCleanupService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
public class ChatControllerV1 {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatParticipantService chatParticipantService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatCleanupService chatCleanupService;

    @Autowired
    private ChatMapper mapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    @GetMapping("/{id}")
    private ChatResponse getChat(@PathVariable("id") Long chatId) {
        var chat = chatService.findChatById(chatId);
        if (chat == null) {
            return null;
        }
        return mapper.toDto(chat);
    }

    @GetMapping("/users")
    private List<ChatResponse> getAllUserChats(@AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        return chatService.findAllDirectChatsByUser(currentUserId);
    }

    @GetMapping("/participants")
    private ChatResponse getChatByParticipants(@RequestParam("id1") long userId1,
                                               @RequestParam("id2") long userId2) {
        return mapper.toDto(chatService.findChatByParticipants(userId1, userId2));
    }

    @DeleteMapping("/{id}/participant")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChatForUser(
            @PathVariable("id") Long chatId,
            @AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        chatCleanupService.clearChatForUser(chatId, currentUserId);
    }

    @DeleteMapping("/{id}/clear-chat-history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearChatHistory(
            @PathVariable("id") Long chatId,
            @AuthenticationPrincipal Jwt token) {
        var currentUserId = Long.parseLong(token.getClaim("sub"));
        chatCleanupService.clearChatHistoryForUser(chatId, currentUserId);
    }
}
