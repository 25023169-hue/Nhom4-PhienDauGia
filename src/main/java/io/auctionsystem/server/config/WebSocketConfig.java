package io.auctionsystem.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Cấu hình broker để phát dữ liệu tới các client đang subscribe (Lắng nghe)
    config.enableSimpleBroker("/topic");
    // Prefix cho các thông điệp gửi từ Client lên Server (nếu cần)
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Mở endpoint cho Client kết nối vào WebSocket
    registry.addEndpoint("/ws").setAllowedOrigins("*");
  }
}
