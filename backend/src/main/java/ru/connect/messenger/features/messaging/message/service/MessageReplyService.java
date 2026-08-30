package ru.connect.messenger.features.messaging.message.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.connect.messenger.features.messaging.message.domain.ReplyContext;
import ru.connect.messenger.features.user.api.UserService;
import ru.connect.messenger.features.user.service.UserServiceImpl;

@Service
@AllArgsConstructor
public class MessageReplyService {
    private final MessageServiceImpl messageService;
    private final UserService userService;

    public ReplyContext getReplyContext(Long replyToId) {
        var msg =  messageService.getMessageById(replyToId);
        var senderFullName = userService.getUserFullName(msg.getSenderId());
        var textSnippet = (msg.getText().length() > 50)
                ? msg.getText().substring(0, 50) : msg.getText();
        return new ReplyContext(msg.getId(), senderFullName, textSnippet);
    }
}
