package ru.connect.messenger.integration.message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import ru.connect.messenger.features.messaging.chat.domain.Chat;
import ru.connect.messenger.features.messaging.message.domain.Message;
import ru.connect.messenger.features.messaging.message.dto.MessageNewResponse;
import ru.connect.messenger.features.messaging.message.dto.SendMessageRequest;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.integration.BaseIT;
import ru.connect.messenger.shared.dto.WSEvent;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessageSendingIT extends BaseIT {
    @LocalServerPort
    private int port;
    private WebSocketStompClient stompClient;

    private User userA;
    private User userB;
    private User userC;

    private String jwtTokenA;
    private String jwtTokenB;
    private String jwtTokenC;

    private Chat chatAB;
    private Chat chatAC;
    private Chat chatCB;

    @BeforeAll
    void init() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient); // <--- Обернули в SockJS
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        var userACreateRequest = new UserCreateRequest(
                "user-a",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        var userBCreateRequest = new UserCreateRequest(
                "user-b",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        var userCCreateRequest = new UserCreateRequest(
                "user-c",
                "FirstName",
                "LastName",
                "12345678",
                "token"
        );
        userA = getUserServiceImpl().create(userACreateRequest, Role.ROLE_USER);
        userB = getUserServiceImpl().create(userBCreateRequest, Role.ROLE_USER);
        userC = getUserServiceImpl().create(userCCreateRequest, Role.ROLE_USER);
        jwtTokenA = getJwtTokenProvider().generateToken(userA);
        jwtTokenB = getJwtTokenProvider().generateToken(userB);
        jwtTokenC = getJwtTokenProvider().generateToken(userC);

        chatAB = createChat(userA, userB);
        chatAC = createChat(userA, userC);
        chatCB = createChat(userC, userB);

    }

    @Test
    @DisplayName("Проверка отправки сообщения - ожидается успех")
    void sendMessage_validJwtToken_success() throws Exception {
        String urlUserA = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenA;
        String urlUserB = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenB;
        // Очередь, куда Получатель сложит пришедшее сообщение
        BlockingQueue<WSEvent<MessageNewResponse>> receiverQueue = new LinkedBlockingQueue<>();

        StompSession receiverSession = stompClient
                .connectAsync(urlUserB, new StompSessionHandlerAdapter() { }) // Заголовки пустые!
                .get(3, TimeUnit.SECONDS);

        // Получатель подписывается на СВОЮ личную очередь
        receiverSession.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WSEvent.class; // Возвращаем базовый класс события
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receiverQueue.add((WSEvent) payload);
            }
        });

        StompSession senderSession = stompClient
                .connectAsync(urlUserA, new StompSessionHandlerAdapter() { })
                .get(3, TimeUnit.SECONDS);

        // Отправитель шлет сообщение на сервер, указывая, что оно для UserB
        SendMessageRequest messageToSend = new SendMessageRequest();
        messageToSend.setChatId(chatAB.getId().toString());
        messageToSend.setSenderId(userA.getId().toString());
        messageToSend.setReceiverId(userB.getId().toString());
        messageToSend.setTimestamp(OffsetDateTime.now().toString());
        messageToSend.setText("Привет, это секретное сообщение!");
        WSEvent<SendMessageRequest> messageEvent = new WSEvent<>(WSEvent.EventType.MESSAGE_SENT, messageToSend);

        senderSession.send("/app/message_sent", messageEvent);

        // Ждем 5 секунд, пока сообщение долетит до Получателя
        WSEvent<?> receivedEvent = receiverQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedEvent, "UserB так и не получил сообщение!");

        // Принудительно конвертируем LinkedHashMap в целевой DTO класс
        MessageNewResponse responsePayload = getObjectMapper().convertValue(
                receivedEvent.getPayload(),
                MessageNewResponse.class
        );

        assertNotNull(responsePayload, "Payload события пустой!");
        assertEquals(messageToSend.getText(), responsePayload.getText(), "Текст сообщения не сопадает!");
        assertEquals(
                messageToSend.getSenderId(),
                responsePayload.getSenderId(),
                "Автор сообщения не совпадает!"
        );

        // Проверка базы данных
        var messageInDatabase = getMessageRepository()
                .findById(Long.valueOf(responsePayload.getId()));
        assertThat(messageInDatabase).isNotEmpty();
        assertEquals(messageInDatabase.get().getChatId(), chatAB.getId(), "Не собпадение id чата");
        assertEquals(messageInDatabase.get().getReceiverId(), userB.getId(), "Отсутствует получатель!");
        assertEquals(messageInDatabase.get().getSenderId(), userA.getId(), "Отсутствует отправитель!");
        assertEquals(messageInDatabase.get().getText(),
                messageToSend.getText(), "Тест сообщения не совпадает с отправленным!");
    }

    @Test
    @DisplayName("Проверка отправки сообщения между заблокированными пользователями - ожидается провал")
    void sendMessage_userAIsBlockedByUserC_fail() throws Exception {
        getUserBlockService().blockUserByUser(userC.getId(), userA.getId());
        OffsetDateTime timestamp = OffsetDateTime.now();

        String urlUserA = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenA;
        String urlUserC = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenC;
        // Очередь, куда Получатель сложит пришедшее сообщение
        BlockingQueue<WSEvent<MessageNewResponse>> receiverQueue = new LinkedBlockingQueue<>();

        StompSession receiverSession = stompClient
                .connectAsync(urlUserC, new StompSessionHandlerAdapter() { }) // Заголовки пустые!
                .get(3, TimeUnit.SECONDS);

        // Получатель подписывается на СВОЮ личную очередь
        receiverSession.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WSEvent.class; // Возвращаем базовый класс события
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receiverQueue.add((WSEvent) payload);
            }
        });

        StompSession senderSession = stompClient
                .connectAsync(urlUserA, new StompSessionHandlerAdapter() { })
                .get(3, TimeUnit.SECONDS);

        // Отправитель шлет сообщение на сервер, указывая, что оно для UserC
        SendMessageRequest messageToSend = new SendMessageRequest();
        messageToSend.setChatId(chatAC.getId().toString());
        messageToSend.setSenderId(userA.getId().toString());
        messageToSend.setReceiverId(userC.getId().toString());
        messageToSend.setTimestamp(OffsetDateTime.now().toString());
        messageToSend.setText("Привет, это секретное сообщение!");
        WSEvent<SendMessageRequest> messageEvent = new WSEvent<>(WSEvent.EventType.MESSAGE_SENT, messageToSend);

        senderSession.send("/app/message_sent", messageEvent);

        // Ждем 5 секунд, пока сообщение долетит до Получателя
        WSEvent<?> receivedEvent = receiverQueue.poll(5, TimeUnit.SECONDS);
        List<Message> chatMessages = getMessageRepository().findChatMessages(
                chatAC.getId(), 1, userC.getId(), timestamp
        );

        assertNull(receivedEvent, "UserC не должен получть сообщение!");
        assertThat(chatMessages)
                .withFailMessage("В базе данных найдены сообщения!")
                .isEmpty();
    }

    @Test
    @DisplayName("Проверка отправки сообщения удаленному пользователю - ожидается провал")
    void sendMessage_toDeletedUser_fail() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        getUserDeletionOrchestrator().softDelete(userB.getId());

        String urlUserC = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenC;
        String urlUserB = "ws://localhost:" + port + "/chat-ws?token=" + jwtTokenB;

        assertThatThrownBy(() -> {
            stompClient.connectAsync(urlUserB, new StompSessionHandlerAdapter() { })
                    .get(3, TimeUnit.SECONDS);
        })
                .withFailMessage("Удаленный пользователь неожиданно смог подключиться!")
                .isNotNull();

        StompSession senderSession = stompClient
                .connectAsync(urlUserC, new StompSessionHandlerAdapter() { })
                .get(3, TimeUnit.SECONDS);


        // Отправитель шлет сообщение на сервер, указывая, что оно для UserC
        SendMessageRequest messageToSend = new SendMessageRequest();
        messageToSend.setChatId(chatCB.getId().toString());
        messageToSend.setSenderId(userC.getId().toString());
        messageToSend.setReceiverId(userB.getId().toString());
        messageToSend.setTimestamp(OffsetDateTime.now().toString());
        messageToSend.setText("Привет, это секретное сообщение!");
        WSEvent<SendMessageRequest> messageEvent = new WSEvent<>(WSEvent.EventType.MESSAGE_SENT, messageToSend);

        senderSession.send("/app/message_sent", messageEvent);

        // Проверка базы данных
        List<Message> chatMessages = getMessageRepository().findChatMessages(
                chatCB.getId(), 1, userB.getId(), timestamp
        );

        assertThat(chatMessages)
                .withFailMessage("В базе данных найдены сообщения!")
                .isEmpty();
    }

    @AfterAll
    void clearDatabase() {
        clearUp();
    }
}
