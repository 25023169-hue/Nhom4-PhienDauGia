package io.auctionsystem.client.pattern;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;
import java.util.List;

public class WebSocketClientManager {
    private static WebSocketClientManager instance;
    private StompSession stompSession;

    private WebSocketClientManager() {}

    public static WebSocketClientManager getInstance() {
        if (instance == null) {
            instance = new WebSocketClientManager();
        }
        return instance;
    }

    public void connect() {
        if (stompSession != null && stompSession.isConnected()) return;

        // Khởi tạo STOMP Client hỗ trợ giao tiếp WebSocket
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new CompositeMessageConverter(List.of(
                new StringMessageConverter(),
                new MappingJackson2MessageConverter()
        )));

        try {
            // Kết nối tới endpoint /ws của Server (đã cấu hình ở WebSocketConfig)
            stompSession = stompClient.connectAsync("ws://localhost:8080/ws", new StompSessionHandlerAdapter() {}).get();
            System.out.println(">>> Client đã kết nối WebSocket thành công!");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(">>> Lỗi kết nối WebSocket từ Client: " + e.getMessage());
        }
    }

    public StompSession getSession() {
        return stompSession;
    }
}
