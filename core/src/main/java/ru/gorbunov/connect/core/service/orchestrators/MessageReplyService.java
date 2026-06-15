package ru.gorbunov.connect.core.service.orchestrators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gorbunov.connect.core.dto.ws.ReplyContext;
import ru.gorbunov.connect.core.service.MessageService;
import ru.gorbunov.connect.core.service.UserService;

@Service
public class MessageReplyService {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    public ReplyContext getReplyContext(Long replyToId) {
        var msg =  messageService.getMessageById(replyToId);
        var senderFullName = userService.getUserFullName(msg.getSenderId());
        var textSnippet = (msg.getText().length() > 50)
                ? msg.getText().substring(0, 50) : msg.getText();
        return new ReplyContext(msg.getId(), senderFullName, textSnippet);
    }
}
