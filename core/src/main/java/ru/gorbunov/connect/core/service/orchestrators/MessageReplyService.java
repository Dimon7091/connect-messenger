package ru.gorbunov.connect.core.service.orchestrators;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.ws.ReplyContext;
import ru.gorbunov.connect.core.service.MessageService;
import ru.gorbunov.connect.core.service.UserService;

@Service
@AllArgsConstructor
public class MessageReplyService {
    private MessageService messageService;
    private UserService userService;

    public ReplyContext getReplyContext(Long replyToId) {
        var msg =  messageService.getMessageById(replyToId);
        var senderFullName = userService.getUserFullName(msg.getSenderId());
        var textSnippet = (msg.getText().length() > 50)
                ? msg.getText().substring(0, 50) : msg.getText();
        return new ReplyContext(msg.getId(), senderFullName, textSnippet);
    }
}
