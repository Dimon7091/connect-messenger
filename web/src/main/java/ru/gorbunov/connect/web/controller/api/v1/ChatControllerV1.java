package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import ru.gorbunov.connect.core.repository.ChatRepository;
import ru.gorbunov.connect.core.service.ChatParticipantService;
import ru.gorbunov.connect.core.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatControllerV1 {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMapper mapper;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatParticipantService chatParticipantService;

    @PostMapping("")
    private ChatResponse createOrGetDirectChat(@RequestBody ChatCreateOrGetRequest requestData) {
        var chat = chatService.createOrGetDirectChat(
                requestData.participants().getFirst(), requestData.participants().getLast()
        );
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

    @GetMapping("/users/{id}")
    private List<ChatResponse> getAllUserChats(@PathVariable("id") Long userId) {
        var chats = chatService.findAllDirectChatsByUser(userId);

        return chats.stream()
                .map(chat -> {
                    var chatResponse = mapper.toDto(chat);
                    var unreadCount = chatParticipantService.getUnreadCount(chat.getId(), userId);
                    chatResponse.setUnreadCount(unreadCount);
                    chatResponse.setLastMessage(chat.getLastMessage());
                    chatResponse.setUpdatedAt(chat.getUpdatedAt());
                    return chatResponse;
                })
                .toList();
    }

    @GetMapping("/participants")
    private ChatResponse getChatByParticipants(@RequestParam("id1") long userId1,
                                               @RequestParam("id2") long userId2) {
        return mapper.toDto(chatService.findChatByParticipants(userId1, userId2));
    }

    @DeleteMapping("/{chatId}/participants/{participant}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChatParticipant(
            @PathVariable("chatId") Long chatId,
            @PathVariable("participant") Long participantId) {
        chatService.deleteChatForUser(chatId, participantId);
    }
}
