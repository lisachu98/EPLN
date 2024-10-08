package centralbank.config;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class WebSocketClient {
    private final WebSocketStompClient stompClient;

    public WebSocketClient(String url, StompSessionHandler sessionHandler) {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new StringMessageConverter());
        this.stompClient.connect(url, sessionHandler);
    }
}