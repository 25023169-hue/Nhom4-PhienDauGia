package server.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class NotificationRealtimePublisherTest {

  @Mock private SimpMessagingTemplate messagingTemplate;
  private NotificationRealtimePublisher realtimePublisher;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    realtimePublisher = new NotificationRealtimePublisher(messagingTemplate);
  }

  @Test
  void publishUnreadCountAfterCommit_OutsideTransactionPublishesUserTopic() {
    realtimePublisher.publishUnreadCountAfterCommit(1L, 3L);

    verify(messagingTemplate).convertAndSend("/topic/notifications/1/unread-count", 3L);
  }
}
